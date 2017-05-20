package com.appdynamics.extensions.wmb;


import com.google.common.base.Strings;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Util {

    public static String convertToString(final Object field,final String defaultStr){
        if(field == null){
            return defaultStr;
        }
        return field.toString();
    }

    public static String join(String separator,String... strings){
        if(strings == null){
            return null;
        }
        StringBuilder builder = new StringBuilder("");
        for(int i=0;i<strings.length;i++){
            builder.append(strings[i] != null ? strings[i] : "");
            if(i != strings.length - 1 && !Strings.isNullOrEmpty(strings[i])){
                builder.append(separator);
            }
        }
        return builder.toString();
    }
    public static String[] split(final String metricType,final String splitOn) {
        return metricType.split(splitOn);
    }

    public static String toBigIntString(final BigDecimal bigD) {
        return bigD.setScale(0, RoundingMode.HALF_UP).toBigInteger().toString();
    }

    static final String WINDOWS = "windows";

    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().indexOf(WINDOWS) >= 0) {
            return true;
        }
        return false;
    }
}
