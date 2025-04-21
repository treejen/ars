package com.hktv.ars.service.impl;

import com.hktv.ars.data.AddressData;
import com.hktv.ars.data.ExcelMappingData;
import com.hktv.ars.enums.AddressType;
import com.hktv.ars.model.District;
import com.hktv.ars.model.Estate;
import com.hktv.ars.model.Street;
import com.hktv.ars.model.StreetNumber;
import com.hktv.ars.repository.DistrictDao;
import com.hktv.ars.repository.EstateDao;
import com.hktv.ars.repository.StreetDao;
import com.hktv.ars.repository.StreetNumberDao;
import com.hktv.ars.service.AhocorasickService;
import com.hktv.ars.service.InitDataService;
import com.hktv.ars.service.KnnService;
import com.hktv.ars.util.ExcelUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Log
@Service
@RequiredArgsConstructor
public class InitDataServiceImpl implements InitDataService {

    @Value("${ars.init-data:true}")
    private boolean isNeedLoadExcel;

    @Value("${ars.excel-file-path}")
    private String excelFilePath;


    private final DistrictDao districtDao;
    private final EstateDao estateDao;
    private final StreetDao streetDao;
    private final StreetNumberDao streetNumberDao;
    private final KnnService knnService;
    private final AhocorasickService ahocorasickService;

    @PostConstruct
    public void initData() {
        if (!isNeedLoadExcel) {
            return;
        }
        log.info("===== start init data =====");

        long start = System.currentTimeMillis();
        List<AddressData> allAddressDataList = new ArrayList<>();
        List<String> allCleanNameList = new ArrayList<>();

        for (AddressType addressType : AddressType.values()) {
            List<AddressData> addressDataList = loadingExcelData(addressType);
            if (addressType.isKeyWord()) {
                allCleanNameList.addAll(
                        ahocorasickService.initMapByAddressType(addressType, addressDataList));
            }
            if (addressType.isHasPoint()) {
                allAddressDataList.addAll(addressDataList);
            }
        }

        ahocorasickService.initModel(allCleanNameList);
        knnService.initModel(allAddressDataList);

        log.info("===== init data cost : " + (System.currentTimeMillis() - start)/1000 + " seconds =====");
    }

    private List<AddressData> loadingExcelData(AddressType addressType) {

        String filePath = excelFilePath;
        if (addressType == AddressType.DISTRICT) {
            districtDao.deleteAll();
            List<District> districtList = ExcelUtil.readResource(filePath, getDistrictHeaderToClassParamMap(), District.class, 3);
            districtDao.saveAll(districtList);
            log.info("save district size : " + districtList.size());
            return districtList.stream().map(District::convertToAddressData).toList();

        } else if (addressType == AddressType.ESTATE) {
            estateDao.deleteAll();
            List<Estate> estateList = Objects.requireNonNull(ExcelUtil.readResource(filePath, getEstateHeaderToClassParamMap(), ExcelMappingData.class, 5))
                    .stream()
                    .filter(estate -> isValidLatitudeLongitude(estate.getLatitude(), estate.getLongitude()))
                    .filter(estate -> StringUtils.isNotEmpty(estate.getDeliveryZoneCode()))
                    .filter(estate -> estate.getDeliveryZoneCode().contains("-"))
                    .map(Estate::covertExcelData)
                    .filter(Objects::nonNull)
                    .toList();
            estateDao.saveAll(estateList);
            log.info("save estate size : " + estateList.size());
            return estateList.stream().map(Estate::convertToAddressData).toList();

        } else if (addressType == AddressType.STREET) {
            streetDao.deleteAll();
            List<Street> streetList = Objects.requireNonNull(ExcelUtil.readResource(filePath, getStreetHeaderToClassParamMap(), ExcelMappingData.class, 9))
                    .stream()
                    .filter(street -> isValidLatitudeLongitude(street.getLatitude(), street.getLongitude()))
                    .filter(street -> StringUtils.isNotEmpty(street.getDeliveryZoneCode()))
                    .filter(street -> street.getDeliveryZoneCode().contains("-"))
                    .map(Street::covertExcelData)
                    .filter(Objects::nonNull)
                    .toList();
            streetDao.saveAll(streetList);
            log.info("save street size : " + streetList.size());
            return streetList.stream().map(Street::convertToAddressData).toList();

        } else if (addressType == AddressType.STREET_NUMBER) {
            streetNumberDao.deleteAll();
            List<StreetNumber> streetNumberList = Objects.requireNonNull(ExcelUtil.readResource(filePath, getStreetNumberHeaderToClassParamMap(), ExcelMappingData.class, 10))
                    .stream()
                    .filter(streetNumber -> isValidLatitudeLongitude(streetNumber.getLatitude(), streetNumber.getLongitude()))
                    .filter(streetNumber -> StringUtils.isNotEmpty(streetNumber.getDeliveryZoneCode()))
                    .filter(streetNumber -> streetNumber.getDeliveryZoneCode().contains("-"))
                    .map(StreetNumber::covertExcelData)
                    .filter(Objects::nonNull)
                    .toList();
            streetNumberDao.saveAll(streetNumberList);
            log.info("save street number size : " + streetNumberList.size());
            return streetNumberList.stream().map(StreetNumber::convertToAddressData).toList();
        }
        return new ArrayList<>();
    }

    private boolean isValidLatitudeLongitude(String latStr, String lonStr) {
        try {
            if (StringUtils.isEmpty(latStr) || StringUtils.isEmpty(lonStr)) {
                return false;
            }

            BigDecimal latitude = new BigDecimal(latStr.trim()).setScale(6, RoundingMode.DOWN);
            BigDecimal longitude = new BigDecimal(lonStr.trim()).setScale(6, RoundingMode.DOWN);

            return latitude.compareTo(BigDecimal.valueOf(-90)) >= 0 && latitude.compareTo(BigDecimal.valueOf(90)) <= 0
                    && longitude.compareTo(BigDecimal.valueOf(-180)) >= 0 && longitude.compareTo(BigDecimal.valueOf(180)) <= 0;
        } catch (NumberFormatException e) {
            return false; // 解析錯誤代表非法數據
        }
    }

    private static Map<String, String> getDeliveryHeaderToClassParamMap() {
        Map<String, String> excelHeaderToClassParamMap = new HashMap<>();
        excelHeaderToClassParamMap.put("DELIVERY ZONE CODE", "zoneCode");
        excelHeaderToClassParamMap.put("DELIVERY ZONE DESC", "zoneDesc");
        return excelHeaderToClassParamMap;
    }

    private static Map<String, String> getDistrictHeaderToClassParamMap() {
        Map<String, String> excelHeaderToClassParamMap = new HashMap<>();
        excelHeaderToClassParamMap.put("DISTRICT CODE", "districtCode");
        excelHeaderToClassParamMap.put("DELIVERY ZONE CODE", "deliveryZoneCode");
        excelHeaderToClassParamMap.put("DISTRICT NAME EN", "districtNameEn");
        excelHeaderToClassParamMap.put("DISTRICT NAME ZH", "districtNameZh");
        return excelHeaderToClassParamMap;
    }

    private static Map<String, String> getEstateHeaderToClassParamMap() {
        Map<String, String> excelHeaderToClassParamMap = new HashMap<>();
        excelHeaderToClassParamMap.put("ESTATE CODE", "estateCode");
        excelHeaderToClassParamMap.put("DISTRICT CODE", "districtCode");
        excelHeaderToClassParamMap.put("DELIVERY ZONE CODE", "deliveryZoneCode");
        excelHeaderToClassParamMap.put("IS ACTIVE", "isActive");
        excelHeaderToClassParamMap.put("WILL DELIVERY", "willDelivery");
        excelHeaderToClassParamMap.put("LATITUDE", "latitude");
        excelHeaderToClassParamMap.put("LONGITUDE", "longitude");
        excelHeaderToClassParamMap.put("ESTATE NAME EN", "estateNameEn");
        excelHeaderToClassParamMap.put("ESTATE NAME ZH", "estateNameZh");
        return excelHeaderToClassParamMap;
    }

    private static Map<String, String> getStreetHeaderToClassParamMap() {
        Map<String, String> excelHeaderToClassParamMap = new HashMap<>();
        excelHeaderToClassParamMap.put("STREET CODE", "streetCode");
        excelHeaderToClassParamMap.put("DISTRICT CODE", "districtCode");
        excelHeaderToClassParamMap.put("DELIVERY ZONE CODE", "deliveryZoneCode");
        excelHeaderToClassParamMap.put("IS ACTIVE", "isActive");
        excelHeaderToClassParamMap.put("WILL DELIVERY", "willDelivery");
        excelHeaderToClassParamMap.put("LATITUDE", "latitude");
        excelHeaderToClassParamMap.put("LONGITUDE", "longitude");
        excelHeaderToClassParamMap.put("STREET NAME EN", "streetNameEn");
        excelHeaderToClassParamMap.put("STREET NAME ZH", "streetNameZh");
        return excelHeaderToClassParamMap;
    }

    private static Map<String, String> getStreetNumberHeaderToClassParamMap() {
        Map<String, String> excelHeaderToClassParamMap = new HashMap<>();
        excelHeaderToClassParamMap.put("STREET CODE", "streetCode");
        excelHeaderToClassParamMap.put("DELIVERY ZONE CODE", "deliveryZoneCode");
        excelHeaderToClassParamMap.put("WILL DELIVERY", "willDelivery");
        excelHeaderToClassParamMap.put("LATITUDE", "latitude");
        excelHeaderToClassParamMap.put("LONGITUDE", "longitude");
        excelHeaderToClassParamMap.put("STREET NUMBER NAME EN", "streetNumber");
        return excelHeaderToClassParamMap;
    }

}
