package com.tangoristo.server.model;

import lombok.Value;

@Value
public class AnalysisResponse {
    private String structureKey;
    private VocabularyAnalysisResult titleAnalysis;
    private VocabularyAnalysisResult bodyAnalysis;
}
