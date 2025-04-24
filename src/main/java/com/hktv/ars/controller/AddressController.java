package com.hktv.ars.controller;

import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.model.AddressRecord;
import com.hktv.ars.rabbitmq.client.MessageClient;
import com.hktv.ars.service.AddressAnalysisService;
import com.hktv.ars.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final AddressAnalysisService addressAnalysisService;
    private final MessageClient messageClient;
    private final RedisService redisService;

    @GetMapping("/test-redis")
    public String testRedis() {
        redisService.saveToRedis("hello", "world");
        return redisService.getFromRedis("hello");
    }

    @GetMapping("/send/{message}")
    public String sendMessage(@PathVariable String message) {
        messageClient.sendData("myExchange", "myRoutingKey", message);
        return "已發送消息: " + message;
    }

    @GetMapping("/find-hk-addresses")
    public RegionResponseData findHkAddresses(@RequestParam String address) {
        AddressRecord record = AddressRecord.builder()
                .receiveTime(LocalDateTime.now())
                .address(address)
                .build();
        return addressAnalysisService.analyzeAddress(record);
    }
}


