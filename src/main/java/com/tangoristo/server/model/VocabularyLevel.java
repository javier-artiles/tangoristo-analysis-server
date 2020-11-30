package com.tangoristo.server.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class VocabularyLevel {
    private List<String> increasingDifficultyLevelLabels;
    private Map<String, Integer> levelToFrequency;
    private int unknownLevelFrequency;
}
