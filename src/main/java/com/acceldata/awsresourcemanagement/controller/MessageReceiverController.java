package com.acceldata.awsresourcemanagement.controller;

import com.acceldata.awsresourcemanagement.feignclient.SlackFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/")
public class MessageReceiverController {

    @Value("${slack.signature.key}")
    private String signatureKey;

    @Value("${slack.signature.algo}")
    private String signatureAlgo;

    @Autowired
    private SlackFeignClient client;

    @PostMapping
    public ResponseEntity<?> receiveMessageFromSlack( @RequestHeader("x-slack-request-timestamp") String timestamp,
            @RequestHeader("x-slack-signature") String signature, @RequestBody String body )
    {
        try
        {
            if( !body.contains("payload") )
            {
                validateSignature(signature, timestamp, body);
            }
            else
            {
                String decode = URLDecoder.decode(body, StandardCharsets.UTF_8.name()).replace("payload=","");
                JsonNode jsonNode = new ObjectMapper().readValue(decode, JsonNode.class);
                String action = jsonNode.get("actions").get(0).get("value").asText();
                if( action.equals("stop") )
                {
                    log.debug("Stopping instance");
                    stopInstance();
                    client.publishMessage("{\"text\":\"Stopping the instance...\"}");
                }
            }
            log.debug("Request Body : {}", body);
            JsonNode jsonNode = new ObjectMapper().readValue(body, JsonNode.class);
            String text = jsonNode.get("event").get("text").asText();
            if( text.equalsIgnoreCase("hi") )
            {
                client.publishMessage("{\"text\":\"Hello\"}");
            }

            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch( Exception e )
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private void stopInstance()
    {
        // Replace with your AWS access key and secret key
        String accessKey = "";
        String secretKey = "";
        String instanceId = "";
        Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.US_EAST_1) // Change to the appropriate AWS region
                .credentialsProvider(() -> AwsBasicCredentials.create(accessKey, secretKey))
                .build();
        StopInstancesRequest request = StopInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        StopInstancesResponse response = ec2Client.stopInstances(request);
        System.out.println("Instance state after stopping: " + response.stoppingInstances().get(0).currentState());
    }

    @PostMapping("/aws")
    public ResponseEntity<?> receiveMessageAWS( @RequestBody JsonNode body )
    {
        try
        {
            String requestBody = "{\"type\":\"modal\",\"title\":{\"type\":\"plain_text\",\"text\":\"My App\",\"emoji\":true},\"blocks\":[{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"%s\"}},{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"If you are not responding in 5min. The instance will be stopped\"},\"accessory\":{\"type\":\"button\",\"text\":{\"type\":\"plain_text\",\"text\":\"Stop Imideatly\",\"emoji\":true},\"value\":\"stop\",\"action_id\":\"button-action\"}},{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"Do you wish to\"},\"accessory\":{\"type\":\"button\",\"text\":{\"type\":\"plain_text\",\"text\":\"Ignore\",\"emoji\":true},\"value\":\"ignore\",\"action_id\":\"button-action\"}}]}";
            System.out.println(body);
            String text = body.get("Records").get(0).get("Sns").get("Subject").asText().replaceAll("\"","");
            client.publishMessage(String.format(requestBody, text));
            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch( Exception e )
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private void validateSignature( String signature, String timestamp, String body ) throws Exception
    {
        try
        {
            String[] split = signature.split("=");
            String version = split[0];
            signature = split[1];
            String data = version + ":" + timestamp + ":" + body;
            String hmac = new HmacUtils(signatureAlgo, signatureKey).hmacHex(data);
            if( !hmac.equals(signature) )
                throw new Exception("Invalid Signature");
        }
        catch( Exception e )
        {
            throw new Exception("Invalid Signature");
        }
    }

}
