package com.hktv.ars.service.impl;

import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.enums.AddressAnalysisType;
import com.hktv.ars.model.AddressRecord;
import com.hktv.ars.service.AddressAnalysisService;
import com.hktv.ars.service.AddressExtractionService;
import com.hktv.ars.service.AddressRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

@Log
@Service
@RequiredArgsConstructor
public class AddressAnalysisServiceImpl implements AddressAnalysisService {

    private final AhocorasickServiceImpl ahocorasickService;
    private final GoogleMapServiceImpl googleMapService;
    private final PythonServiceImpl pythonService;
    private final AddressRecordService addressRecordService;

    public RegionResponseData analyzeAddress(AddressRecord addressRecord) {
        String address = addressRecord.getAddress();

        RegionResponseData regionResponseData = tryExtract(address, AddressAnalysisType.ACHOCORASICK, ahocorasickService, addressRecord);
        if (regionResponseData != null) return regionResponseData;

        regionResponseData = tryExtract(address, AddressAnalysisType.GOOGLE_MAP, googleMapService, addressRecord);
        if (regionResponseData != null) return regionResponseData;

        regionResponseData = tryExtract(address, AddressAnalysisType.STANZA, pythonService, addressRecord);
        return regionResponseData;
    }

    private RegionResponseData tryExtract(String address, AddressAnalysisType analysisType,
                                          AddressExtractionService service, AddressRecord addressRecord) {
        RegionResponseData data = service.extractAddresses(address);
        if (data != null && data.getDeliveryZoneCode() != null) {
            populateAddressResult(addressRecord, data, analysisType);
            addressRecordService.saveAddressRecord(addressRecord);
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
    }

}
