package com.hktv.ars.service;

import com.hktv.ars.data.AddressRecordData;
import com.hktv.ars.data.base.PaginationResultData;
import com.hktv.ars.model.AddressRecord;

import org.springframework.data.domain.Pageable;


public interface AddressRecordService {

    void saveAddressRecord(AddressRecord addressRecord);

    PaginationResultData<AddressRecordData> getRecords(Pageable pageable);
}
