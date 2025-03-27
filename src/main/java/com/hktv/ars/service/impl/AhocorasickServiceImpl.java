package com.hktv.ars.service.impl;

import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.model.Estate;
import com.hktv.ars.model.Street;
import com.hktv.ars.model.StreetNumber;
import com.hktv.ars.repository.DistrictDao;
import com.hktv.ars.repository.EstateDao;
import com.hktv.ars.repository.StreetDao;
import com.hktv.ars.repository.StreetNumberDao;
import com.hktv.ars.service.AhocorasickService;
import com.hktv.ars.util.CleanWordUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AhocorasickServiceImpl implements AhocorasickService {

    private static final Map<Character, Integer> CHINESE_NUMBER_MAP = Map.of(
            '零', 0, '一', 1, '二', 2, '三', 3, '四', 4,
            '五', 5, '六', 6, '七', 7, '八', 8, '九', 9
    );

    private static final String CHINESE_NO = "號";

    private static final Map<String, String> STREET_AND_DBNAME = new HashMap<>();
    private static final Map<String, String> DISTRICT_AND_CODE = new HashMap<>();
    private static final Map<String, String> ESTATE_AND_DBNAME = new HashMap<>();
    private static final Pattern CHINESE_NUMBER_PATTERN = Pattern.compile("([零一二三四五六七八九十百]+)(?=號)");
    private static final Pattern ENGLISH_NUMBER_PATTERN = Pattern.compile("(?<=No\\.)\\s*\\d+(?:-\\d+)?|\\b\\d+-\\d+\\b|\\b\\d+\\b(?=\\s+[A-Za-z])");
    //    private static final Pattern ENGLISH_NUMBER_PATTERN = Pattern.compile("\\b\\d+[a-zA-Z-]*\\b");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("(\\d+)(?=號)");

    private static final Set<String> EXCLUDE_WORDS = Set.of("apt", "room", "unit", "floor", "building", "f");

    private static Trie TRIE;
    private static final Logger log = LoggerFactory.getLogger(AhocorasickServiceImpl.class);

    private final DistrictDao districtDao;
    private final EstateDao estateDao;
    private final StreetDao streetDao;
    private final StreetNumberDao streetNumberDao;

    @PostConstruct
    private void buildTrieFromDB() {
        long time = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        List<String> keywords = new ArrayList<>();
        districtDao.findAll()
                .forEach(district -> {
                            String cleanedDistrictEn = CleanWordUtil.cleanAddress(district.getDistrictNameEn()).replace("hk", "");
                            String cleanedDistrictZh = CleanWordUtil.cleanAddress(district.getDistrictNameZh());
                            DISTRICT_AND_CODE.put(cleanedDistrictEn, district.getDistrictCode());
                            DISTRICT_AND_CODE.put(cleanedDistrictZh, district.getDistrictCode());
                            keywords.add(cleanedDistrictEn);
                            keywords.add(cleanedDistrictZh);
                        }
                );

        streetDao.findAll().forEach(street -> {
                    String cleanedStreetEn = CleanWordUtil.cleanAddress(street.getStreetNameEn());
                    String cleanedStreetZh = CleanWordUtil.cleanAddress(street.getStreetNameZh());
                    STREET_AND_DBNAME.put(cleanedStreetEn, street.getStreetNameEn());
                    STREET_AND_DBNAME.put(cleanedStreetZh, street.getStreetNameZh());
                    keywords.add(cleanedStreetEn);
                    keywords.add(cleanedStreetZh);
                }
        );

        estateDao.findAll().forEach(estate -> {
                    String cleanedEstateEn = CleanWordUtil.cleanAddress(estate.getEstateNameEn());
                    String cleanedEstateZh = CleanWordUtil.cleanAddress(estate.getEstateNameZh());
                    ESTATE_AND_DBNAME.put(cleanedEstateEn, estate.getEstateNameEn());
                    ESTATE_AND_DBNAME.put(cleanedEstateZh, estate.getEstateNameZh());
                    keywords.add(cleanedEstateEn);
                    keywords.add(cleanedEstateZh);
                }
        );

        TRIE = Trie.builder()
                .addKeywords(keywords)
                .addKeywords(List.of(CHINESE_NO))
                .build();

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("=====Ahocorasick資料存入[ " + keywords.size() + " ]筆, 耗時[ " + (System.currentTimeMillis() - time) + " ]毫秒=====");
        System.out.println("=====Ahocorasick佔用了[ " + (afterMemory - beforeMemory) / 1024 / 1024 + " ]MB=====");
    }

    public RegionResponseData extractAddresses(String text) {

        RegionResponseData regionResponseData = new RegionResponseData();
        boolean isChinese = CleanWordUtil.containsChinese(text);
        String address = CleanWordUtil.cleanAddress(text);
        regionResponseData.setAddress(address);

        List<String> streetList = new ArrayList<>();
        List<String> distList = new ArrayList<>();
        List<String> estateList = new ArrayList<>();
        Collection<Emit> matches = TRIE.parseText(address.toLowerCase());
        for (Emit match : matches) {
            String keyword = match.getKeyword();
            if (!isChinese) {
                int begin = match.getStart();
                int end = match.getEnd();
                boolean isWordStart = (begin == 0) || address.charAt(begin - 1) == ' ' || address.charAt(begin - 1) == ',';
                boolean isWordEnd = (end + 1 == address.length()) || address.charAt(end + 1) == ' ' || address.charAt(end + 1) == ',';
                if (!isWordStart || !isWordEnd) { // 確保是整個單字
                    continue;
                }
            }

            if (DISTRICT_AND_CODE.containsKey(keyword)) {
                distList.add(keyword);
            } else if (STREET_AND_DBNAME.containsKey(keyword)) {
                streetList.add(keyword);
            } else if (ESTATE_AND_DBNAME.containsKey(keyword)) {
                estateList.add(keyword);
            } else if (CHINESE_NO.equals(keyword)) {
                int start = Math.max(0, match.getStart() - 5); // 避免索引越界
                int end = match.getEnd() + 1;
                String subText = address.substring(start, end);

                Matcher matcher = ADDRESS_PATTERN.matcher(subText);
                Matcher matcher2 = CHINESE_NUMBER_PATTERN.matcher(subText);

                if (matcher.find()) {
                    regionResponseData.setNumber(matcher.group());
                } else if (matcher2.find()) {
                    String chineseNum = matcher2.group();
                    int arabicNum = chineseToArabic(chineseNum);
                    regionResponseData.setNumber(String.valueOf(arabicNum));
                } else {
                    regionResponseData.setNumber(keyword);
                }
            }
        }

        if (!isChinese) {
            Matcher matcher = ENGLISH_NUMBER_PATTERN.matcher(address);
            if (matcher.find()) {
                String doorNumber = matcher.group().trim();
                if (doorNumber.contains("-")) {
                    String[] doorNumbers = doorNumber.split("-");
                    regionResponseData.setNumber(doorNumbers[doorNumbers.length - 1]);
                } else {
                    regionResponseData.setNumber(doorNumber);
                }
            }
        }

        if (!distList.isEmpty()) {
            distList.sort(Comparator.comparingInt(String::length).reversed());
            regionResponseData.setDist(distList.getFirst());
        }

        if (!streetList.isEmpty()) {
            streetList.sort(Comparator.comparingInt(String::length).reversed());
            regionResponseData.setStreet(streetList.getFirst());
        }

        if (!estateList.isEmpty()) {
            estateList.sort(Comparator.comparingInt(String::length).reversed());
            regionResponseData.setEstate(estateList.getFirst());
        }

        return getDeliveryZoneCode(regionResponseData);
    }

    public RegionResponseData getDeliveryZoneCode(RegionResponseData regionResponseData) {
        String street = regionResponseData.getStreet();
        String number = regionResponseData.getNumber();
        String estate = regionResponseData.getEstate();
        String address = regionResponseData.getAddress();
        String district = regionResponseData.getDist();

        if (street != null || number != null) {
            String dbName = STREET_AND_DBNAME.get(street);
            Optional<StreetNumber> streetNumberOptional = streetNumberDao.findByStreetNameAndNumber(dbName, number);
            if (streetNumberOptional.isPresent()) {
                regionResponseData.setLatitude(streetNumberOptional.get().getLatitude());
                regionResponseData.setLongitude(streetNumberOptional.get().getLongitude());
                regionResponseData.setDeliveryZoneCode(streetNumberOptional.get().getDeliveryZoneCode());
            } else {
                Optional<Street> streetOptional = streetDao.findByStreetName(street);
                if (streetOptional.isPresent()) {
                    regionResponseData.setLatitude(streetOptional.get().getLatitude());
                    regionResponseData.setLongitude(streetOptional.get().getLongitude());
                    regionResponseData.setDeliveryZoneCode(streetOptional.get().getDeliveryZoneCode());
                }
            }
        }

        if (regionResponseData.getDeliveryZoneCode() == null && estate != null) {
            String dbName = ESTATE_AND_DBNAME.get(estate);
            Optional<Estate> estateOptional = estateDao.findByEstateName(dbName);
            if (estateOptional.isPresent() && (estateOptional.get().getDistrictCode().equals(DISTRICT_AND_CODE.get(district))
                        || address.equals(estate))) {
                    regionResponseData.setLatitude(estateOptional.get().getLatitude());
                    regionResponseData.setLongitude(estateOptional.get().getLongitude());
                    regionResponseData.setDeliveryZoneCode(estateOptional.get().getDeliveryZoneCode());
                }

        }
        return regionResponseData;
    }

    private static int chineseToArabic(String chineseNum) {
        if (chineseNum.equals("十")) return 10; // 特殊情況
        int result = 0, temp = 0;
        boolean hasTen = false;

        for (char c : chineseNum.toCharArray()) {
            if (c == '十') {
                temp = (temp == 0) ? 10 : temp * 10;
                hasTen = true;
            } else {
                temp += CHINESE_NUMBER_MAP.getOrDefault(c, 0);
            }
        }

        return hasTen ? temp : result + temp;
    }

}
