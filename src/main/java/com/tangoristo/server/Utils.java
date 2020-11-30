package com.tangoristo.server;

import com.tangoristo.server.model.VocabularyLevel;

public class Utils {
    public static double getVocabularyLevelScore(VocabularyLevel vocabularyLevel) {
        double sum = vocabularyLevel.getIncreasingDifficultyLevelLabels().stream()
                .mapToInt(level -> vocabularyLevel.getLevelToFrequency().getOrDefault(level, 0))
                .sum();
        double score = 0;
        for (int i = 0; i < vocabularyLevel.getIncreasingDifficultyLevelLabels().size(); i++) {
            String level = vocabularyLevel.getIncreasingDifficultyLevelLabels().get(i);
            int freq = vocabularyLevel.getLevelToFrequency().getOrDefault(level, 0);
            double weight = Math.pow(i, 2);
            score += (freq / sum) * weight;
        }
        return score;
    }
}
