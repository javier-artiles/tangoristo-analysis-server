package com.tangoristo.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentWithAnalyzedVocabulary {
    private Document document;
    private VocabularyAnalysisResult vocabularyAnalysis;
}
