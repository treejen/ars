package com.hktv.ars.algo;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * 距離函數枚舉
 * 定義了可用於KNN算法的距離計算函數
 * 使用枚舉而非lambda表達式，解決序列化問題
 */
public enum DistanceFunction implements Serializable {
    
    /**
     * 歐氏距離 (L2距離)
     */
    EUCLIDEAN {
        @Override
        public double calculate(double[] a, double[] b) {
            return WeightedKNNUtils.calculateDistance(a, b);
        }
        
        @Override
        public String toString() {
            return "euclidean";
        }
    },
    
    /**
     * 曼哈頓距離 (L1距離)
     */
    MANHATTAN {
        @Override
        public double calculate(double[] a, double[] b) {
            return WeightedKNNUtils.calculateManhattanDistance(a, b);
        }
        
        @Override
        public String toString() {
            return "manhattan";
        }
    },
    
    /**
     * 切比雪夫距離 (L∞距離)
     */
    CHEBYSHEV {
        @Override
        public double calculate(double[] a, double[] b) {
            return WeightedKNNUtils.calculateChebyshevDistance(a, b);
        }
        
        @Override
        public String toString() {
            return "chebyshev";
        }
    },
    
    /**
     * 餘弦距離
     */
    COSINE {
        @Override
        public double calculate(double[] a, double[] b) {
            return WeightedKNNUtils.calculateCosineDistance(a, b);
        }
        
        @Override
        public String toString() {
            return "cosine";
        }
    };
    
    /**
     * 計算兩點之間的距離
     *
     * @param a 第一個點的特徵向量
     * @param b 第二個點的特徵向量
     * @return 兩點之間的距離
     */
    public abstract double calculate(double[] a, double[] b);
    
    /**
     * 轉換為BiFunction接口
     * 提供向後兼容性
     *
     * @return 距離計算函數
     */
    public BiFunction<double[], double[], Double> toBiFunction() {
        return this::calculate;
    }
    
    /**
     * 根據名稱獲取距離函數
     *
     * @param name 距離函數名稱
     * @return 對應的距離函數枚舉值，默認為EUCLIDEAN
     */
    public static DistanceFunction fromString(String name) {
        if (name == null) {
            return EUCLIDEAN;
        }
        
        switch (name.toLowerCase()) {
            case "manhattan":
                return MANHATTAN;
            case "chebyshev":
                return CHEBYSHEV;
            case "cosine":
                return COSINE;
            case "euclidean":
            default:
                return EUCLIDEAN;
        }
    }
}