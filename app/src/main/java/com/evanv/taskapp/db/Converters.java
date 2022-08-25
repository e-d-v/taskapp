package com.evanv.taskapp.db;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Date;

/**
 * Class to convert Date to long for Room DB.
 *
 * @author Evan Voogd
 */
public class Converters {

    @TypeConverter
    public static Date toDate(Long dateLong){
        return dateLong == null ? null: new Date(dateLong);
    }

    @TypeConverter
    public static Long fromDate(Date date){
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static ArrayList<Long> fromString(String value) {
        String[] stringLongs = value.split(",");

        ArrayList<Long> toReturn = new ArrayList<>();

        for (String s : stringLongs) {
            toReturn.add(Long.parseLong(s));
        }

        return toReturn;
    }

    @TypeConverter
    public static String fromArrayList(ArrayList<Long> list) {
        StringBuilder sb = new StringBuilder();

        for (long l: list) {
            sb.append(l).append(",");
        }

        return sb.toString();
    }
}
