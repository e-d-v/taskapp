package com.evanv.taskapp.db;

import androidx.room.TypeConverter;

import org.threeten.bp.LocalDate;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;

import java.util.ArrayList;

/**
 * Class to convert various types for easier storage in Room DB.
 *
 * @author Evan Voogd
 */
public class Converters {

    /**
     * Convert Long to LocalDate for easy storing of LocalDate fields in Room DB.
     *
     * @param dateLong The long returned from LocalDate.toEpochDay() stored in the Room DB.
     * @return the LocalDate associated with the long stored in the RoomDB.
     */
    @TypeConverter
    public static LocalDate toLocalDate(Long dateLong) {
        return LocalDate.ofEpochDay(dateLong);
    }

    /**
     * Convert LocalDate to Long for easy storing of LocalDate fields in Room DB.
     *
     * @param date The day to be converted to a long
     * @return A long based on the day
     */
    @TypeConverter
    public static Long fromLocalDate(LocalDate date) {
        return date.toEpochDay();
    }

    /**
     * Convert Long to LocalDateTime for easy storing of LocalDateTime fields in Room DB.
     *
     * @param dateLong The long returned from ofEpochSecond stored in the Room DB.
     * @return the LocalDate associated with the long stored in the RoomDB.
     */
    @TypeConverter
    public static LocalDateTime toLocalDateTime(Long dateLong) {
        return LocalDateTime.ofEpochSecond(dateLong, 0, ZoneOffset.UTC);
    }

    /**
     * Convert LocalDateTime to Long for easy storing of LocalDateTime fields in Room DB.
     *
     * @param date The day to be converted to a long
     * @return A long based on the day
     */
    @TypeConverter
    public static Long fromLocalDateTime(LocalDateTime date) {
        return date.toEpochSecond(ZoneOffset.UTC);
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

        if (stringLongs.length == 0) {
            return toReturn;
        }

        if (stringLongs[0].equals("")) {
            return toReturn;
        }

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
