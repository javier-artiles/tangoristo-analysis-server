package com.tangoristo.server.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.tangoristo.server.services.QueueReaderService;
import com.tangoristo.server.services.impl.SQSQueueReaderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Configuration
public class SQSConfig {

    private static final String ANALYSIS_REQUEST_QUEUE_URL = "${settings.aws.sqs.analysisRequestQueueUrl}";
    private static final String MAX_NUMBER_OF_MESSAGES = "${settings.aws.sqs.maxNumberOfMessages}";

    @Bean
    public AmazonSQS getRealAmazonSQS() {
        return AmazonSQSClientBuilder.standard()
                .withRegion(Regions.US_WEST_2)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Bean
    public QueueReaderService getAnalysisQueueReaderService(
            AmazonSQS amazonSqs,
            @Value(ANALYSIS_REQUEST_QUEUE_URL) String queueUrl,
            @Value(MAX_NUMBER_OF_MESSAGES) @Min(1) @Max(10) int maxNumberOfMessages
    ) {
        return new SQSQueueReaderService(amazonSqs, queueUrl, maxNumberOfMessages);
    }

}
