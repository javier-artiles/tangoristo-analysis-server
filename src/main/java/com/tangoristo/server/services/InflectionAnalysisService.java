package com.tangoristo.server.services;

import com.tangoristo.server.model.InflectionAnalysisResult;

import java.util.Optional;

public interface InflectionAnalysisService {
    Optional<InflectionAnalysisResult> analyze(String surfaceForm, String dictionaryForm);
}
