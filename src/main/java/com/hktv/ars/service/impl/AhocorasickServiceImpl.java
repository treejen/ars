package com.hktv.ars.service.impl;

import com.hktv.ars.data.AddressData;
import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.enums.AddressType;
import com.hktv.ars.model.Estate;
import com.hktv.ars.model.Street;
import com.hktv.ars.model.StreetNumber;
import com.hktv.ars.repository.EstateDao;
import com.hktv.ars.repository.StreetDao;
import com.hktv.ars.repository.StreetNumberDao;
import com.hktv.ars.service.AhocorasickService;
import com.hktv.ars.util.CleanWordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Service;

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

@Log
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
    private final EstateDao estateDao;
    private final StreetDao streetDao;
    private final StreetNumberDao streetNumberDao;

    public List<String> initMapByAddressType(AddressType addressType, List<AddressData> addressDataList) {
        List<String> keywords = new ArrayList<>();
        addressDataList.forEach(
                data -> {
                    String cleanedEn = CleanWordUtil.cleanAddress(data.getEnName());
                    String cleanedZh = CleanWordUtil.cleanAddress(data.getZhName());
                    keywords.add(cleanedEn);
                    keywords.add(cleanedZh);
                    if (addressType == AddressType.DISTRICT) {
                        DISTRICT_AND_CODE.put(cleanedEn, data.getCode());
                        DISTRICT_AND_CODE.put(cleanedZh, data.getCode());
                    } else if (addressType == AddressType.ESTATE) {
                        ESTATE_AND_DBNAME.put(cleanedEn, data.getEnName());
                        ESTATE_AND_DBNAME.put(cleanedZh, data.getZhName());
                    } else if (addressType == AddressType.STREET) {
                        STREET_AND_DBNAME.put(cleanedEn, data.getEnName());
                        STREET_AND_DBNAME.put(cleanedZh, data.getZhName());
                    }
                }
        );

        return keywords;
    }

    public void initModel(List<String> keywords) {
        TRIE = Trie.builder()
                .addKeywords(keywords)
                .addKeywords(List.of(CHINESE_NO))
                .build();
        log.info("Ahocorasick資料存入[ " + keywords.size()+1 + " ]筆");
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
