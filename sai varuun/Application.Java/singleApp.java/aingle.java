package com.example.mutualfollowers;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@SpringBootApplication
public class SingleApp {

    public static void main(String[] args) {
        SpringApplication.run(SingleApp.class, args);
    }

    @Component
    public static class StartupRunner implements CommandLineRunner {

        private final RestTemplate restTemplate = new RestTemplate();

        @Override
        public void run(String... args) {
            String registrationUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook";

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("name", "John Doe");
            requestBody.put("regNo", "REG12347");
            requestBody.put("email", "john@example.com");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(registrationUrl, request, Map.class);
                Map<String, Object> responseBody = response.getBody();

                String webhookUrl = (String) responseBody.get("webhook");
                String accessToken = (String) responseBody.get("accessToken");

                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                List<Map<String, Object>> users = (List<Map<String, Object>>) data.get("users");

                List<List<Integer>> outcome = findMutualFollowers(users);

                Map<String, Object> result = new HashMap<>();
                result.put("regNo", "REG12347");
                result.put("outcome", outcome);

                HttpHeaders webhookHeaders = new HttpHeaders();
                webhookHeaders.setContentType(MediaType.APPLICATION_JSON);
                webhookHeaders.set("Authorization", accessToken);

                HttpEntity<Map<String, Object>> webhookRequest = new HttpEntity<>(result, webhookHeaders);

                int attempts = 0;
                while (attempts < 4) {
                    try {
                        ResponseEntity<String> webhookResponse = restTemplate.postForEntity(webhookUrl, webhookRequest, String.class);
                        if (webhookResponse.getStatusCode().is2xxSuccessful()) {
                            System.out.println("✅ Successfully sent result to webhook.");
                            break;
                        }
                    } catch (Exception ex) {
                        System.out.println("❌ Attempt " + (attempts + 1) + " failed. Retrying...");
                        attempts++;
                        Thread.sleep(1000);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private List<List<Integer>> findMutualFollowers(List<Map<String, Object>> users) {
            Map<Integer, Set<Integer>> followMap = new HashMap<>();
            for (Map<String, Object> user : users) {
                int id = (int) user.get("id");
                List<Integer> follows = (List<Integer>) user.get("follows");
                followMap.put(id, new HashSet<>(follows));
            }

            Set<List<Integer>> result = new HashSet<>();
            for (Map.Entry<Integer, Set<Integer>> entry : followMap.entrySet()) {
                int userId = entry.getKey();
                for (int followedId : entry.getValue()) {
                    if (followMap.containsKey(followedId) && followMap.get(followedId).contains(userId)) {
                        List<Integer> pair = Arrays.asList(Math.min(userId, followedId), Math.max(userId, followedId));
                        result.add(pair);
                    }
                }
            }
            return new ArrayList<>(result);
        }
    }
}
