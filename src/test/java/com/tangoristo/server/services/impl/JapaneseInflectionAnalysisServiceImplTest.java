package com.tangoristo.server.services.impl;

import com.tangoristo.server.model.InflectionAnalysisResult;
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
import java.util.Optional;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.io.IOUtils.readLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
@SpringBootTest
@Slf4j
public class JapaneseInflectionAnalysisServiceImplTest {
    private static final String DATA_PATH = "data/inflection_test_data.tsv";

    @Autowired
    private JapaneseInflectionAnalysisServiceImpl analysisService;

    private TestContextManager testContextManager;

    @Parameterized.Parameter(value = 0)
    public String surfaceForm;

    @Parameterized.Parameter(value = 1)
    public String dictionaryForm;

    @Parameterized.Parameter(value = 2)
    public String expectedInflectionBase;

    @Parameterized.Parameter(value = 3)
    public String expectedInflectionForm;

    @Parameterized.Parameter(value = 4)
    public String expectedInflectionName;

    @Parameterized.Parameters(name = "{index}: analyze(\"{0}\", \"{1}\", \"{2}\") -> ")
    public static Collection<Object[]> data() throws IOException {
        InputStream inputStream = JapaneseVocabularyServiceImplTest.class.getClassLoader().getResourceAsStream(DATA_PATH);
        return readLines(inputStream, "UTF-8").stream()
                .skip(1)
                .map(JapaneseInflectionAnalysisServiceImplTest::asTestParams)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Object[] asTestParams(String line) {
        String[] lineSpl = line.split("\t");
        String expectedInflectionBase = lineSpl[0];
        String expectedInflectionForm = lineSpl[1];
        String expectedInflectionName = lineSpl[2];
        String surfaceForm = lineSpl[3];
        String dictionaryForm = lineSpl[4];
        return new Object[]{surfaceForm, dictionaryForm, expectedInflectionBase, expectedInflectionForm, expectedInflectionName};
    }

    @Before
    public void setUp() throws Exception {
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);
    }

    @Test
    public void testAnalyze() {
        Optional<InflectionAnalysisResult> optionalResult = analysisService.analyze(surfaceForm, dictionaryForm);

        assert(optionalResult.isPresent());

        InflectionAnalysisResult result = optionalResult.get();
        assertThat(result.getInflectionBase(), is(expectedInflectionBase));
        assertThat(result.getInflectionForm(), is(expectedInflectionForm));
        assertThat(result.getInflectionName(), is(expectedInflectionName));
    }

}
