package com.tangoristo.server.services.impl;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.tangoristo.server.model.AnalysisRequest;
import com.tangoristo.server.services.QueueReaderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Slf4j
public class SQSQueueReaderService implements QueueReaderService {

    private static final ObjectReader OBJECT_READER = new ObjectMapper().readerFor(AnalysisRequest.class);

    private final AmazonSQS amazonSqs;
    private final String queueUrl;
    private final int maxNumberOfMessages;

    public SQSQueueReaderService(
            AmazonSQS amazonSqs,
            String queueUrl,
            int maxNumberOfMessages
    ) {
        this.amazonSqs = amazonSqs;
        this.queueUrl = queueUrl;
        this.maxNumberOfMessages = maxNumberOfMessages;
    }

    @Override
    public List<AnalysisRequest> getMessages() {
        return receiveMessages(queueUrl).stream()
                .map(this::deserialize)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public int getMaxMessagesPerRequest() {
        return maxNumberOfMessages;
    }

    private Optional<AnalysisRequest> deserialize(Message message) {
        try {
            String gzippedBase64MesageBody = message.getBody();
            String messageBody = uncompressBase64String(gzippedBase64MesageBody);
            AnalysisRequest readObject = OBJECT_READER.readValue(messageBody);
            return Optional.of(readObject);
        } catch (IOException e) {
            log.warn("Failed to deserialize SQS message '{}'", message.toString(), e);
            return Optional.empty();
        }
    }

    private List<Message> receiveMessages(String queueUrl) {
        ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(maxNumberOfMessages);
        List<Message> messages = amazonSqs.receiveMessage(request).getMessages();
        if (messages.size() > 0) {
            log.info("Received {} messages from {}", messages.size(), queueUrl);
        }
        deleteMessagesFromQueue(queueUrl, messages);
        return messages;
    }

    private void deleteMessagesFromQueue(String queueUrl, List<Message> messages) {
        messages.forEach(msg -> amazonSqs.deleteMessage(queueUrl, msg.getReceiptHandle()));
    }

    private static String uncompressBase64String(String base64String) throws IOException {
        byte[] compressed = Base64.decodeBase64(base64String);
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(bis);
        BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        gis.close();
        bis.close();
        return sb.toString();
    }
}
