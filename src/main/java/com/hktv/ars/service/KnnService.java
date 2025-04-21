package com.hktv.ars.service;

import com.hktv.ars.data.AddressData;

import java.util.List;
import java.util.Map;

public interface KnnService {

    void initModel(List<AddressData> addressDataList);

    String predict(double x, double y);

    Map<String, Double> predictWithProbabilities(double x, double y);
}