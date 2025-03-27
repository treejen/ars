package com.hktv.ars.service.impl;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.hktv.ars.data.RegionEntry;
import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.service.JeibaService;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import static java.util.Map.entry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JeibaServiceImpl implements JeibaService {

    private static final Map<String, String> CHINESE_TO_ARABIC = Map.ofEntries(
            entry("一", "1"),
            entry("二", "2"),
            entry("三", "3"),
            entry("四", "4"),
            entry("五", "5"),
            entry("六", "6"),
            entry("七", "7"),
            entry("八", "8"),
            entry("九", "9"),
            entry("零", "0"),
            entry("十", "10"),
            entry("百", "100"),
            entry("千", "1000")
    );

    private static final List<RegionEntry> REGION_CODE_LIST = new ArrayList<>();

    private static final List<String> ADDRESS_INDICATORS = List.of(
            "號", "巷", "樓", "F", "室"
    );

    private static final GeoApiContext geoApiContext = new GeoApiContext.Builder()
            .apiKey("YOUR_API_KEY") // 替換為你的 Google Maps API Key
            .build();

//    static {
//        try {
//            Runtime runtime = Runtime.getRuntime();
//            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
//            AtomicInteger count = new AtomicInteger();
//            AtomicInteger ignoreWord = new AtomicInteger();
//            long time = System.currentTimeMillis();
//            String dictPath = "src/main/resources/address_street.txt";
//            String dictPath2 = "src/main/resources/address_estate.txt";
//            WordDictionary.getInstance().loadUserDict(Paths.get(dictPath));
//            WordDictionary.getInstance().loadUserDict(Paths.get(dictPath2));
//
//            Files.lines(Paths.get(dictPath))
//                    .map(line -> line.trim().split("\\s+"))
//                    .filter(parts -> parts.length == 3)
//                    .forEach(parts -> {
//                        String word = parts[0].replace("_", " ").toLowerCase();
//                        int frequency = Integer.parseInt(parts[1]);
//                        String code = parts[2];
//                        REGION_CODE_LIST.add(new RegionEntry(word, code, frequency));
//                        count.getAndIncrement();
//                    });
//
//            Files.lines(Paths.get(dictPath2))
//                    .map(line -> line.trim().split("\\s+"))
//                    .filter(parts -> parts.length == 3)
//                    .forEach(parts -> {
//                        String word = parts[0].replace("_", " ").toLowerCase();
//                        if(word.contains("(") || word.contains("（")){
//                            ignoreWord.getAndIncrement();
//                        }
//                        int frequency = Integer.parseInt(parts[1]);
//                        String code = parts[2];
//                        REGION_CODE_LIST.add(new RegionEntry(word, code, frequency));
//                        count.getAndIncrement();
//                    });
//
//            long afterMemory = runtime.totalMemory() - runtime.freeMemory();
//
//            System.out.println("=====Jeiba資料存入[ " + count + " ]筆, ignore[ " + ignoreWord + " ]筆, 耗時[ " + (System.currentTimeMillis() - time) + " ]毫秒=====");
//            System.out.println("=====Jeiba佔用了[ " + (afterMemory - beforeMemory) / 1024 / 1024 + " ]MB=====");
//
//        } catch (IOException e) {
//            throw new RuntimeException("無法載入地址詞典檔案", e);
//        }
//    }

    @Override
    public RegionResponseData extractAddresses(String address) {
        String street = null;
        String dist = null;
        String estate = null;
        StringBuilder number = new StringBuilder();

        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<SegToken> tokens = segmenter.process(address, JiebaSegmenter.SegMode.SEARCH);
        boolean inAddress = false;
        for (SegToken token : tokens) {
            String word = token.word.replace("_", " ");
            Optional<RegionEntry> optional = REGION_CODE_LIST.stream()
                    .filter(entry -> entry.getName().equals(word)).findFirst();
            if (optional.isPresent()) {
                RegionEntry regionEntry = optional.get();
                if (regionEntry.getFrequency() == 500) {
                    dist = word;
                } else if (regionEntry.getFrequency() == 1000) {
                    street = word;
                    inAddress = true;
                } else if (regionEntry.getFrequency() == 1500) {
                    estate = word;
                }
            } else if (inAddress || ADDRESS_INDICATORS.stream().anyMatch(word::contains)) {
                String normalizeNumber = normalizeNumber(word);
                number.append(normalizeNumber);
            }
        }

//        return new RegionResponseData(address, street, dist, number.toString(), estate);
        return null;
    }

//    @Override
//    public RegionResponseData extractAddresses(String text) {
//        JiebaSegmenter segmenter = new JiebaSegmenter();
////        String normalizedWord = normalizeNumber(text);
//        List<SegToken> tokens = segmenter.process(text, JiebaSegmenter.SegMode.SEARCH);
//        StringBuilder currentAddress = new StringBuilder();
//        boolean inAddress = false;
//
//        for (SegToken token : tokens) {
//            String word = token.word.replace("_", " "); // 還原空格
//
//            if (isAddressRelated(word)) {
//                if (!inAddress) {
//                    inAddress = true;
//                    currentAddress = new StringBuilder();
//                }
//                currentAddress.append(word);
//            } else if (inAddress) {
//                String address = currentAddress.toString();
//                String regionCode = findRegionCode(address);
//                if (regionCode != null) {
//                    return new RegionResponseData(text, address, regionCode);
//                }
//                inAddress = false;
//                currentAddress = new StringBuilder();
//            }
//        }
//
//        if (inAddress) {
//            String address = currentAddress.toString();
//            String regionCode = findRegionCode(address);
//            if (regionCode != null) {
//                return new RegionResponseData(text, address, regionCode);
//            }
//        }
//
//        return new RegionResponseData(text, null, null);
//    }

    private boolean isAddressRelated(String word) {
        return ADDRESS_INDICATORS.stream().anyMatch(word::contains) ||
                REGION_CODE_LIST.stream().anyMatch(entry -> entry.getName().equals(word));
    }

    private String normalizeNumber(String word) {
        StringBuilder result = new StringBuilder();
        char[] chars = word.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            String charStr = String.valueOf(chars[i]);

            if (List.of("十", "百", "千").contains(charStr)) {
                String arabicNumber = CHINESE_TO_ARABIC.getOrDefault(charStr, charStr);
                if (i != 0) {
                    arabicNumber = arabicNumber.substring(1);
                }
                if (i != chars.length - 1) {
                    String nextWord = String.valueOf(chars[i + 1]);
                    if (CHINESE_TO_ARABIC.containsKey(nextWord)) {
                        continue;
                    }
                }
                result.append(arabicNumber);
            } else {
                result.append(CHINESE_TO_ARABIC.getOrDefault(charStr, charStr));
            }
        }
        return result.toString();
    }

    private String findRegionCode(String address) {
        RegionEntry bestMatch = null;
        int highestFreq = -1;

        for (RegionEntry entry : REGION_CODE_LIST) {
            if (address.contains(entry.getName()) && entry.getFrequency() > highestFreq) {
                bestMatch = entry;
                highestFreq = entry.getFrequency();
            }
        }

        return bestMatch != null ? bestMatch.getCode() : null;
    }


    private Map<String, Object> getCoordinatesFromGoogle(String address) {
        try {
            String normalizedAddress = address.replace("_", " ");
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, normalizedAddress).await();
            if (results.length > 0) {
                double latitude = results[0].geometry.location.lat;
                double longitude = results[0].geometry.location.lng;
                return Map.of(
                        "address", normalizedAddress,
                        "latitude", latitude,
                        "longitude", longitude
                );
            } else {
                return Map.of("error", "找不到該地址的座標");
            }
        } catch (Exception e) {
            return Map.of("error", "獲取座標失敗: " + e.getMessage());
        }
    }

}
