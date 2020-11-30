package com.tangoristo.server.services.impl;

import com.javierartiles.commons.jadict.model.InfoCode;
import com.javierartiles.commons.jadict.model.Sense;
import com.javierartiles.commons.jadict.model.Trans;
import com.tangoristo.server.model.DictionaryEntry;
import com.tangoristo.server.model.InflectionAnalysisResult;
import com.tangoristo.server.model.RubyHint;
import com.tangoristo.server.model.Token;
import com.tangoristo.server.model.TokenSequence;
import com.tangoristo.server.model.VocabularyEntity;
import com.tangoristo.server.model.VocabularyLevel;
import com.tangoristo.server.model.vocabularylevels.JapaneseLanguageProficiencyTest;
import com.tangoristo.server.services.DictionaryService;
import com.tangoristo.server.services.InflectionAnalysisService;
import com.tangoristo.server.services.VocabularyService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JapaneseVocabularyServiceImpl implements VocabularyService {

    private static final String ENCODING = "UTF-8";
    private static final String PUNCTUATION = " “”‘’。，、：；！？－…》《〈〉「」﹁﹂『』\n";
    private static final String DIGITS = "1234567890１２３４５６７８９０";

    private final ResourceLoader resourceLoader;
    private final InflectionAnalysisService inflectionAnalysisService;

    private List<String> proficiencyLabels = Arrays.stream(JapaneseLanguageProficiencyTest.values())
            .map(Enum::name).collect(Collectors.toList());

    private final DictionaryService dictionaryService;
    private final Map<String, Integer> wordToFrequencyRank;
    private final Set<ImmutablePair<InfoCode, String>> jmdictToKuromojiPos;
    private final int lookaheadLimit;

    @Autowired
    public JapaneseVocabularyServiceImpl(ResourceLoader resourceLoader,
                                         InflectionAnalysisService inflectionAnalysisService,
                                         DictionaryService dictionaryService,
                                         @Value("${dictionaries.japanese.frequencyPath}") String frequencyPath,
                                         @Value("${dictionaries.japanese.posMappingPath}") String posMappingPath,
                                         @Value("${dictionaries.japanese.lookaheadLimit}") int lookaheadLimit)
            throws VocabularyAnalysisServiceException {
        this.resourceLoader = resourceLoader;
        this.inflectionAnalysisService = inflectionAnalysisService;
        this.lookaheadLimit = lookaheadLimit;
        this.dictionaryService = dictionaryService;
        try {
            wordToFrequencyRank = loadFrequencyMap(frequencyPath);
            jmdictToKuromojiPos = loadPosMapping(posMappingPath);

        } catch(Exception e) {
            throw new VocabularyAnalysisServiceException("Failed to loadDictionaryEntries dictionary resources", e);
        }
    }

    private Set<ImmutablePair<InfoCode, String>> loadPosMapping(String posMappingPath) throws IOException {
        Set<ImmutablePair<InfoCode, String>> mapping = new HashSet<>();
        for (String line : IOUtils.readLines(resourceLoader.getResource(posMappingPath).getInputStream(), ENCODING)) {
            String[] lineSpl = line.split("\t");
            InfoCode infoCode = InfoCode.valueOf(lineSpl[0].replace("-", "_"));
            String kuromojiPos = Arrays.stream(lineSpl).skip(1).collect(Collectors.joining(","));
            mapping.add(ImmutablePair.of(infoCode, kuromojiPos));
        }
        return mapping;
    }

    private Map<String, Integer> loadFrequencyMap(String frequencyPath) throws IOException {
        List<String> lines = IOUtils.readLines(resourceLoader.getResource(frequencyPath).getInputStream(), ENCODING);
        Map<String, Integer> wordToFrequencyRank = new HashMap<>();
        for (int i = 0; i < lines.size(); i ++) {
            wordToFrequencyRank.put(lines.get(i).trim(), i + 1);
        }
        return wordToFrequencyRank;
    }

    /**
     * Kuromoji tokenization splits inflected forms, this method merges them back into a single token
     * @param tokens
     * @return
     */
    private List<Token> mergeInflections(List<Token> tokens) {
        List<Token> tokensWithMergedInflections = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (isInflectionablePartOfSpeech(token.getPartOfSpeech())) {
                StringBuilder surfaceFormBuilder = new StringBuilder(token.getSurfaceForm());
                StringBuilder surfaceReadingBuilder = new StringBuilder(token.getSurfaceReading());
                List<String> inflectionPosList = new ArrayList<>();
                for (int c = i + 1; c < tokens.size() &&
                        isInflectionPartOfSpeech(tokens.get(c).getPartOfSpeech(), tokens.get(c).getSurfaceForm()); c++) {
                    Token auxToken = tokens.get(c);
                    inflectionPosList.add(auxToken.getPartOfSpeech());
                    surfaceFormBuilder.append(auxToken.getSurfaceForm());
                    surfaceReadingBuilder.append(auxToken.getSurfaceReading());
                    i = c;
                }

                Optional<String> posTransform = getPartOfSpeechTransformation(token.getPartOfSpeech(), inflectionPosList);

                String baseForm = token.getBaseForm();
                if (posTransform.isPresent() && posTransform.get().equals("名詞,*,*,*")) {
                    baseForm = surfaceFormBuilder.toString();
                }

                token = Token.builder()
                        .baseForm(baseForm)
                        .surfaceForm(surfaceFormBuilder.toString())
                        .surfaceReading(surfaceReadingBuilder.toString())
                        .startOffset(token.getStartOffset())
                        .partOfSpeech(posTransform.orElse(token.getPartOfSpeech()))
                        .isInflected(true)
                        .build();
            }

            tokensWithMergedInflections.add(token);
        }
        return tokensWithMergedInflections;
    }

    private List<Token> mergeDigits(List<Token> tokens) {
        List<Token> mergedTokens = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (isDigit(token)) {
                StringBuilder surfaceFormBuilder = new StringBuilder(token.getSurfaceForm());
                StringBuilder surfaceReadingBuilder = new StringBuilder(token.getSurfaceReading());
                for (int c = i + 1; c < tokens.size() && isDigit(tokens.get(c)); c++) {
                    Token auxToken = tokens.get(c);
                    surfaceFormBuilder.append(auxToken.getSurfaceForm());
                    surfaceReadingBuilder.append(auxToken.getSurfaceReading());
                    i = c;
                }
                String surfaceForm = surfaceFormBuilder.toString();
                String surfaceReading = surfaceFormBuilder.toString();

                token = Token.builder()
                        .isInflected(false)
                        .startOffset(token.getStartOffset())
                        .partOfSpeech(token.getPartOfSpeech())
                        .surfaceForm(surfaceForm)
                        .surfaceReading(surfaceReading)
                        .build();
            }
            mergedTokens.add(token);
        }
        return mergedTokens;
    }

    private boolean isDigit(Token token) {
        return token.getSurfaceForm().matches("[" + DIGITS + "]+");
    }

    private boolean isInflectionablePartOfSpeech(String partOfSpeech) {
        return partOfSpeech.matches("(形容詞|形容動詞|動詞|助動詞),.+");
    }

    private boolean isInflectionPartOfSpeech(String partOfSpeech, String surfaceForm) {
        return partOfSpeech.matches("(助動詞|動詞,接尾).+")
                || partOfSpeech.matches("名詞,接尾.+")
                || (partOfSpeech.matches("助詞,接続助詞,\\*,\\*") && surfaceForm.equals("て"));
    }

    /**
     * Some words change POS after being inflected (e.g. adj + さ -> noun)
     */
    private Optional<String> getPartOfSpeechTransformation(String inflectionablePos, List<String> inflectionPosList) {
        // adj + さ -> noun
        if (inflectionablePos.matches("形容詞,.+") &&
                inflectionPosList.stream().anyMatch(pos -> pos.matches("名詞,接尾.+") )) {
            return Optional.of("名詞,*,*,*");
        }
        return Optional.empty();
    }

    private Optional<VocabularySearchResult> searchVocabularyCandidates(List<Token> tokens,
                                                                        int startIndex,
                                                                        boolean properNameSearch) {
        VocabularySearchResult searchResult = null;
        StringBuilder keyBuffer = new StringBuilder();
        boolean initialTokenIsProperNoun = tokens.get(startIndex).getPartOfSpeech().startsWith("名詞,固有名詞");
        for (int indexJump = 0; indexJump < lookaheadLimit && (startIndex + indexJump) < tokens.size(); indexJump++) {
            Token token = tokens.get(startIndex + indexJump);
            String tokenStr = StringUtils.isEmpty(token.getBaseForm()) ? token.getSurfaceForm() : token.getBaseForm();
            keyBuffer.append(tokenStr);
            List<DictionaryEntry> dictionaryMatches = null;
            String dictionaryQuery = keyBuffer.toString();
            try {
                dictionaryMatches = dictionaryService.search(dictionaryQuery, Locale.JAPANESE);
            } catch (DictionaryServiceException e) {
                log.warn("Failed to search dictionary with query = {}", dictionaryQuery, e);
            }
            List<DictionaryEntry> partialMatchCandidates = ObjectUtils
                    .defaultIfNull(dictionaryMatches, new ArrayList<DictionaryEntry>())
                    .stream()
                    .filter(e -> {
                        if (properNameSearch) {
                            return !e.getTransList().isEmpty();
                        } else {
                            return e.getTransList().isEmpty();
                        }
                    })
                    .collect(Collectors.toList());

            // Exclude proper nouns from candidates list if the initial token was not a proper noun
            if (partialMatchCandidates != null && !initialTokenIsProperNoun) {
                partialMatchCandidates = partialMatchCandidates
                        .stream()
                        .filter(de -> de.getTransList().size() == 0)
                        .collect(Collectors.toList());
            }

            if (!CollectionUtils.isEmpty(partialMatchCandidates)) {
                TokenSequence tokenSequence = TokenSequence.builder()
                        .tokenList(tokens.subList(startIndex, startIndex + indexJump + 1))
                        .build();
                Optional<InflectionAnalysisResult> inflectionAnalysisResult =
                        inflectionAnalysisService.analyze(tokenSequence.getSurfaceForm(), tokenSequence.getBaseForm());
                tokenSequence.setInflectionAnalysisResult(inflectionAnalysisResult.orElse(null));
                searchResult = new VocabularySearchResult(partialMatchCandidates, tokenSequence, indexJump);

                // When we encounter a honorific (e.g. さん) between a noun and a particle,
                // we can safely skip further exploration of longer dictionary candidates.
                if (startIndex > 0
                        && startIndex + 1 < tokens.size()
                        && token.getPartOfSpeech().equals("名詞,接尾,人名,*")
                        && tokens.get(startIndex - 1).getPartOfSpeech().startsWith("名詞,")
                        && tokens.get(startIndex + 1).getPartOfSpeech().startsWith("助詞,")) {
                    break;
                }
            }
        }
        return Optional.ofNullable(searchResult);
    }

    @lombok.Value
    @AllArgsConstructor
    private static class VocabularySearchResult {
        private List<DictionaryEntry> matchCandidates;
        private TokenSequence tokenSequence;
        private int indexJump;
    }

    @Override
    public List<VocabularyEntity> getVocabulary(List<Token> rawTokens) {
        return getVocabulary(rawTokens, null);
    }

    @Override
    public List<VocabularyEntity> getVocabulary(List<Token> rawTokens, List<RubyHint> rubyHints) {
        List<Token> mergedTokens = mergeInflections(rawTokens);
        mergedTokens = mergeDigits(mergedTokens);

        List<VocabularyEntity> vocabularyEntities = new ArrayList<>();
        for (int i = 0; i < mergedTokens.size(); i++) {

            Optional<VocabularySearchResult> vocabularySearchResultOptional
                    = searchVocabularyCandidates(mergedTokens, i, false);

            // If nothing was found, try again with proper names only
            if (!vocabularySearchResultOptional.isPresent()) {
                vocabularySearchResultOptional = searchVocabularyCandidates(mergedTokens, i, true);
            }

            if (vocabularySearchResultOptional.isPresent()) {
                VocabularySearchResult vocabularySearchResult = vocabularySearchResultOptional.get();
                i += vocabularySearchResult.getIndexJump();
                List<DictionaryEntry> matchCandidates = vocabularySearchResult.getMatchCandidates();
                TokenSequence tokenSequence = vocabularySearchResult.getTokenSequence();

                Optional<String> optionalLeftContext = Optional.empty();
                if (vocabularyEntities.size() > 0) {
                    optionalLeftContext = Optional.of(vocabularyEntities.get(vocabularyEntities.size() - 1).getTokenSequenceOccurrences().get(0).getSurfaceForm());
                }
                DictionaryEntry topDictEntryCandidate = getTopDictionaryCandidate(matchCandidates, tokenSequence,
                        rubyHints, optionalLeftContext);
                // Merge day and month ordinals
                if (topDictEntryCandidate.getDictionaryForm().matches("([０-９][０-９]?|[一二三四五六七八九十]+)(日|月)")) {
                    Token firstTokenInOrdinal = tokenSequence.getTokenList().get(0);
                    Token lastTokenInOrdinal = tokenSequence.getTokenList().get(tokenSequence.getTokenList().size() - 1);
                    tokenSequence = TokenSequence.builder()
                            .tokenList(Collections.singletonList(Token.builder()
                                    .isInflected(false)
                                    .partOfSpeech(lastTokenInOrdinal.getPartOfSpeech())
                                    .startOffset(firstTokenInOrdinal.getStartOffset())
                                    .baseForm(topDictEntryCandidate.getDictionaryForm())
                                    .surfaceForm(tokenSequence.getSurfaceForm())
                                    .surfaceReading(topDictEntryCandidate.getAlternateForm())
                                    .build()))
                            .build();
                }

                // In some very specific cases, if there is discrepancy between the token sequence reading and the
                // dictionary reading, we will favor the latter.
                String reading = tokenSequence.getSurfaceReading();
                if (topDictEntryCandidate.getAlternateForm() != null
                        && !tokenSequence.isInflected()
                        && !Token.isKatakana(topDictEntryCandidate.getAlternateForm())
                        && !tokenSequence.getSurfaceReading().equals(topDictEntryCandidate.getAlternateForm())
                        && topDictEntryCandidate.getPartOfSpeech().stream()
                              .anyMatch(infoCode -> infoCode.getCode().matches("(adv|adj.+)"))
                        ) {
                    log.debug("{} ({}) : {} != {}",
                            tokenSequence.getSurfaceForm(),
                            StringUtils.join(topDictEntryCandidate.getPartOfSpeech(), ", "),
                            tokenSequence.getSurfaceReading(), topDictEntryCandidate.getAlternateForm());
                    reading = topDictEntryCandidate.getAlternateForm();
                    Token replacementToken = Token.builder()
                            .baseForm(topDictEntryCandidate.getDictionaryForm())
                            .surfaceForm(tokenSequence.getSurfaceForm())
                            .surfaceReading(topDictEntryCandidate.getAlternateForm())
                            .partOfSpeech(tokenSequence.getTokenList().get(0).getPartOfSpeech())
                            .startOffset(tokenSequence.getTokenList().get(0).getStartOffset())
                            .isInflected(false)
                            .build();
                    tokenSequence = TokenSequence.builder()
                            .tokenList(Collections.singletonList(replacementToken))
                            .build();
                }

                VocabularyEntity vocabularyEntity = VocabularyEntity.builder()
                        .dictionaryEntry(topDictEntryCandidate)
                        .tokenSequenceOccurrences(Collections.singletonList(tokenSequence))
                        .reading(reading)
                        .firstOccurrenceOffset(tokenSequence.getFirstOccurrenceOffset())
                        .build();
                vocabularyEntities.add(vocabularyEntity);
            } else {
                // Don't warn about punctuation not matched with dict
                if (!(PUNCTUATION.contains(mergedTokens.get(i).getSurfaceForm())
                        || DIGITS.contains(mergedTokens.get(i).getSurfaceForm()))) {
                    log.trace("Failed to find dictionary entry for token sequence = {}", mergedTokens.get(i));
                }
            }
        }
        return vocabularyEntities;
    }

    private DictionaryEntry getTopDictionaryCandidate(
            List<DictionaryEntry> candidates,
            TokenSequence tokenSequence,
            List<RubyHint> rubyHints,
            Optional<String> optionalLeftContext
    ) {
        if (candidates.size() == 0) {
            throw new RuntimeException("Received empty candidates set");
        } else if (candidates.size() == 1) {
            return candidates.iterator().next();
        }

        if (!Token.isKana(tokenSequence.getSurfaceForm())) {
            candidates = filterCandidateBasedOnRubyHints(tokenSequence, candidates, rubyHints, optionalLeftContext);
        }
        log.trace("tokenSequence = {}; ", tokenSequence);
        return getTopCandidateByHeuristicRanking(candidates, tokenSequence);
    }

    static List<DictionaryEntry> filterCandidateBasedOnRubyHints(
            TokenSequence tokenSequence,
            List<DictionaryEntry> candidates,
            List<RubyHint> rubyHints,
            Optional<String> optionalLeftContext
    ) {
        if (rubyHints == null) {
            return candidates;
        }
        String sequenceSurfaceForm = tokenSequence.getSurfaceForm();
        Set<String> readingHints = rubyHints.stream()
                .filter(hint -> {
                    boolean hasValidLeftContext = true;
                    if (optionalLeftContext.isPresent() && !StringUtils.isEmpty(hint.getLeftContext())) {
                        hasValidLeftContext = hint.getLeftContext().equals(optionalLeftContext.get());
                    }
                    String sequenceSurfaceFormKanaSuffix = Token.getKanaSuffix(sequenceSurfaceForm.toCharArray());
                    String sequenceSurfaceFormWithoutKanaSuffix = sequenceSurfaceForm.replace(sequenceSurfaceFormKanaSuffix, "");
                    boolean matchesRuby = hint.getRuby().equals(sequenceSurfaceFormWithoutKanaSuffix);
                    return hasValidLeftContext && matchesRuby;
                })
                .map(RubyHint::getRt)
                .distinct()
                .collect(Collectors.toSet());
        List<DictionaryEntry> filteredCandidates = candidates.stream()
                .filter(cand -> {
                    String alternateForm = cand.getAlternateForm();
                    if (alternateForm != null) {
                        String kanaSuffix = Token.getKanaSuffix(cand.getDictionaryForm().toCharArray());
                        String alternateFormWithoutKanaSuffix = alternateForm.replaceFirst(Pattern.quote(kanaSuffix) + "$", "");
                        return readingHints.contains(alternateFormWithoutKanaSuffix);
                    } else {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        if (filteredCandidates.size() > 0) {
            log.trace("Filtered down candidates for '{}' (from {} to {}) list using ruby hints.",
                    tokenSequence.getSurfaceForm(), candidates.size(), filteredCandidates.size());
            return filteredCandidates;
        } else {
            return candidates;
        }
    }

    private DictionaryEntry getTopCandidateByHeuristicRanking(List<DictionaryEntry> candidates, TokenSequence tokenSequence) {
        List<ScoredDictionaryEntry> rankedEntries = candidates.stream()
                .map(de -> scoredDictionaryEntry(de, tokenSequence))
                .sorted(Comparator.comparingDouble(ScoredDictionaryEntry::getScore).reversed())
                .collect(Collectors.toList());

        if (rankedEntries.size() >= 2 &&
                rankedEntries.get(0).getScore() == rankedEntries.get(1).getScore()) {
            double tieScore = rankedEntries.get(0).getScore();
            List<ScoredDictionaryEntry> tiedEntries =
                    rankedEntries.stream().filter(e -> e.getScore() == tieScore).collect(Collectors.toList());
            log.trace("There is a tie matching token sequence = {}\n candidates = \n {} ;",
                    tokenSequence, tiedEntries.stream().map(ScoredDictionaryEntry::toString)
                            .collect(Collectors.joining("\n")));
            log.trace("Using Wikipedia word frequency to break tie");
            return getMostFrequent(tiedEntries.stream().map(ScoredDictionaryEntry::getDictionaryEntry)
                    .collect(Collectors.toList()));
        } else {
            return rankedEntries.get(0).getDictionaryEntry();
        }
    }

    private DictionaryEntry getMostFrequent(Collection<DictionaryEntry> dictionaryEntries) {
        Collection<ScoredDictionaryEntry> entriesByAscFreqRank = dictionaryEntries.stream().map(e -> {
            int freq = wordToFrequencyRank.getOrDefault(e.getDictionaryForm(), Integer.MAX_VALUE);
            return ScoredDictionaryEntry.builder().dictionaryEntry(e).score(freq).build();
        }).sorted(Comparator.comparingDouble(ScoredDictionaryEntry::getScore)).collect(Collectors.toList());
        log.trace("Tied entries sorted by ascending frequency rank:");
        entriesByAscFreqRank.forEach(e -> log.trace(e.toString()));
        return entriesByAscFreqRank.stream().findFirst().get().getDictionaryEntry();
    }

    /**
     * Returns a POS match score where 0 is not match, 1 is one level, 2 two levels, etc.
     */
    private int posMatch(List<InfoCode> dictionaryEntryPos, String kuromojiPos) {
        Set<ImmutablePair<InfoCode, String>> candidateMatches = jmdictToKuromojiPos
                .stream()
                .filter(pair -> kuromojiPos.startsWith(pair.getRight()) && dictionaryEntryPos.contains(pair.getLeft()))
                .collect(Collectors.toSet());
        if (candidateMatches.size() == 0) {
            log.trace("Found no match for POS {}", kuromojiPos);
        }
        int maxScore = 0;
        for (ImmutablePair<InfoCode, String> candidate : candidateMatches) {
            int sc = candidate.getRight().split(",").length;
            if (maxScore < sc) {
                maxScore = sc;
            }
        }
        return maxScore;
    }

    private List<InfoCode> negativeInfoCodes = Arrays.asList(InfoCode.obs, InfoCode.obsc, InfoCode.arch, InfoCode.X,
            InfoCode.vulg, InfoCode.col);

    private ScoredDictionaryEntry scoredDictionaryEntry(DictionaryEntry dictionaryEntry, TokenSequence tokenSequence) {
        int score = 0;

        log.trace("dictionaryEntry = {}; ", dictionaryEntry);

        // Favor POS matches, but consider POS associated to each sense individually not as a whole
        int maxPosScore = -1;
        List<InfoCode> selectedPosGroup = Collections.emptyList();
        List<InfoCode> selectedMiscInfo = Collections.emptyList();

        for (Sense sense : dictionaryEntry.getSenseList()) {
            int posGroupScore = posMatch(sense.getPos(), tokenSequence.getTokenList().get(0).getPartOfSpeech());
            if (posGroupScore > maxPosScore) {
                maxPosScore = posGroupScore;
                selectedPosGroup = sense.getPos();
                selectedMiscInfo = sense.getMisc();
            }
        }

        for (List<InfoCode> ic : dictionaryEntry.getTransList().stream().map(Trans::getNameTypeList).collect(Collectors.toSet())) {
            int posGroupScore = posMatch(ic, tokenSequence.getTokenList().get(0).getPartOfSpeech());
            if (posGroupScore > maxPosScore) {
                maxPosScore = posGroupScore;
                selectedPosGroup = ic;
                selectedMiscInfo = Collections.emptyList();
            }
        }

        log.trace("+ posScore = {}", maxPosScore);
        score += Math.max(0, maxPosScore);

        if (dictionaryEntry.isCommonWord()) {
            log.trace("+ is common word");
            score += 2;
        }

        // Favor cases where the reading matches
        if (tokenSequence.getSurfaceReading().equals(dictionaryEntry.getDictionaryForm())) {
            log.trace("+ ts surface reading and dict. form match");
            score ++;
        }
        if (tokenSequence.getSurfaceReading().equals(dictionaryEntry.getAlternateForm())) {
            log.trace("+ ts surface reading and alt. form match");
            score += 2;
        }

        // Favor cases where there is no alternate form and dictionary reading matches
        if (dictionaryEntry.getAlternateForm() == null
                && dictionaryEntry.getDictionaryForm().equals(tokenSequence.getSurfaceForm())) {
            log.trace("+ no alt. form but dict. from and ts. reading match");
            score ++;
        }

        // Sequence is inflected and its base form matches the dictionary form reading
        if (tokenSequence.isInflected() && tokenSequence.getBaseForm() != null &&
                tokenSequence.getBaseForm().equals(dictionaryEntry.getAlternateForm())) {
            log.trace("+ sequence is inflected and its base form matches the dictionary reading");
            score ++;
        }

        // uk	word usually written using kana alone
        if (selectedMiscInfo.contains(InfoCode.uk) &&
                ((!tokenSequence.isInflected() && tokenSequence.getSurfaceForm().equals(dictionaryEntry.getAlternateForm())) ||
                 (tokenSequence.isInflected() && tokenSequence.getBaseForm().equals(dictionaryEntry.getAlternateForm())))) {
            log.trace("+ dictionary entry usually written in kana and found kana written");
            score ++;
        }

        // uK	word usually written using kanji alone
        if (selectedMiscInfo.contains(InfoCode.uK) &&
                ((!tokenSequence.isInflected() && tokenSequence.getSurfaceForm().equals(dictionaryEntry.getDictionaryForm())) ||
                 (tokenSequence.isInflected() && tokenSequence.getBaseForm().equals(dictionaryEntry.getDictionaryForm())))) {
            log.trace("+ dictionary entry usually written in kanji and found kanji written");
            score ++;
        }

        if (!Collections.disjoint(selectedMiscInfo, negativeInfoCodes)) {
            log.trace("- is obsolete or obscure word");
            score -= 2;
        }

        log.trace("score: {}", score);

        return ScoredDictionaryEntry.builder()
                .dictionaryEntry(dictionaryEntry)
                .score(score)
                .build();
    }

    @Override
    public VocabularyLevel getVocabularyLevel(List<VocabularyEntity> vocabulary) {
        Map<String, Integer> levelToFrequency = new HashMap<>();
        proficiencyLabels.stream().forEach(l -> levelToFrequency.put(l, 0));

        int unknownLevelFreq = 0;
        for (VocabularyEntity vocabularyEntity : vocabulary) {
            // Skip particles for vocabulary level assessment
            if (!vocabularyEntity.getDictionaryEntry().getPartOfSpeech().contains(null) &&
                    vocabularyEntity.getDictionaryEntry().getPartOfSpeech().stream().anyMatch(pos -> pos.equals(InfoCode.prt))) {
                continue;
            }

            String proficiencyLevel = vocabularyEntity.getDictionaryEntry().getOfficialProficiencyLevel();
            if (proficiencyLevel != null && levelToFrequency.containsKey(proficiencyLevel)) {
                levelToFrequency.merge(proficiencyLevel, 1, Integer::sum);
            } else {
                unknownLevelFreq += 1;
            }
        }
        return VocabularyLevel.builder()
                .increasingDifficultyLevelLabels(proficiencyLabels)
                .levelToFrequency(levelToFrequency)
                .unknownLevelFrequency(unknownLevelFreq)
                .build();
    }

    @lombok.Value
    @Builder
    @ToString
    private static class ScoredDictionaryEntry {
        private int score;
        private DictionaryEntry dictionaryEntry;
    }
}
