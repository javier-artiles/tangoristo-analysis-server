package com.tangoristo.server.config;

import com.amazonaws.services.s3.AmazonS3;
import com.tangoristo.server.services.QueueReaderService;
import com.tangoristo.server.services.StorageService;
import com.tangoristo.server.services.impl.AnalysisRequestMessageHandler;
import com.tangoristo.server.services.impl.QueuedMessageProcessor;
import com.tangoristo.server.services.impl.S3StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class MessageProcessorConfig {

    private static final String ANALYSIS_QUEUE_PROCESSOR_IS_ENABLED = "settings.analysisQueueProcessorEnabled";
    private static final String ANALYSIS_QUEUE_MAX_CONCURRENT_MESSAGES = "${settings.analysisQueueMaxConcurrentMessages}";
    private static final String BUCKET_NAME = "${settings.aws.s3.bucketName}";
    private static final String KEY_PREFIX = "${settings.aws.s3.keyPrefix}";

    @Bean
    @ConditionalOnProperty(name = ANALYSIS_QUEUE_PROCESSOR_IS_ENABLED, havingValue = "true")
    public QueuedMessageProcessor getQueueMessageProcessor(
            QueueReaderService requestQueueService,
            AnalysisRequestMessageHandler messageHandler,
            AmazonS3 amazonS3,
            @Value(ANALYSIS_QUEUE_MAX_CONCURRENT_MESSAGES) int maxConcurrentMessages,
            @Value(BUCKET_NAME) String bucketName,
            @Value(KEY_PREFIX) String keyPrefix
    ){
        ThreadPoolExecutor messageProcessingThreadPool = newFixedCapacityMessageProcessingThreadPool(maxConcurrentMessages);
        StorageService storageService = new S3StorageService(amazonS3, bucketName, keyPrefix);
        return new QueuedMessageProcessor(
                requestQueueService,
                messageHandler,
                storageService,
                messageProcessingThreadPool
        );
    }

    private static ThreadPoolExecutor newFixedCapacityMessageProcessingThreadPool(int concurrency) {
        return new ThreadPoolExecutor(concurrency, concurrency, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(concurrency), new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
