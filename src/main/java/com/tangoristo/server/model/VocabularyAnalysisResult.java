package com.tangoristo.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Builder
@AllArgsConstructor
public class VocabularyAnalysisResult {
    private String language;
    private VocabularyLevel vocabularyLevel;
    private List<LinkedSubstring> linkedText;
    private List<VocabularyEntity> vocabulary;
}
