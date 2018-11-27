package com.ndrd.cloud.sqs.producer.lambda.service;

import com.ndrd.cloud.sqs.producer.lambda.io.FunctionReq;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.stereotype.Service;

@Service
public class FunctionService {

    private MessageService messageService;

    public FunctionService(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Process the request
     *
     * @param request input from the API Gateway
     * @return String handleRequest status message
     */
    public String handleRequest(FunctionReq request) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        messageService.sendMessage(gson.toJson(request), request.getApiKey(), request.getStage());
        return "Request accepted, request Id = " + request.getRequestId();
    }


}
