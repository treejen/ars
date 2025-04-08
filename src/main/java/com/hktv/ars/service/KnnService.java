package com.hktv.ars.service;

import com.hktv.ars.data.AddressData;

import java.util.List;

public interface KnnService {

    void initModel(List<AddressData> addressDataList);

    String predict(double x, double y);
}
