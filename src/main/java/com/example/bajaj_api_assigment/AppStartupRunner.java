package com.example.bajaj_api_assigment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class AppStartupRunner implements CommandLineRunner {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.name}")
    private String name;

    @Value("${app.regNo}")
    private String regNo;

    @Value("${app.email}")
    private String email;

    @Override
    public void run(String... args) {
        try {

            String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
            Map<String, String> generateRequest = Map.of(
                    "name", name,
                    "regNo", regNo,
                    "email", email
            );

            ResponseEntity<Map> generateResponse = restTemplate.postForEntity(generateUrl, generateRequest, Map.class);

            String webhookUrl = (String) generateResponse.getBody().get("webhook");
            String accessToken = (String) generateResponse.getBody().get("accessToken");

            System.out.println("Webhook URL: " + webhookUrl);
            System.out.println("Access Token: " + accessToken);

            if (webhookUrl == null || accessToken == null) {
                System.err.println("Failed to get webhook URL or access token.");
                return;
            }
            String finalQuery = getFinalQuery(regNo);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken); // yo have to look at this one

            Map<String, String> answer = Map.of("finalQuery", finalQuery);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(answer, headers);

            ResponseEntity<String> submitResponse = restTemplate.postForEntity(webhookUrl, entity, String.class);
            System.out.println("Submission response: " + submitResponse.getBody());

        } catch (HttpClientErrorException.Unauthorized ex) {
            System.err.println("Error 401: Unauthorized. Check your access token or webhook URL.");
        } catch (HttpClientErrorException.BadRequest ex) {
            System.err.println("Error 400: Bad Request. Check your payload format.");
            System.err.println("Response body: " + ex.getResponseBodyAsString());
        } catch (HttpClientErrorException ex) {
            System.err.println("HTTP Error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
 // okay now we deal with SQL query na les goo
    private String getFinalQuery(String regNo) {
        int lastTwoDigits = Integer.parseInt(regNo.substring(regNo.length() - 2));
        if (lastTwoDigits % 2 == 0) {
            return """
                    SELECT 
                        e.EMP_ID,
                        e.FIRST_NAME,
                        e.LAST_NAME,
                        d.DEPARTMENT_NAME,
                        COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT
                    FROM EMPLOYEE e
                    JOIN DEPARTMENT d 
                        ON e.DEPARTMENT = d.DEPARTMENT_ID
                    LEFT JOIN EMPLOYEE e2
                        ON e.DEPARTMENT = e2.DEPARTMENT
                       AND e2.DOB > e.DOB
                    GROUP BY e.EMP_ID, e.FIRST_NAME, e.LAST_NAME, d.DEPARTMENT_NAME
                    ORDER BY e.EMP_ID DESC;
                    """;
        } else {
            return "SELECT * FROM question1_solution;";
        }
    }
}
