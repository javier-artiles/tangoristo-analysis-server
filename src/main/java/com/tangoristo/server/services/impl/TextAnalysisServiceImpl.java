package com.tangoristo.server.services.impl;

import com.neovisionaries.i18n.LanguageCode;
import com.tangoristo.server.model.DictionaryEntry;
import com.tangoristo.server.model.LinkedSubstring;
import com.tangoristo.server.model.ReadingToSurfaceFormPair;
import com.tangoristo.server.model.RubyHint;
import com.tangoristo.server.model.Token;
import com.tangoristo.server.model.TokenSequence;
import com.tangoristo.server.model.VocabularyAnalysisResult;
import com.tangoristo.server.model.VocabularyEntity;
import com.tangoristo.server.model.VocabularyLevel;
import com.tangoristo.server.services.LanguageDetectionService;
import com.tangoristo.server.services.TextAnalysisService;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.javatuples.Triplet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TextAnalysisServiceImpl implements TextAnalysisService {
    @Autowired
    private JapaneseTokenizationServiceImpl japaneseTokenizationService;
    @Autowired
    private JapaneseVocabularyServiceImpl japaneseDictionaryService;
    @Autowired
    private LanguageDetectionService languageDetectionService;

    @Override
    public VocabularyAnalysisResult analyze(String text) {
        return analyze(text, null);
    }

    @Override
    public VocabularyAnalysisResult analyze(String text, List<RubyHint> rubyHintList) {
        Optional<LanguageCode> language = languageDetectionService.detectLanguage(text);
        // TODO refactor to allow support languages downstream or fail with unsupported language
        List<Token> tokens = japaneseTokenizationService.tokenize(text);
        List<VocabularyEntity> vocabularyEntityList = japaneseDictionaryService.getVocabulary(tokens, rubyHintList);
        List<VocabularyEntity> mergedVocabularyEntityList = mergeVocabularyEntityOcurrences(vocabularyEntityList);
        VocabularyLevel vocabularyLevel = japaneseDictionaryService.getVocabularyLevel(mergedVocabularyEntityList);
        List<LinkedSubstring> linkedSubstringList = getLinkedSubstringList(mergedVocabularyEntityList, text);
        return VocabularyAnalysisResult.builder()
                .vocabularyLevel(vocabularyLevel)
                .language(language.orElse(LanguageCode.undefined).name())
                .linkedText(linkedSubstringList)
                .vocabulary(mergedVocabularyEntityList)
                .build();
    }

    // TODO use token information to provide reading for OOV tokens
    private List<LinkedSubstring> getLinkedSubstringList(List<VocabularyEntity> vocabulary, String text) {
        List<LinkedSubstring> linkedSubstringList = new ArrayList<>();
        List<DictionaryEntry> dictionaryEntryList = vocabulary.stream()
                .map(VocabularyEntity::getDictionaryEntry).collect(Collectors.toList());
        Comparator<Triplet<Integer, Integer, TokenSequence>> byFirstOccurence =
                Comparator.comparingInt(e -> e.getValue2().getFirstOccurrenceOffset());
        List<Triplet<Integer, Integer, TokenSequence>> sortedPairs = vocabulary.stream()
                .flatMap(vocEnt -> vocEnt.getTokenSequenceOccurrences().stream()
                    .map(tokSeq -> Triplet.with(dictionaryEntryList.indexOf(vocEnt.getDictionaryEntry()),
                                            vocEnt.getTokenSequenceOccurrences().indexOf(tokSeq),
                                            tokSeq)))
                .sorted(byFirstOccurence)
                .collect(Collectors.toList());

        int lastEndOffset = 0;
        for (Triplet<Integer, Integer, TokenSequence> pair : sortedPairs) {
            Integer vocabularyIndex = pair.getValue0();
            Integer sequenceIndex = pair.getValue1();
            TokenSequence tokenSequence = pair.getValue2();

            int startOffset = tokenSequence.getFirstOccurrenceOffset();

            // If there is a gap between sequences, fill with the text (lookahead splitting on period)
            if (startOffset != lastEndOffset) {
                String fillText = text.substring(lastEndOffset, startOffset);
                Arrays.stream(fillText.split("(?<=。)"))
                        .filter(fill -> fill.length() > 0)
                        .forEach(fill -> {
                            LinkedSubstring linkedFillSubstring = LinkedSubstring.builder()
                                .readingList(Collections.singletonList(""))
                                .surfaceList(Collections.singletonList(fill))
                                .build();
                            linkedSubstringList.add(linkedFillSubstring);
                        });
            }

            int endOffset = startOffset + tokenSequence.getSurfaceForm().length();

            List<ReadingToSurfaceFormPair> alignedReadingToFormList = tokenSequence.getTokenList().stream()
                    .flatMap(tok -> tok.getAlignedReadingsToForms().stream()).collect(Collectors.toList());


            List<String> readingList = alignedReadingToFormList.stream().map(ReadingToSurfaceFormPair::getReading).collect(Collectors.toList());
            List<String> surfaceList = alignedReadingToFormList.stream().map(ReadingToSurfaceFormPair::getSurfaceForm).collect(Collectors.toList());

            // Sometime kuromoji and the matched dictionary entry diverge on their reading
            // If we are looking at a kanji surface string and there is disagreement, go with the dictionary choice
            DictionaryEntry dictionaryEntry = vocabulary.get(vocabularyIndex).getDictionaryEntry();
            String dictionaryForm = dictionaryEntry.getDictionaryForm();
            String dictionaryAlternateForm = dictionaryEntry.getAlternateForm();
            if (dictionaryAlternateForm != null) {
                String dictionaryFormKanaSuffix = Token.getKanaSuffix(dictionaryForm.toCharArray());
                String dictionaryReading = dictionaryAlternateForm.replaceAll(Pattern.quote(dictionaryFormKanaSuffix) + "$", "");
                if (readingList.size() == 1
                        && !StringUtils.isEmpty(readingList.get(0))
                        && !readingList.get(0).equals(dictionaryReading)) {
                    readingList.set(0, dictionaryReading);
                } else if (readingList.size() == 2 && !StringUtils.isEmpty(readingList.get(0)) && !StringUtils.isEmpty(readingList.get(1))){
                    if (readingList.get(1).equals(dictionaryFormKanaSuffix) && !readingList.get(0).equals(dictionaryReading)) {
                        readingList.set(0, dictionaryReading);
                    }
                }
            }

            LinkedSubstring linkedSubstring = LinkedSubstring.builder()
                    .readingList(readingList)
                    .surfaceList(surfaceList)
                    .vocabularyIndex(vocabularyIndex)
                    .sequenceIndex(sequenceIndex)
                    .build();

            linkedSubstringList.add(linkedSubstring);
            lastEndOffset = endOffset;
        }

        if (lastEndOffset < text.length()) {
            String fillText = text.substring(lastEndOffset, text.length()).trim();
            LinkedSubstring linkedFillSubstring = LinkedSubstring.builder()
                    .readingList(Collections.singletonList(""))
                    .surfaceList(Collections.singletonList(fillText))
                    .build();
            linkedSubstringList.add(linkedFillSubstring);
        }

        return linkedSubstringList;
    }

    private List<VocabularyEntity> mergeVocabularyEntityOcurrences(List<VocabularyEntity> vocabularyEntityList) {
        return vocabularyEntityList.stream()
                .collect(Collectors.groupingBy(VocabularyEntity::getDictionaryEntry)).entrySet().stream()
                .map(entry -> {
                    DictionaryEntry dictionaryEntry = entry.getKey();
                    List<VocabularyEntity> listVocabEntities = entry.getValue();
                    List<TokenSequence> tokenSequences = listVocabEntities.stream().
                            flatMap(ve -> ve.getTokenSequenceOccurrences().stream()).collect(Collectors.toList());
                    return VocabularyEntity.builder()
                            .dictionaryEntry(dictionaryEntry)
                            .tokenSequenceOccurrences(tokenSequences)
                            .reading(listVocabEntities.get(0).getReading())
                            .firstOccurrenceOffset(listVocabEntities.get(0).getFirstOccurrenceOffset())
                            .build();
                })
                .collect(Collectors.toList());
    }

}
