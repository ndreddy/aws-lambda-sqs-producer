package com.ndrd.cloud.sqs.producer.lambda.aws;

import com.ndrd.cloud.sqs.producer.lambda.io.FunctionRes;
import org.springframework.cloud.function.adapter.aws.SpringBootRequestHandler;

import java.util.Map;

public class ProducerFunctionHandler extends SpringBootRequestHandler<Map<String, Object>, FunctionRes> {
}
