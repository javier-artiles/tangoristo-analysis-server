package com.tangoristo.server.services.impl;

import com.javierartiles.commons.jadict.JMDictParser;
import com.javierartiles.commons.jadict.model.Entry;
import com.javierartiles.commons.jadict.model.Gloss;
import com.javierartiles.commons.jadict.model.InfoCode;
import com.javierartiles.commons.jadict.model.KEle;
import com.javierartiles.commons.jadict.model.Priority;
import com.javierartiles.commons.jadict.model.REle;
import com.javierartiles.commons.jadict.model.Sense;
import com.javierartiles.commons.jadict.model.TransDet;
import com.neovisionaries.i18n.LanguageCode;
import com.tangoristo.server.model.DictionaryEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class JapaneseDictionaryLoader {

    private static final Collection<Priority> commonTagMarkers = new HashSet<>(Arrays.asList(
            Priority.news1, Priority.news2, Priority.ichi1, Priority.ichi2, Priority.spec1, Priority.spec2,
            Priority.gai1, Priority.gai2));

    private final ResourceLoader resourceLoader;
    private final String jmdictPath;
    private final String jmdictExtPath;
    private final String jmnedictPath;
    private final String jlptPrefixPath;

    public JapaneseDictionaryLoader(
            ResourceLoader resourceLoader,
            @Value("${dictionaries.japanese.jmdictPath}") String jmdictPath,
            @Value("${dictionaries.japanese.jmdictExtPath}") String jmdictExtPath,
            @Value("${dictionaries.japanese.jmnedictPath}") String jmnedictPath,
            @Value("${dictionaries.japanese.jlptPrefixPath}") String jlptPrefixPath
    ) {
        this.resourceLoader = resourceLoader;
        this.jmdictPath = jmdictPath;
        this.jmdictExtPath = jmdictExtPath;
        this.jmnedictPath = jmnedictPath;
        this.jlptPrefixPath = jlptPrefixPath;
    }

    public List<DictionaryEntry> loadDictionaryEntries() throws ParserConfigurationException, SAXException, IOException {
        log.info("Loading JLPT mapping");
        Map<String, Set<Integer>> jlptLevelToEntSeqSet = loadJlptMap(jlptPrefixPath);
        log.info("Loading dictionary entries");
        return loadDictionaryEntries(jmdictPath, jmdictExtPath, jmnedictPath, jlptLevelToEntSeqSet);
    }


    private String getProficiencyLevel(int entSeq, Map<String, Set<Integer>> jlptLevelToEntSeqSet) {
        for (String level : jlptLevelToEntSeqSet.keySet()) {
            if (jlptLevelToEntSeqSet.get(level).contains(entSeq)) {
                return level;
            }
        }
        return "UNKNOWN";
    }

    private Map<String, Set<Integer>> loadJlptMap(String jlptPrefixPath) throws IOException {
        Map<String, Set<Integer>> jlptLevelToEntSeqSet = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            String level = String.format("JLPT_N%d", i);
            String jlptPath = jlptPrefixPath + i + ".lst";
            InputStream jlptInputStream = resourceLoader.getResource(jlptPath).getInputStream();
            Set<Integer> entSeqSet = IOUtils.readLines(jlptInputStream, "UTF-8")
                    .stream()
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
            jlptLevelToEntSeqSet.put(level, entSeqSet);
        }
        return jlptLevelToEntSeqSet;
    }

    private List<DictionaryEntry> loadDictionaryEntries(
            String jmdictPath,
            String jmdictExtPath,
            String jmnedictPath,
            Map<String, Set<Integer>> jlptLevelToEntSeqSet
    ) throws IOException, ParserConfigurationException, SAXException {
        JMDictParser parser = new JMDictParser();
        Stream<Entry> jmdictExtStream = parser.parse(resourceLoader.getResource(jmdictExtPath).getInputStream()).stream();
        Stream<Entry> jmdictStream = parser.parse(resourceLoader.getResource(jmdictPath).getInputStream()).stream();
        Stream<Entry> jmnedictStream = parser.parse(resourceLoader.getResource(jmnedictPath).getInputStream()).stream();
        return Stream.concat(Stream.concat(jmdictStream, jmdictExtStream), jmnedictStream)
                .map(entry -> {
                    String dictionaryForm = entry.getKEleList().stream().map(KEle::getKeb).findFirst().orElse(null);
                    String alternateForm = entry.getREleList().stream().map(REle::getReb).findFirst().orElse(null);

                    Stream<InfoCode> sensePosStream = entry.getSenseList().stream().flatMap(sense -> sense.getPos().stream());
                    Stream<InfoCode> entityTypeStream = entry.getTransList().stream().flatMap(trans -> trans.getNameTypeList().stream());
                    List<InfoCode> partOfSpeech = Stream.concat(sensePosStream, entityTypeStream)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    Map<LanguageCode, List<String>> definitions = new HashMap<>();
                    List<String> definitionStrings = entry.getSenseList().stream().map(Sense::getGloss)
                            .map(glosses -> glosses.stream().map(Gloss::getText).collect(Collectors.joining("; ")))
                            .filter(def -> !def.isEmpty())
                            .collect(Collectors.toList());
                    if (definitionStrings.size() == 0) {
                        definitionStrings = entry.getTransList().stream()
                                .flatMap(trans -> trans.getTransDetList().stream())
                                .filter(transDet -> transDet.getLang().equals(LanguageCode.en))
                                .map(TransDet::getText)
                                .collect(Collectors.toList());
                    }

                    definitions.put(LanguageCode.en, definitionStrings);
                    boolean isProperNoun = partOfSpeech.contains(InfoCode.n_pr) || !entry.getTransList().isEmpty();
                    Stream<Priority> kePri = entry.getKEleList().stream().flatMap(kEle -> kEle.getKePri().stream());
                    Stream<Priority> rePri = entry.getREleList().stream().flatMap(rEle -> rEle.getRePri().stream());
                    boolean isCommonNoun = Stream.concat(kePri, rePri).anyMatch(commonTagMarkers::contains);
                    String officialProficiencyLevel = getProficiencyLevel(entry.getEntSeq(), jlptLevelToEntSeqSet);

                    if (dictionaryForm == null) {
                        dictionaryForm = alternateForm;
                        alternateForm = null;
                    }

                    // All proper nouns should have the noun pos
                    if (!entry.getTransList().isEmpty() && !partOfSpeech.contains(InfoCode.n)) {
                        partOfSpeech.add(InfoCode.n);
                    }

                    return DictionaryEntry.builder()
                            .entSeq(entry.getEntSeq())
                            .kEleList(entry.getKEleList())
                            .rEleList(entry.getREleList())
                            .senseList(entry.getSenseList())
                            .transList(entry.getTransList())
                            .dictionaryForm(dictionaryForm)
                            .alternateForm(alternateForm)
                            .partOfSpeech(partOfSpeech)
                            .definitions(definitions)
                            .officialProficiencyLevel(officialProficiencyLevel)
                            .isProperNoun(isProperNoun)
                            .isCommonWord(isCommonNoun)
                            .build();
                })
                .collect(Collectors.toList());
    }

}
