package com.hktv.ars.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AddressDataGenerator {
    private static final Random RANDOM = new Random();
    private static final String[] REGIONS = {"香港", "九龍", "新界"};
    private static final String[] AREAS = {"中環", "尖沙咀", "銅鑼灣", "旺角", "灣仔", "上環", "西環"};
    private static final String[] STREETS = {"皇后大道中", "彌敦道", "軒尼詩道", "德輔道中", "NATHAN_ROAD", "HENNESSY_ROAD"};
    private static final String[] BUILDINGS = {"中環中心", "半島酒店", "時代廣場", "銅鑼灣中心"};

    public static List<String> generateAddressData(int count) {
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String address = generateRandomAddress().toLowerCase();
            String code = generateRegionCode(address);
            int frequency = code.startsWith("CODE_") ? 500 : 500 + RANDOM.nextInt(1001);
            lines.add(address + " " + frequency + " " + code);
        }
        return lines;
    }

    private static String generateRandomAddress() {
        int type = RANDOM.nextInt(3); // 0: 完整地址, 1: 街道, 2: 大廈
        String region = REGIONS[RANDOM.nextInt(REGIONS.length)];
        String area = AREAS[RANDOM.nextInt(AREAS.length)];
        String street = STREETS[RANDOM.nextInt(STREETS.length)];
        String building = BUILDINGS[RANDOM.nextInt(BUILDINGS.length)];
        int number = RANDOM.nextInt(999) + 1; // 1-999

        return switch (type) {
            case 0 -> region + area + street + number + "號";
            case 1 -> region + area + street;
            case 2 -> region + area + building;
            default -> building;
        };
    }

    private static String generateRegionCode(String address) {
        if (address.contains("號")) {
            for (String street : STREETS) {
                if (address.contains(street)) {
                    String regionCode = switch (street) {
                        case "皇后大道中", "德輔道中" -> "HK_CENTRAL_";
                        case "彌敦道", "NATHAN_ROAD" -> "KLN_TST_";
                        case "軒尼詩道", "HENNESSY_ROAD" -> "HK_CWB_";
                        default -> "HK_";
                    };
                    String number = address.replaceAll("[^0-9]", "");
                    return regionCode + "_" + number;
                }
            }
        }
        for (String building : BUILDINGS) {
            if (address.contains(building)) {
                return switch (building) {
                    case "中環中心" -> "HK_CENTRAL_CCC";
                    case "半島酒店" -> "KLN_TST_PH";
                    case "時代廣場" -> "HK_CWB_TS";
                    case "銅鑼灣中心" -> "HK_CWB_CCC";
                    default -> "HK_";
                };
            }
        }
        return "CODE_"+RANDOM.nextInt(9);
    }
}
