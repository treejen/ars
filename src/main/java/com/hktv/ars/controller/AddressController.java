package com.hktv.ars.controller;

import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.rabbitmq.client.MessageClient;
import com.hktv.ars.service.AhocorasickService;
import com.hktv.ars.service.GoogleMapService;
import com.hktv.ars.service.PythonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final AhocorasickService ahocorasickService;
    private final GoogleMapService googleMapService;
    private final PythonService pythonService;
    private final MessageClient messageClient;

    @GetMapping("/send/{message}")
    public String sendMessage(@PathVariable String message) {
        messageClient.sendData("myExchange", "myRoutingKey", message);
        return "已發送消息: " + message;
    }

    @GetMapping("/find-hk-addresses-from-file")
    public List<RegionResponseData> findHkAddressesFromFile(@RequestParam String type) {
        try {
            long time = System.currentTimeMillis();
            List<RegionResponseData> results = new ArrayList<>();
            String filePath = "src/main/resources/test_addresses.txt";
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                results.add(getResultByType(type, line));
            }
            long success = results.stream().filter(response -> StringUtils.isNotBlank(response.getDeliveryZoneCode())).count();
            log.info("=====中文使用 [{}] 毫秒, [{} / {}] 筆成功, 成功率 : {}=====", (System.currentTimeMillis() - time), success, lines.size(),
                    BigDecimal.valueOf(success).divide(BigDecimal.valueOf(lines.size()), 2, RoundingMode.HALF_UP));

            long enTime = System.currentTimeMillis();
            List<RegionResponseData> resultsEn = new ArrayList<>();
            String filePathEn = "src/main/resources/test_addresses_en.txt";
            List<String> enLines = Files.readAllLines(Paths.get(filePathEn));
            for (String line : enLines) {
                if (line.trim().isEmpty()) continue;
                resultsEn.add(getResultByType(type, line));
            }
            long successEn = resultsEn.stream().filter(response -> StringUtils.isNotBlank(response.getDeliveryZoneCode())).count();
            log.info("=====英文使用 [{}] 毫秒, [{} / {}] 筆成功, 成功率 : {}=====", (System.currentTimeMillis() - enTime), successEn, enLines.size(),
                    BigDecimal.valueOf(successEn).divide(BigDecimal.valueOf(enLines.size()), 2, RoundingMode.HALF_UP));
            results.addAll(resultsEn);
            return results;

        } catch (IOException e) {
            throw new RuntimeException("無法讀取檔案: " + e);
        }
    }

    private RegionResponseData getResultByType(String type, String address) {
        switch (type) {
            case "gm":
                return googleMapService.extractAddresses(address);
            case "py":
                return pythonService.extractAddresses(address);
            default:
                return ahocorasickService.extractAddresses(address);
        }
    }

    @GetMapping("/compare")
    public Map<String, Map<String, String>> compare() {
        try {
            String filePath = "src/main/resources/test_addresses.txt";
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            Map<String, Map<String, String>> results = new HashMap<>();
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                RegionResponseData ac = ahocorasickService.extractAddresses(line);
                RegionResponseData gm = googleMapService.extractAddresses(line);
                RegionResponseData py = pythonService.extractAddresses(line);
                Map<String, String> map = new HashMap<>();
                map.put("ac", ac.getDeliveryZoneCode());
                map.put("gm", gm.getDeliveryZoneCode());
                map.put("py", py.getDeliveryZoneCode());
                Boolean isSame = Objects.equals(ac.getDeliveryZoneCode(), gm.getDeliveryZoneCode())
                        && Objects.equals(gm.getDeliveryZoneCode(), py.getDeliveryZoneCode());
                map.put("isTheSame", isSame.toString());
                results.put(ac.getAddress(), map);
            }

            String filePathEn = "src/main/resources/test_addresses_en.txt";
            List<String> enLines = Files.readAllLines(Paths.get(filePathEn));
            for (String line : enLines) {
                if (line.trim().isEmpty()) continue;
                RegionResponseData ac = ahocorasickService.extractAddresses(line);
                RegionResponseData gm = googleMapService.extractAddresses(line);
                RegionResponseData py = pythonService.extractAddresses(line);
                Map<String, String> map = new HashMap<>();
                map.put("ac", ac.getDeliveryZoneCode());
                map.put("gm", gm.getDeliveryZoneCode());
                map.put("py", py.getDeliveryZoneCode());
                Boolean isSame = Objects.equals(ac.getDeliveryZoneCode(), gm.getDeliveryZoneCode())
                        && Objects.equals(gm.getDeliveryZoneCode(), py.getDeliveryZoneCode());
                map.put("isTheSame", isSame.toString());
                results.put(ac.getAddress(), map);
            }
            return results;
        } catch (IOException e) {
            throw new RuntimeException("無法讀取檔案: " + e);
        }
    }
}


