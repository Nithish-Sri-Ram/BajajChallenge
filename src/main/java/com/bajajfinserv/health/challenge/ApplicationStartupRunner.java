package com.bajajfinserv.health.challenge;

import com.bajajfinserv.health.challenge.model.Request;
import com.bajajfinserv.health.challenge.model.User;
import com.bajajfinserv.health.challenge.model.WebhookRequest;
import com.bajajfinserv.health.challenge.service.ProblemSolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ApplicationStartupRunner implements CommandLineRunner {

    private final RestTemplate restTemplate;
    private final ProblemSolver problemSolver;

    private static final int MAX_RETRIES = 4;

    public ApplicationStartupRunner(RestTemplate restTemplate,
                                    ProblemSolver problemSolver,
                                    ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.problemSolver = problemSolver;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Application started. Making initial request...");

        // Step 1: Make initial request
        Request request = new Request();
        request.setName("Nithish Sri Ram");
        request.setRegNo("AP22110010837");
        request.setEmail("nithishsriram_tt@srmap.edu.in");

        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Request> entity = new HttpEntity<>(request, headers);

        try {
            Map<String, Object> responseMap = restTemplate.postForObject(url, entity, Map.class);
            log.info("Received response: {}", responseMap);

            if (responseMap != null) {
                String webhookUrl = (String) responseMap.get("webhook");
                String accessToken = (String) responseMap.get("accessToken");

                Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
                List<User> users = extractUsers(dataMap);

                List<List<Integer>> mutualPairs = problemSolver.findMutualFollowers(users);

                WebhookRequest webhookRequest = new WebhookRequest();
                webhookRequest.setRegNo(request.getRegNo());
                webhookRequest.setOutcome(mutualPairs);

                sendToWebhookWithRetry(webhookUrl, accessToken, webhookRequest);
            }
        } catch (Exception e) {
            log.error("Error in processing: ", e);
        }
    }

    private List<User> extractUsers(Map<String, Object> dataMap) {
        List<User> users = new ArrayList<>();

        if (dataMap != null) {
            Object usersObj = dataMap.get("users");

            if (usersObj instanceof Map) {
                Map<String, Object> usersMap = (Map<String, Object>) usersObj;
                if (usersMap.containsKey("users")) {
                    List<Map<String, Object>> userMaps = (List<Map<String, Object>>) usersMap.get("users");
                    return convertToUsers(userMaps);
                }
            } else if (usersObj instanceof List) {
                List<Map<String, Object>> userMaps = (List<Map<String, Object>>) usersObj;
                return convertToUsers(userMaps);
            }
        }

        return users;
    }

    private List<User> convertToUsers(List<Map<String, Object>> userMaps) {
        List<User> users = new ArrayList<>();
        for (Map<String, Object> userMap : userMaps) {
            User user = new User();
            user.setId(((Number) userMap.get("id")).intValue());
            user.setName((String) userMap.get("name"));

            Object followsObj = userMap.get("follows");
            if (followsObj instanceof List) {
                List<Integer> follows = new ArrayList<>();
                for (Object followId : (List<?>) followsObj) {
                    if (followId instanceof Number) {
                        follows.add(((Number) followId).intValue());
                    }
                }
                user.setFollows(follows);
            }

            users.add(user);
        }
        return users;
    }

    private void sendToWebhookWithRetry(String webhookUrl, String accessToken, WebhookRequest request) {
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", accessToken);

                HttpEntity<WebhookRequest> entity = new HttpEntity<>(request, headers);

                log.info("Attempting to post to webhook (attempt {})", retryCount + 1);
                restTemplate.postForObject(webhookUrl, entity, String.class);

                log.info("Successfully posted to webhook!");
                return;
            } catch (Exception e) {
                retryCount++;
                log.error("Failed to post to webhook (attempt {}): {}", retryCount, e.getMessage());

                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        log.error("Max retries reached. Failed to post to webhook.");
    }
}