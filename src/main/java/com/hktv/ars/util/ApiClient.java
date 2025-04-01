package com.hktv.ars.util;

import lombok.experimental.UtilityClass;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@UtilityClass
public class ApiClient {

    private static final WebClient webClient = WebClient.create();

    public String getUrl(String url) {

        // 發送 GET 請求
        Mono<String> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);

        return response.block();
    }

    public static void postUrl(String url, String jsonBody) {

        // 發送 POST 請求
        Mono<String> response = webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(String.class);

        response.subscribe(System.out::println);
    }

}
