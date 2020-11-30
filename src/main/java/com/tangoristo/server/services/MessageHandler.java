package com.tangoristo.server.services;

import com.tangoristo.server.model.AnalysisRequest;
import com.tangoristo.server.model.AnalysisResponse;

public interface MessageHandler {
    AnalysisResponse processMessage(AnalysisRequest message);
}