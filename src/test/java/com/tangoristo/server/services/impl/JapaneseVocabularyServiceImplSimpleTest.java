package com.tangoristo.server.services.impl;

import com.javierartiles.commons.jadict.model.InfoCode;
import com.tangoristo.server.model.DictionaryEntry;
import com.tangoristo.server.model.InflectionAnalysisResult;
import com.tangoristo.server.model.RubyHint;
import com.tangoristo.server.model.Token;
import com.tangoristo.server.model.TokenSequence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(SpringJUnit4ClassRunner.class)
public class JapaneseVocabularyServiceImplSimpleTest {

    private TokenSequence getTokenSequence() {
        Token token = Token.builder()
                .partOfSpeech("動詞,自立,*,*")
                .isInflected(true)
                .startOffset(127)
                .surfaceForm("増えました")
                .baseForm("増える")
                .surfaceReading("ふえました")
                .build();
        List<Token> tokenList = Collections.singletonList(token);
        InflectionAnalysisResult inflectionAnalysisResult = InflectionAnalysisResult.builder()
                .inflectionForm("ました")
                .inflectionBase("る")
                .inflectionName("polite past")
                .build();
        return TokenSequence.builder()
                .tokenList(tokenList)
                .inflectionAnalysisResult(inflectionAnalysisResult)
                .build();
    }

    private DictionaryEntry getDictionaryEntryA() {
        return DictionaryEntry.builder()
                .isCommonWord(true)
                .isProperNoun(false)
                .officialProficiencyLevel("JLPT_N4")
                .dictionaryForm("増える")
                .alternateForm("ふえる")
                .partOfSpeech(Arrays.asList(InfoCode.v1, InfoCode.vi))
                .definitions(Collections.emptyMap())
                .build();
    }

    private DictionaryEntry getDictionaryEntryB() {
        return DictionaryEntry.builder()
                .isCommonWord(true)
                .isProperNoun(false)
                .officialProficiencyLevel("JLPT_N4")
                .dictionaryForm("増える")
                .alternateForm("ぶえる")
                .partOfSpeech(Arrays.asList(InfoCode.v1, InfoCode.vi))
                .definitions(Collections.emptyMap())
                .build();
    }

    private List<RubyHint> getRubyHints() {
        return Arrays.asList(
            RubyHint.builder()
                    .ruby("増")
                    .rt("ふ")
                    .build()
        );
    }

    @Test
    public void testFilterCandidatesBasedOnRubyHints() {
        TokenSequence tokenSequence = getTokenSequence();
        List<DictionaryEntry> candidates = Arrays.asList(getDictionaryEntryA(), getDictionaryEntryB());
        List<RubyHint> rubyHints = getRubyHints();

        List<DictionaryEntry> filteredCandidates =
                JapaneseVocabularyServiceImpl.filterCandidateBasedOnRubyHints(tokenSequence, candidates, rubyHints, Optional.empty());

        assertThat(filteredCandidates.size(), is(1));
        assertThat(filteredCandidates.get(0), is(candidates.get(0)));
    }
}
