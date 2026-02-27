package com.pollen.management.util;

import java.time.YearMonth;

public class PeriodUtils {

    private PeriodUtils() {
        // utility class
    }

    /**
     * 获取当前月份的周期标识，格式 "YYYY-MM"
     */
    public static String currentPeriod() {
        return YearMonth.now().toString();
    }

    /**
     * 校验周期格式是否合法（"YYYY-MM"，月份 01-12）
     */
    public static boolean isValidPeriod(String period) {
        return period != null && period.matches("\\d{4}-(0[1-9]|1[0-2])");
    }
}
