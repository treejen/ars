package com.hktv.ars.service.impl;

import com.hktv.ars.data.AddressRecordData;
import com.hktv.ars.data.base.PaginationResultData;
import com.hktv.ars.model.AddressRecord;
import com.hktv.ars.repository.AddressRecordDao;
import com.hktv.ars.service.AddressRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Log
@Service
@RequiredArgsConstructor
public class AddressRecordServiceImpl implements AddressRecordService {

    private final AddressRecordDao addressRecordDao;


    @Override
    public void saveAddressRecord(AddressRecord addressRecord) {
        addressRecordDao.save(addressRecord);
    }

    @Override
    public PaginationResultData<AddressRecordData> getRecords(Pageable pageable) {
        Page<AddressRecordData> addressRecordPage = addressRecordDao.findRecords(pageable);
        return PaginationResultData.convertToPaginationData(addressRecordPage, addressRecordPage.getContent());
    }
}
