package com.evanv.taskapp.db;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Date;

/**
 * Class to convert various types for easier storage in Room DB.
 *
 * @author Evan Voogd
 */
public class Converters {

    /**
     * Convert Long to Date for easy storing of Date fields in Room DB.
     *
     * @param dateLong The long returned from Date.getTime() stored in the Room DB.
     * @return the Date associated with the long stored in the RoomDB.
     */
    @TypeConverter
    public static Date toDate(Long dateLong) {
        return dateLong == null ? null : new Date(dateLong);
    }

    /**
     * Convert Date to Long for easy storing of Date fields in Room DB.
     *
     * @param date The day to be converted to a long
     * @return A long based on the day
     */
    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }

    /**
     * Convert String of Longs (used for IDs) to an ArrayList, useful for storing parent/child
     * relations
     *
     * @param value A comma separated list of Longs
     * @return An ArrayList of Longs
     */
    @TypeConverter
    public static ArrayList<Long> fromString(String value) {
        String[] stringLongs = value.split(",");

        ArrayList<Long> toReturn = new ArrayList<>();

        for (String s : stringLongs) {
            toReturn.add(Long.parseLong(s));
        }

        return toReturn;
    }

    /**
     * Convert ArrayList of Longs (used for IDs) to a String, useful for storing parent/child
     * relations
     *
     * @param list An arraylist of longs
     * @return A comma separated list of longs
     */
    @TypeConverter
    public static String fromArrayList(ArrayList<Long> list) {
        StringBuilder sb = new StringBuilder();

        for (long l : list) {
            sb.append(l).append(",");
        }

        return sb.toString();
    }
}
