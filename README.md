# aws-lambda-sqs-producer

Configure Handler in the AWS console.

New lambda -> Function Code -> Handler ->com.ndrd.cloud.sqs.producer.lambda.aws.ProducerFunctionHandler
Upload the jar to deploy the function

mvn -> install -> aws-lambda-sqs-producer-1.0-SNAPSHOT-aws.jar
The function code expects lambda env variables configured in

Lambda configs
Concurrency - Unreserved account concurrency 975
Memory (MB) - 512MB (Optimize using load testing tools like Gatling)
Timeout - 59 sec (Optimiese using perf testing like x-ray)

Spring Cloud

ProducerFunctionHandler -> SpringBootRequestHandler -> SpringFunctionInitializer -> application.properties -> function.name -> ProducerFunction -> FunctionService -> MessageService ->AwsSQSExtendedService

ProducerFunctionHandler extends SpringBootRequestHandler which extends SpringFunctionInitializer which is where the magic happens.

When a request is received, the handler will attempt to initialize the spring context.During initialization, it will look up the property function.name defined in the application.properties which is the name of function component bean that would of been discovered during component scanning.