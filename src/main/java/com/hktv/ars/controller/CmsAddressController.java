package com.hktv.ars.controller;

import com.hktv.ars.data.AddressRecordData;
import com.hktv.ars.data.base.PaginationResultData;
import com.hktv.ars.service.AddressRecordService;
import com.hktv.ars.util.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CmsAddressController {

    private final AddressRecordService addressAnalysisService;

    @GetMapping("/records")
    public PaginationResultData<AddressRecordData> getRecords(
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "1") Integer pageNo) {
        Pageable pageable = PageUtil.createPageable(pageSize, pageNo);
        return addressAnalysisService.getRecords(pageable);
    }

    @PostMapping("/records")
    public Boolean updateRecord() {

        return Boolean.TRUE;
    }

    @GetMapping("/send")
    public Boolean sendToMMS() {

        return Boolean.TRUE;
    }
}
