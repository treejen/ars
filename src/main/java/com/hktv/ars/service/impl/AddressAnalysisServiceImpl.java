package com.hktv.ars.service.impl;

import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.enums.AddressAnalysisType;
import com.hktv.ars.enums.SystemVariableEnum;
import com.hktv.ars.model.AddressRecord;
import com.hktv.ars.service.AddressAnalysisService;
import com.hktv.ars.service.AddressExtractionService;
import com.hktv.ars.service.AddressRecordService;
import com.hktv.ars.service.SystemVariableService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressAnalysisServiceImpl implements AddressAnalysisService {

    private final AhocorasickServiceImpl ahocorasickService;
    private final GoogleMapServiceImpl googleMapService;
    private final PythonServiceImpl pythonService;
    private final AddressRecordService addressRecordService;
    private final SystemVariableService systemVariableService;

    public RegionResponseData analyzeAddress(AddressRecord addressRecord) {
        String address = addressRecord.getAddress();

        RegionResponseData regionResponseData = tryExtract(address, AddressAnalysisType.ACHOCORASICK, ahocorasickService, addressRecord);
        if (regionResponseData != null) return regionResponseData;

        int limit = systemVariableService.getInt(SystemVariableEnum.GOOGLE_API_LIMIT.name(), 8000);
        
        regionResponseData = tryExtract(address, AddressAnalysisType.GOOGLE_MAP, googleMapService, addressRecord);
        if (regionResponseData != null) return regionResponseData;
//
//        regionResponseData = tryExtract(address, AddressAnalysisType.STANZA, pythonService, addressRecord);
        return RegionResponseData.builder().address(address).deliveryZoneCode("DUMMY").build();
    }

    private RegionResponseData tryExtract(String address, AddressAnalysisType analysisType,
                                          AddressExtractionService service, AddressRecord addressRecord) {
        RegionResponseData data = service.extractAddresses(address);
        if (data != null && StringUtils.isNotBlank(data.getDeliveryZoneCode())) {
            long time = Timestamp.valueOf(addressRecord.getReceiveTime()).getTime();
//            populateAddressResult(addressRecord, data, analysisType);
//            addressRecordService.saveAddressRecord(addressRecord);
            log.info("use {} millisecond and type {} to find delivery zone code", System.currentTimeMillis() - time, analysisType);
            return data;
        }
        return null;
    }

    private void populateAddressResult(AddressRecord result, RegionResponseData data, AddressAnalysisType type) {
        result.setAnalysisType(type);
        result.setOriginalDeliveryZoneCode(data.getDeliveryZoneCode());
        result.setEstate(data.getEstate());
        result.setStreet(data.getStreet());
        result.setStreetNumber(data.getNumber());
        result.setDistrict(data.getDist());
        result.setLatitude(data.getLatitude());
        result.setLongitude(data.getLongitude());
        result.setWillDeliver(data.getWillDelivery());
    }

}
