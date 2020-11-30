package com.tangoristo.server.services.impl;

import com.tangoristo.server.model.InflectionAnalysisResult;
import com.tangoristo.server.services.InflectionAnalysisService;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.io.IOUtils.readLines;

@Service
@Slf4j
public class JapaneseInflectionAnalysisServiceImpl implements InflectionAnalysisService {
    private static final String RULES_FILE = "data/ja/deinflect.dat";
    private Set<InflectionRule> inflectionRules;


    public JapaneseInflectionAnalysisServiceImpl() throws IOException {
        this.inflectionRules = loadInflectionRules(RULES_FILE);
    }

    private static Set<InflectionRule> loadInflectionRules(String rulesFile) throws IOException {
        InputStream inputStream = JapaneseInflectionAnalysisServiceImpl.class.getClassLoader().getResourceAsStream(rulesFile);
        List<String> lines = readLines(inputStream, "UTF-8");

        List<String> descriptions = lines.stream()
                .skip(1)
                .filter(lineSpl -> !lineSpl.contains("\t"))
                .collect(Collectors.toList());

        return lines.stream()
                .skip(1)
                .map(line -> line.split("\t"))
                .filter(lineSpl -> lineSpl.length == 4)
                .map(lineSpl -> JapaneseInflectionAnalysisServiceImpl.asInflectionRule(lineSpl, descriptions))
                .collect(Collectors.toSet());
    }

    private static InflectionRule asInflectionRule(String[] lineSpl, List<String> descriptions) {
        return InflectionRule.builder()
                .inflection(lineSpl[0])
                .base(lineSpl[1])
                .type(Integer.parseInt(lineSpl[2]))
                .description(descriptions.get(Integer.parseInt(lineSpl[3])))
                .build();
    }

    @Override
    public Optional<InflectionAnalysisResult> analyze(String surfaceForm, String dictionaryForm) {
        List<InflectionRule> matchesByLength = inflectionRules.stream()
                .filter(rule -> dictionaryForm.endsWith(rule.getBase()))
                .filter(rule -> surfaceForm.endsWith(rule.getInflection()))
                .sorted((r1, r2) -> r1.getInflection().length() > r2.getInflection().length() ? -1 : 1)
                .collect(Collectors.toList());

        if (matchesByLength.size() == 0) {
            return Optional.empty();
        }

        int topMatchInflectionLength = matchesByLength.get(0).getInflection().length();
        List<InflectionRule> tiedMatches = matchesByLength.stream()
                .filter(r -> r.getInflection().length() == topMatchInflectionLength)
                .collect(Collectors.toList());
        if (tiedMatches.size() > 1) {
            log.trace("There is a tie applying inflection rules for surfaceForm = {}, dictionaryForm = {}: {}",
                    surfaceForm, dictionaryForm, tiedMatches);
        }

        InflectionRule topMatchingRule = matchesByLength.get(0);

        InflectionAnalysisResult result = InflectionAnalysisResult.builder()
                .inflectionForm(topMatchingRule.getInflection())
                .inflectionBase(topMatchingRule.getBase())
                .inflectionName(topMatchingRule.getDescription())
                .build();

        return Optional.of(result);
    }

    @Value
    @Builder
    @ToString
    private static class InflectionRule {
        private String inflection;
        private String base;
        private int type;
        private String description;
    }

}
