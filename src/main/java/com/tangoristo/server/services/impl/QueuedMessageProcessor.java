package com.tangoristo.server.services.impl;

import com.tangoristo.server.model.AnalysisRequest;
import com.tangoristo.server.model.AnalysisResponse;
import com.tangoristo.server.services.StorageService;
import com.tangoristo.server.services.MessageHandler;
import com.tangoristo.server.services.QueueReaderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PreDestroy;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
public class QueuedMessageProcessor implements Runnable {

    private static final int ONE_SECOND_IN_MILLISECONDS = 1000;
    private static final int ONE_MINUTE_IN_MILLISECONDS = ONE_SECOND_IN_MILLISECONDS * 60;

    private final QueueReaderService requestQueueService;
    private final MessageHandler messageHandler;
    private final StorageService storageService;
    private final ExecutorService messageProcessingThreadPool;
    private final ExecutorService synchronousQueueRetrievingThread =
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
    private boolean isShuttingDown = false;

    public QueuedMessageProcessor(
            QueueReaderService requestQueueService,
            MessageHandler messageHandler,
            StorageService storageService,
            ExecutorService messageProcessingThreadPool
    ) {
        this.requestQueueService = requestQueueService;
        this.messageHandler = messageHandler;
        this.storageService = storageService;
        this.messageProcessingThreadPool = messageProcessingThreadPool;
    }

    @Override
    @Scheduled(fixedDelay = ONE_SECOND_IN_MILLISECONDS)
    public void run() {
        if (!isShuttingDown) {
            try {
                try {
                    int numMessages = synchronousQueueRetrievingThread.submit(this::retrieveAndProcessMessages).get();
                    if (numMessages == 0) {
                        Thread.sleep(ONE_MINUTE_IN_MILLISECONDS);
                    }
                } catch (RejectedExecutionException e) {
                    log.info("Scheduled run failed as synchronous queue retrieving thread is busy - this is expected. " +
                            "Waiting for {} milliseconds before continuing.", ONE_MINUTE_IN_MILLISECONDS);
                    Thread.sleep(ONE_MINUTE_IN_MILLISECONDS);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Unexpected exception: ", e);
            }
        }
    }

    private int retrieveAndProcessMessages() {
        try {
            List<AnalysisRequest> messages = requestQueueService.getMessages();
            messages.forEach(message -> messageProcessingThreadPool.submit(() -> this.processMessage(message)));
            return messages.size();
        } catch (Exception e) {
            log.error("{}: Failed to process queued messages. Up to {} messages may have failed and been lost.",
                    this.getClass().getSimpleName(), requestQueueService.getMaxMessagesPerRequest(), e);
            return 0;
        }
    }

    private void processMessage(AnalysisRequest message) {
        try {
            log.info("Analyzing {}", message.getStructureKey());
            AnalysisResponse messageResponse = messageHandler.processMessage(message);
            log.info("Storing result for {}", message.getStructureKey());
            storageService.store(messageResponse, messageResponse.getStructureKey().replaceFirst("structure/", ""));
            log.info("Finished processing {}", message.getStructureKey());
        } catch (Exception e) {
            log.error("Processing {} failed unexpectedly", message, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        isShuttingDown = true;
        Stream.of(
                new SimpleEntry<>("Queue Retrieving Thread", synchronousQueueRetrievingThread),
                new SimpleEntry<>("Message Processing Thread Pool", messageProcessingThreadPool)
        ).forEach(entry -> performGracefulShutdown(entry.getKey(), entry.getValue()));
    }

    private static void performGracefulShutdown(String threadPoolName, ExecutorService threadPool) {
        long shutdownTimeoutSeconds = 120L;
        long shutdownNowTimeoutSeconds = 10L;

        try {
            log.info("Shutting down {}", threadPoolName);
            threadPool.shutdown();

            log.info("Waiting up to {} seconds for {} tasks to complete.", shutdownTimeoutSeconds, threadPoolName);
            if (!threadPool.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                threadPool.awaitTermination(shutdownNowTimeoutSeconds, TimeUnit.SECONDS);
                log.info("{} isTerminated: {}", threadPoolName, threadPool.isTerminated());
            } else {
                log.info("{} terminated gracefully.", threadPoolName);
            }
        } catch (InterruptedException e) {
            log.error("{} termination was interrupted.", threadPoolName, e);
        }
    }
}
