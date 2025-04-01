package com.hktv.ars.service;

public interface KnnService {

    void initModel();

    String predict(double x, double y);
}
