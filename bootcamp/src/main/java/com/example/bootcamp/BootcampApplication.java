package com.example.bootcamp;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Say;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Message;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@SpringBootApplication
@RestController
public class BootcampApplication {

    public static final String NPS_API_KEY = System.getenv("NPS_API_KEY");

    public static void main(String[] args) {
        SpringApplication.run(BootcampApplication.class, args);
    }

    @RequestMapping(value = "/sms", produces = { MediaType.APPLICATION_XML_VALUE })
    public String getStateParks(@RequestBody String smsBody) throws URISyntaxException {
        //Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        //String smsBody = requestBody.getParameter("Body");

        int startIndex = smsBody.indexOf("Body=");
        int endIndex = smsBody.indexOf("&", startIndex);
        String stateCode = smsBody.substring(startIndex+5,endIndex);
        System.out.println(stateCode);

        if (stateCode.length() != 2 || stateCode.matches(".*\\d.*")){
            Body responseBody = new Body
                    .Builder("SMS a State Code to get a list of National State Parks.\n\nExample: GA, CA, FL...")
                    .build();
            Message sms = new Message
                    .Builder()
                    .body(responseBody)
                    .build();
            MessagingResponse responseTwiml = new MessagingResponse
                    .Builder()
                    .message(sms)
                    .build();
            return responseTwiml.toXml();

        } else {
            final String baseUrl = "https://developer.nps.gov/api/v1/parks?stateCode=" + stateCode;
            URI uri = new URI(baseUrl);
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", NPS_API_KEY);

            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

            ResponseEntity<String> result = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
            String jsonResponse = result.getBody();

            ReadContext ctx = JsonPath.parse(jsonResponse);
            List<String> parkNames = ctx.read("$..fullName");

            String firstLine = String.format("National Parks in State %s:\n\n", stateCode);

            Body responseBody = new Body
                    .Builder(firstLine + String.join(", ", parkNames))
                    .build();
            Message sms = new Message
                    .Builder()
                    .body(responseBody)
                    .build();
            MessagingResponse responseTwiml = new MessagingResponse
                    .Builder()
                    .message(sms)
                    .build();
            return responseTwiml.toXml();
        }
    }
}