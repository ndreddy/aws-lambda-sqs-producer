package com.ndrd.cloud.sqs.producer.lambda.service;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class AwsSQSExtendedService implements MessageService {

    static final Logger logger = LogManager.getLogger(AwsSQSExtendedService.class);

    private static final String INBOUND_MESSAGE_QUEUE = "inboundMessageQueue";
    private static final String DEFAULT = "default";

    /**
     * Sends message to SQS backed by S3.
     *
     * @param message payload
     * @param apiKey  used of queue lookup and as message group id.
     * @param stage   deployment stage like dev or test or prod
     */
    public void sendMessage(String message, String apiKey, String stage) {
        logger.debug("Sending message to SQS ");

        // 1. S3 Bucket Config for Extended SQS
        String bucketName = lookupBucketName(stage);
        createBucketIfNotExists(bucketName);

        // 2. SQS Config
        String queueName = lookupQueueName(apiKey, stage);
        String queueUrl = createFIFOQueueIfNotExists(queueName);

        // 3. SQS Extended Client Config
        final AmazonSQSExtendedClient sqsExtended = newAmazonSQSExtendedClient(bucketName);

        // 4. Sends messages to the queue
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(message)
                .withMessageGroupId(apiKey);
        sqsExtended.sendMessage(sendMessageRequest);
    }

    /**
     * Looksup the bucket name
     *
     * @param stage deployment stage
     * @return bucket name
     */
    private String lookupBucketName(String stage) {
        String key = "/" + stage + "/default/s3Bucket";
        return getParameterFromSSMByName(key);
    }


    /**
     * Looks up the queue name in config.
     *
     * @param apiKey user id
     * @param stage deploy stage
     * @return queue name
     */
    private static String lookupQueueName(String apiKey, String stage) {
        String value = null;
        StringBuilder key = new StringBuilder("/" + stage + "/");
        if(apiKey!=null){
            key.append(apiKey);
            key.append("/" + INBOUND_MESSAGE_QUEUE);
            try {
                value = getParameterFromSSMByName(key.toString());
            } catch (ParameterNotFoundException pe){
                key = new StringBuilder("/" + stage + "/");
                key.append(DEFAULT);
                key.append("/" + INBOUND_MESSAGE_QUEUE);
                value =  getParameterFromSSMByName(key.toString());
            }
        }

       logger.debug("Parameter value = " + value);
        return value;
    }


    /**
     * Instantiates SQS Extended d=Client
     *
     * @param bucketName bucket name
     * @return SQS Extended Client
     */
    private static AmazonSQSExtendedClient newAmazonSQSExtendedClient(String bucketName) {
        logger.debug("Configuring SQS Extended");

        /*
         * Create a new instance of the builder with all defaults (credentials
         * and region) set automatically. For more information, see
         * Creating Service Clients in the AWS SDK for Java Developer Guide.
         */
        final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

        /*
         * Set the Amazon S3 bucket name, and then set a lifecycle rule on the
         * bucket to permanently delete objects 14 days after each object's
         * creation date.
         */
        final BucketLifecycleConfiguration.Rule expirationRule =
                new BucketLifecycleConfiguration.Rule();
        expirationRule.withExpirationInDays(14).withStatus("Enabled");
        final BucketLifecycleConfiguration lifecycleConfig =
                new BucketLifecycleConfiguration().withRules(expirationRule);


        s3.setBucketLifecycleConfiguration(bucketName, lifecycleConfig);
        logger.debug("S3 Bucket configured for SQS Extended");

        /*
         * Set the Amazon SQS extended client configuration with large payload
         * support enabled.
         */
        final ExtendedClientConfiguration extendedClientConfig =
                new ExtendedClientConfiguration()
                        .withLargePayloadSupportEnabled(s3, bucketName);

        return new AmazonSQSExtendedClient(AmazonSQSClientBuilder
                .defaultClient(), extendedClientConfig);
    }

    /**
     * Creates FIFO Queue if not created already.
     *
     * @param queueName queue name
     * @return queue url
     */
    private static String createFIFOQueueIfNotExists(String queueName) {
        /*
         * Create a new instance of the builder with all defaults (credentials
         * and region) set automatically. For more information, see
         * Creating Service Clients in the AWS SDK for Java Developer Guide.
         */
        final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();


        // Create a FIFO queue.
        final Map<String, String> attributes = new HashMap<>();

        // A FIFO queue must have the FifoQueue attribute set to true.
        attributes.put("FifoQueue", "true");

        /*
         * If the user doesn't provide a MessageDeduplicationId, generate a
         * MessageDeduplicationId based on the content.
         */
        attributes.put("ContentBasedDeduplication", "true");

        // The FIFO queue name must end with the .fifo suffix.
        final CreateQueueRequest createQueueRequest =
                new CreateQueueRequest(queueName)
                        .withAttributes(attributes);

        try {
            String qUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
            System.out.format("SQS %s created.\n", queueName);
            return qUrl;
        } catch (AmazonSQSException e) {
            if (!e.getErrorCode().equals("QueueAlreadyExists")) {
                throw e;
            } else {
                System.out.format("Queue %s already exists \n",  queueName);
            }
        }

        return sqs.getQueueUrl(queueName).getQueueUrl();

    }

    /**
     * Creates S3 bucket.
     *
     * @param bucketName bucket name.
     */
    private static void createBucketIfNotExists(String bucketName) {

        final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
        if (s3.doesBucketExistV2(bucketName)) {
            System.out.format("Bucket %s already exists.\n", bucketName);
        } else {
            try {
                s3.createBucket(bucketName);
                System.out.format("Bucket %s created.\n", bucketName);
            } catch (AmazonS3Exception e) {
                System.err.println(e.getErrorMessage());
            }
        }
    }

    /**
     * Gets parameter from AWS SSM Parameter store
     *
     * @param key key of the parameter store
     * @return value
     */
    private static String getParameterFromSSMByName(String key) {
        String value = null;
        logger.debug("Accessing Parameter Store with key = " + key);
        AWSSimpleSystemsManagement ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
        GetParameterRequest parameterRequest = new GetParameterRequest();
        parameterRequest.withName(key).setWithDecryption(Boolean.TRUE);
        GetParameterResult parameterResult = ssmClient.getParameter(parameterRequest);
        Parameter param = parameterResult.getParameter();
        if (param != null) {
            value = param.getValue();
        }

        logger.debug("Parameter Store returns value = " + value);
        return value;
    }
}



