package com.tangoristo.server.services;

import com.tangoristo.server.model.RubyHint;
import com.tangoristo.server.model.Token;
import com.tangoristo.server.model.VocabularyEntity;
import com.tangoristo.server.model.VocabularyLevel;

import java.util.List;

public interface VocabularyService {
    List<VocabularyEntity> getVocabulary(List<Token> tokens);
    List<VocabularyEntity> getVocabulary(List<Token> rawTokens, List<RubyHint> rubyHints);
    VocabularyLevel getVocabularyLevel(List<VocabularyEntity> vocabulary);
}
