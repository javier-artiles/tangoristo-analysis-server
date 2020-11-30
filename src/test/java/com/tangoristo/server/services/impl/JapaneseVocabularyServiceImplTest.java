package com.tangoristo.server.services.impl;

import com.tangoristo.server.model.DictionaryEntry;
import com.tangoristo.server.model.Token;
import com.tangoristo.server.model.VocabularyEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestContextManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.io.IOUtils.readLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@SpringBootTest
@Slf4j
public class JapaneseVocabularyServiceImplTest {
    private static final String DATA_PATH = "data/vocabulary_test_data.tsv";

    @Autowired
    private JapaneseVocabularyServiceImpl japaneseDictionaryService;

    @Autowired
    private JapaneseTokenizationServiceImpl japaneseTokenizationService;

    private TestContextManager testContextManager;

    @Parameter(value = 0)
    public String input;

    @Parameter(value = 1)
    public int expectedVocabularySize;

    @Parameter(value = 2)
    public int testVocabularyEntryIndex;

    @Parameter(value = 3)
    public String expectedDictionaryForm;

    @Parameter(value = 4)
    public String expectedAlternateForm;

    @Parameter(value = 5)
    public String expectedJlptLevelForm;

    @Parameters(name = "{index}: getVocabulary(...)[{2}] -> dict \"{3}\", alt \"{4}\"")
    public static Collection<Object[]> data() throws IOException {
        return loadTestData();
    }

    private static Collection<Object[]> loadTestData() throws IOException {
        InputStream inputStream = JapaneseVocabularyServiceImplTest.class.getClassLoader().getResourceAsStream(DATA_PATH);
        return readLines(inputStream, "UTF-8").stream()
                .skip(2)
                .map(JapaneseVocabularyServiceImplTest::asTestParams)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Object[] asTestParams(String line) {
        String[] lineSpl = line.split("\t", -1);
        try {
            int expectedVocabularySize = Integer.parseInt(lineSpl[0]);
            String expectedDictionaryForm = lineSpl[1];
            String expectedAlternateForm = lineSpl[2].isEmpty() ? null : lineSpl[2];
            String expectedJlptLevelForm = lineSpl[3];
            int testVocabularyEntryIndex = Integer.parseInt(lineSpl[4]);
            String input = lineSpl[5];
            return new Object[] { input, expectedVocabularySize, testVocabularyEntryIndex,
                    expectedDictionaryForm, expectedAlternateForm, expectedJlptLevelForm };
        } catch(Exception e) {
            e.printStackTrace();
            log.error("Failed to parse line '{}': {}", line, e.getMessage());
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);
    }

    @Test
    public void testGetVocabulary() {
        List<Token> rawTokens = japaneseTokenizationService.tokenize(input);
        List<VocabularyEntity> vocabularyEntityList = japaneseDictionaryService.getVocabulary(rawTokens);
        vocabularyEntityList.forEach(entry -> log.info(entry.toString()));

        VocabularyEntity vocabularyEntity = vocabularyEntityList.get(testVocabularyEntryIndex);
        DictionaryEntry testDictionaryEntry = vocabularyEntity.getDictionaryEntry();

        assertThat(vocabularyEntityList.size(), equalTo(expectedVocabularySize));
        assertThat(testDictionaryEntry.getDictionaryForm(), equalTo(expectedDictionaryForm));
        assertThat(testDictionaryEntry.getAlternateForm(), equalTo(expectedAlternateForm));
        assertThat(vocabularyEntity.getDictionaryEntry().getOfficialProficiencyLevel(), equalTo(expectedJlptLevelForm));
    }
}
