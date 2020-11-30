package com.tangoristo.server.services;

import com.tangoristo.server.model.AnalysisRequest;

import java.util.List;

public interface QueueReaderService {
    List<AnalysisRequest> getMessages();
    int getMaxMessagesPerRequest();
}