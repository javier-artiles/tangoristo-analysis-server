package com.tangoristo.server.model;

import com.tangoristo.server.services.impl.JapaneseVocabularyServiceImplTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestContextManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.io.IOUtils.readLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
@SpringBootTest
@Slf4j
public class TokenTest {
    private static final String DATA_PATH = "data/token_test_data.tsv";

    private TestContextManager testContextManager;

    @Parameterized.Parameter(value = 0)
    public Token token;

    @Parameterized.Parameter(value = 1)
    public List<String> expectedReadingList;

    @Parameterized.Parameter(value = 2)
    public List<String> expectedSurfaceList;

    @Parameterized.Parameters(name = "{index}: testGetAlignedReadingsToForms()[{0}] -> expectedReadingList \"{1}\", expectedSurfaceList \"{2}\"")
    public static Collection<Object[]> data() throws IOException {
        return loadTestData();
    }

    private static Collection<Object[]> loadTestData() throws IOException {
        InputStream inputStream = JapaneseVocabularyServiceImplTest.class.getClassLoader().getResourceAsStream(DATA_PATH);
        return readLines(inputStream, "UTF-8").stream()
                .skip(1)
                .map(TokenTest::asTestParams)
                .collect(Collectors.toList());
    }

    private static Object[] asTestParams(String line) {
        String[] lineSpl = line.split("\t", -1);
        List<String> expectedReadingList = Arrays.stream(lineSpl[0].split(",", -1)).collect(Collectors.toList());
        List<String> expectedSurfaceList = Arrays.stream(lineSpl[1].split(",", -1)).collect(Collectors.toList());
        Token token = Token.builder()
                .baseForm(lineSpl[2])
                .surfaceForm(lineSpl[3])
                .surfaceReading(lineSpl[4])
                .isInflected(Boolean.parseBoolean(lineSpl[5]))
                .partOfSpeech(lineSpl[6])
                .build();
        return new Object[] { token, expectedReadingList, expectedSurfaceList };
    }

    @Before
    public void setUp() throws Exception {
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);
    }

    @Test
    public void testGetAlignedReadingsToForms() {
        List<ReadingToSurfaceFormPair> alignedReadingsToForms = token.getAlignedReadingsToForms();

        String tokenReading = alignedReadingsToForms.stream().map(ReadingToSurfaceFormPair::getReading)
                .collect(Collectors.joining(","));
        String tokenSurface = alignedReadingsToForms.stream().map(ReadingToSurfaceFormPair::getSurfaceForm)
                .collect(Collectors.joining(","));

        log.info("{} -> {} / {}", token.toString(), tokenReading, tokenSurface);
        assertThat(expectedSurfaceList.size(), is(expectedReadingList.size()));
        assertThat(alignedReadingsToForms.size(), is(expectedReadingList.size()));
        assertThat(tokenReading, is(StringUtils.join(expectedReadingList, ',')));
        assertThat(tokenSurface, is(StringUtils.join(expectedSurfaceList, ',')));
    }
}
