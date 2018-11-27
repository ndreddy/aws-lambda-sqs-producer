package com.ndrd.cloud.sqs.producer.lambda;

import com.ndrd.cloud.sqs.producer.lambda.io.FunctionReq;
import com.ndrd.cloud.sqs.producer.lambda.io.FunctionRes;
import com.ndrd.cloud.sqs.producer.lambda.service.FunctionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

@Component("functionImpl")
public class ProducerFunction implements Function<Map<String, Object>, FunctionRes> {

    // Initialize the Log4j logger.
    private static final Logger logger = LogManager.getLogger(ProducerFunction.class);
    private static final String BODY = "body";
    private static final String REQUEST_CONTEXT = "requestContext";
    private static final String AUTHORIZER = "authorizer";
    private static final String REQUEST_ID = "requestId";
    private static final String ID_TOKEN = "IdToken";
    private static final String ACCESS_TOKEN = "AccessToken";
    private static final String REFRESH_TOKEN = "RefreshToken";
    private static final String PRINCIPAL_ID = "principalId";

    private final FunctionService functionService;

    public ProducerFunction(final FunctionService functionService) {
        this.functionService = functionService;
    }

    @Override
    public FunctionRes apply(Map<String, Object> input) {
        logger.debug("Starting lambda execution ProducerFunction.apply()");
//        input.forEach((key, value) -> System.out.println(key + ":" + value));

        final FunctionReq req = newRequest(input);
        final FunctionRes res = new FunctionRes();

        // Constraints blank input
        if (req.getBody() == null) {
            res.setBody("Bad Request. Message body is empty");
            res.setStatusCode(400);
            return res;
        }
        res.setBody(functionService.handleRequest(req));
        res.setStatusCode(200);
        return res;
    }

    /**
     * Creates new FunctionReq obj from the input
     *
     * @param map input map
     * @return inbound request object.
     */
    private FunctionReq newRequest(Map<String, Object> map) {
        FunctionReq iReq = new FunctionReq();
        logger.debug("Converting input into FunctionReq object");

        // Populates request body
        String body = (String) map.get(BODY);
        if (body == null) {
            logger.error("body is missing in the input");
            return iReq;
        }
        iReq.setBody(body);

        // RequestContext
        Map<String, Object> reqContext = (Map<String, Object>) map.get(REQUEST_CONTEXT);
        if (reqContext == null) {
            logger.error("RequestContext is missing in the input");
            return iReq;
        }
        // Request Id to uniquely identify the request.
        iReq.setRequestId((String) reqContext.get(REQUEST_ID));
        iReq.setStage((String)reqContext.get("stage"));

        // Authorizer will have tokens and principle
        Map<String, String> authorizer = (Map<String, String>) reqContext.get(AUTHORIZER);
        if (authorizer == null) {
            logger.error("Authorizer is missing in the RequestContext of the input");
            return iReq;
        }
       /* iReq.setIdToken(authorizer.get(ID_TOKEN));
        iReq.setAccessToken(authorizer.get(ACCESS_TOKEN));
        iReq.setRefreshToken(authorizer.get(REFRESH_TOKEN));*/
        iReq.setUserId(authorizer.get(PRINCIPAL_ID));

        Map<String,String> identity = (Map<String, String>) reqContext.get("identity");
        if(identity!=null){
            String apiKey = identity.get("apiKey");
            logger.info("API key = " + apiKey);
            iReq.setApiKey(apiKey);
        }

        return iReq;
    }
}
