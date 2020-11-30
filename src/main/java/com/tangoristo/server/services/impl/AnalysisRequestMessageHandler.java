package com.tangoristo.server.services.impl;

import com.tangoristo.server.model.AnalysisRequest;
import com.tangoristo.server.model.AnalysisResponse;
import com.tangoristo.server.model.RubyHint;
import com.tangoristo.server.model.VocabularyAnalysisResult;
import com.tangoristo.server.services.MessageHandler;
import com.tangoristo.server.services.TextAnalysisService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisRequestMessageHandler implements MessageHandler {

    private final TextAnalysisService textAnalysisService;

    public AnalysisRequestMessageHandler(TextAnalysisService textAnalysisService) {
        this.textAnalysisService = textAnalysisService;
    }

    @Override
    public AnalysisResponse processMessage(AnalysisRequest analysisRequest) {
        String title = analysisRequest.getTitle();
        String body = analysisRequest.getBody();
        List<RubyHint> rubyHints = analysisRequest.getRubyHints();
        VocabularyAnalysisResult titleAnalysis = textAnalysisService.analyze(title, rubyHints);
        VocabularyAnalysisResult bodyAnalysis = textAnalysisService.analyze(body, rubyHints);
        return new AnalysisResponse(
                analysisRequest.getStructureKey(),
                titleAnalysis,
                bodyAnalysis
        );
    }
}
