# aws-lambda-sqs-producer

Configure Handler in the AWS console.

New lambda -> Function Code -> Handler ->com.ndrd.cloud.sqs.producer.lambda.aws.ProducerFunctionHandler
Upload the jar to deploy the function

mvn -> install -> aws-lambda-sqs-producer-1.0-SNAPSHOT-aws.jar
The function code expects lambda env variables configured in

Lambda configs
Concurrency - Unreserved account concurrency 975


Spring Cloud

How the AuthorizerFunction gets invoked via the AuthorizerFunctionHandler?
AuthorizerFunctionHandler -> SpringBootRequestHandler -> SpringFunctionInitializer -> application.properties -> function.name -> AuthorizeFunction
AuthorizerFunctionHandler extends SpringBootRequestHandler which extends SpringFunctionInitializer which is where the magic happens.

When a request is received, the handler will attempt to initialize the spring context.During initialization, it will look up the property function.name defined in the application.properties which is the name of function component bean that would of been discovered during component scanning.