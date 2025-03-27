package com.hktv.ars.util;

public class GeoUtil {
    // 地球平均半徑（公尺）
    private static final double EARTH_RADIUS = 6371000;

    /**
     * 計算兩個經緯度之間的距離（單位：公尺）
     * @param lat1 第一個地址的緯度
     * @param lng1 第一個地址的經度
     * @param lat2 第二個地址的緯度
     * @param lng2 第二個地址的經度
     * @return 距離（公尺）
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // 將角度轉換為弧度
        double lat1Rad = Math.toRadians(lat1);
        double lng1Rad = Math.toRadians(lng1);
        double lat2Rad = Math.toRadians(lat2);
        double lng2Rad = Math.toRadians(lng2);

        // 緯度和經度的差值
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLng = lng2Rad - lng1Rad;

        // Haversine 公式
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c; // 返回距離（公尺）
    }
}
