package com.hktv.ars.service.impl;

import com.hktv.ars.data.ExcelMappingData;
import com.hktv.ars.model.DeliveryZone;
import com.hktv.ars.model.District;
import com.hktv.ars.model.Estate;
import com.hktv.ars.model.Street;
import com.hktv.ars.model.StreetNumber;
import com.hktv.ars.repository.DeliveryZoneDao;
import com.hktv.ars.repository.DistrictDao;
import com.hktv.ars.repository.EstateDao;
import com.hktv.ars.repository.StreetDao;
import com.hktv.ars.repository.StreetNumberDao;
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

    private final DeliveryZoneDao deliveryZoneDao;
    private final DistrictDao districtDao;
    private final EstateDao estateDao;
    private final StreetDao streetDao;
    private final StreetNumberDao streetNumberDao;
    private final KnnService knnService;

    @PostConstruct
    public void initData() {
        if (isNeedLoadExcel) {
            loadingExcelData();
        }
        knnService.initModel();
    }

    private void loadingExcelData() {
        String filePath = "C:\\Users\\tracy.chang\\Downloads\\Address-20250220103453.xlsx";
        try {
            //delivery zone code
            deliveryZoneDao.deleteAll();
            List<DeliveryZone> deliveryZoneList = ExcelUtil.readResource(filePath, getDeliveryHeaderToClassParamMap(), DeliveryZone.class, 2);
            deliveryZoneDao.saveAll(deliveryZoneList);
            log.info("save delivery zone size : " + deliveryZoneList.size());
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
        }
        try {
            //district
            districtDao.deleteAll();
            List<District> districtList = ExcelUtil.readResource(filePath, getDistrictHeaderToClassParamMap(), District.class, 3);
            districtDao.saveAll(districtList);
            log.info("save district size : " + districtList.size());
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
        }
        try {
            //estate
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
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
        }
        try {
            //street
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
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
        }
        try {
            //street number
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
            log.info("save streetNumber size : " + streetNumberList.size());
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
        }
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
