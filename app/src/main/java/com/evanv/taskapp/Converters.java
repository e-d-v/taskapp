package com.evanv.taskapp;

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
    public static ArrayList<Integer> fromString(String value) {
        String[] stringInts = value.split(",");

        ArrayList<Integer> toReturn = new ArrayList<>();

        for (String s : stringInts) {
            toReturn.add(Integer.parseInt(s));
        }

        return toReturn;
    }

    @TypeConverter
    public static String fromArrayList(ArrayList<Integer> list) {
        StringBuilder sb = new StringBuilder();

        for (int i : list) {
            sb.append(i).append(",");
        }

        return sb.toString();
    }
}
