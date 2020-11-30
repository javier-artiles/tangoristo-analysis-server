package com.tangoristo.server.services;

import com.tangoristo.server.model.RubyHint;
import com.tangoristo.server.model.VocabularyAnalysisResult;

import java.util.List;


public interface TextAnalysisService {
    VocabularyAnalysisResult analyze(String text);
    VocabularyAnalysisResult analyze(String text, List<RubyHint> rubyHintList);
}
