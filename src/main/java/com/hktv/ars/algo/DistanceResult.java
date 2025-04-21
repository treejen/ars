package com.hktv.ars.algo;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 存儲距離計算結果的不可變類
 * 用於計算和排序距離結果
 */
@Getter
public class DistanceResult implements Comparable<DistanceResult>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private final double distance; // 計算得出的距離
    private final String label;    // 數據點的標籤

    /**
     * 建構一個距離計算結果
     *
     * @param distance 計算得出的距離
     * @param label 數據點的標籤
     */
    public DistanceResult(double distance, String label) {
        this.distance = distance;
        this.label = label;
    }

    @Override
    public int compareTo(DistanceResult other) {
        return Double.compare(this.distance, other.distance);
    }

    @Override
    public String toString() {
        return "DistanceResult{distance=" + distance + ", label='" + label + "'}"; 
    }
}