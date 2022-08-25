package com.evanv.taskapp;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.evanv.taskapp.db.Converters;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Represents a single event, e.g. something that has a set date/time
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
@Entity(tableName = "event_table")
@TypeConverters(Converters.class)
public final class Event {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long mID;             // PrimaryKey for Event. Used as duplicate event names is allowed.
    @NonNull
    @ColumnInfo(name = "name")
    private final String mName;   // The name of the event to display in the schedule
    @ColumnInfo(name = "length")
    private final int mLength;    // How long the event lasts
    // The start time for the event. Called "doDate" with consistency to Task
    @NonNull
    @ColumnInfo(name = "do_date")
    private final Date mDoDate;
    // SimpleDateFormat that formats date in the style "08/20/22 08:12 PM"
    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MM/dd/yy hh:mm aa");
    // SimpleDateFormat that formats time in the style "08:12 PM"
    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aa");

    public Event(@NonNull String name, int length, @NonNull Date doDate) {
        mName = name;
        mLength = length;
        mDoDate = doDate;
    }

    public long getID() {
        return mID;
    }

    public void setID(long mID) {
        this.mID = mID;
    }

    /**
     * Returns the name of the event
     *
     * @return The name of the event
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the starting time of the event
     *
     * @return The starting time of the event
     */
    public Date getDoDate() {
        return mDoDate;
    }

    /**
     * Returns the length of the event
     *
     * @return The length of the event
     */
    public int getLength() {
        return mLength;
    }

    /**
     * Initializes an object representing an event
     *
     * @param event The name of the event
     * @param start The start time for the event
     * @param length The time to complete for the event
     */
    public Event(@NonNull String event, Date start, int length) {
        this.mName = event;
        this.mDoDate = clearTime(start);
        this.mLength = length;
    }

    /**
     * Initializes an object representing an event
     *
     * @param event The name of the event
     * @param start The start time for the event
     * @param length The time to complete for the event
     */
    public Event(int id, @NonNull String event, Date start, int length) {
        this.mID = id;
        this.mName = event;
        this.mDoDate = clearTime(start);
        this.mLength = length;
    }


    /**
     * Clears second/ms information from Date. Very useful for Events as Events have no information
     * past seconds.
     *
     * @param toClear The date to return without a seconds
     * @return toClear but at the time HOURS:MINUTES:00.0000
     */
    public static Date clearTime(Date toClear) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(toClear);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}