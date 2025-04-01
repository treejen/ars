package com.hktv.ars.util;

import com.github.houbb.opencc4j.util.ZhTwConverterUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CleanWordUtil {
    /**
     * 清理地址：自動判斷是中文或英文，並移除括號內容及特殊符號
     */
    public static String cleanAddress(String text) {
        String address = removeBrackets(text);
        return removeSpecialChars(address, containsChinese(address));
    }

    /**
     * 檢查字串是否包含中文
     */
    public static boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            if (Character.toString(c).matches("[\u4e00-\u9fa5]")) {
                return true;  // 只要找到一個中文字符，就認定為中文地址
            }
        }
        return false;
    }

    public static boolean hasCommaAfterNumber(String address, String number) {
        int index = address.indexOf(number);
        if (index == -1 || index + number.length() >= address.length()) {
            return false;
        }
        return address.charAt(index + number.length()) == ',';
    }

    /**
     * 移除括號內容及特殊符號
     * - 若為中文地址：移除括號內容、特殊符號，**同時去掉空格**
     * - 若為英文地址：移除括號內容、特殊符號，**但保留空格**
     */
    public static String removeSpecialChars(String address, boolean isChinese) {
        if (isChinese) {
            // 移除特殊符號 -> 移除所有空格
            return ZhTwConverterUtil.toTraditional(address)
                    .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5-]", "");  // 移除特殊符號
        } else {
            // 移除特殊符號（但允許空格）-> 轉小寫
            return address
                    .replaceAll("[^a-zA-Z0-9 .,-]", "")
                    .toLowerCase();
        }
    }

    public static String removeBrackets(String value) {
        if (value == null || !value.contains("(")) {
            return value;
        }
        return value.replaceAll("\\s*\\(.*?\\)", "");
    }

    public static String truncateString(String value, int maxLength) {
        return (value != null && value.length() > maxLength) ? value.substring(0, maxLength) : value;
    }
}
