package com.ndrd.cloud.sqs.producer.lambda.service;

public interface MessageService {
    void sendMessage(String message, String msgGroupId, String stage);
}
