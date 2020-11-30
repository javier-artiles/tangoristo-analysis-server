package com.tangoristo.server.services.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.tangoristo.server.model.AnalysisResponse;
import com.tangoristo.server.services.StorageService;

public class S3StorageService implements StorageService {

    private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writerFor(AnalysisResponse.class);

    private final AmazonS3 amazonS3;
    private final String bucketName;
    private final String keyPrefix;

    public S3StorageService(AmazonS3 amazonS3, String bucketName, String keyPrefix) {
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public void store(AnalysisResponse analysisResponse, String keySuffix) throws StorageServiceException {
        String key = String.format("%s/%s", keyPrefix, keySuffix);
        try {
            String content = OBJECT_WRITER.writeValueAsString(analysisResponse);
            amazonS3.putObject(bucketName, key, content);
        } catch (JsonProcessingException e) {
            String message = String.format("Failed to store object on S3 (bucket = %s, key = %s)", bucketName, key);
            throw new StorageServiceException(message, e);
        }
    }

}
