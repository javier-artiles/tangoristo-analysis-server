package com.tangoristo.server.services;

import com.tangoristo.server.model.AnalysisResponse;
import com.tangoristo.server.services.impl.StorageServiceException;

public interface StorageService {

    void store(AnalysisResponse analysisResponse, String keySuffix) throws StorageServiceException;

}
