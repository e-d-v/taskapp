package com.evanv.taskapp.logic;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.evanv.taskapp.db.Converters;

import org.threeten.bp.LocalDateTime;

import org.threeten.bp.format.DateTimeFormatter;
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
public class Event implements Comparable {
    // Fields
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long mID;              // PrimaryKey for Event. Used as duplicate event names is allowed.
    @NonNull
    @ColumnInfo(name = "name")
    private String mName;          // The name of the event to display in the schedule
    @ColumnInfo(name = "length")
    private int mLength;           // How long the event lasts
    @NonNull
    @ColumnInfo(name = "do_date")
    private LocalDateTime mDoDate; // The start time for the event. Named to be consistent with Task

    // Static Fields
    // SimpleDateFormat that formats date in the style "08/20/22 08:12 PM"
    public static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("M/d/yy hh:mm a");
    // SimpleDateFormat that formats time in the style "08:12 PM"
    public static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm a");

    /**
     * Initializes an object representing an event
     *
     * @param name The name of the event
     * @param length The time to complete for the event
     * @param doDate The start time for the event
     */
    public Event(@NonNull String name, int length, @NonNull LocalDateTime doDate) {
        this.mName = name;
        this.mDoDate = doDate;
        this.mLength = length;
    }

    /**
     * Initializes an object representing an event
     *
     * @param id The ID of the event
     * @param event The name of the event
     * @param start The start time for the event
     * @param length The time to complete for the event
     */
    public Event(int id, @NonNull String event, LocalDateTime start, int length) {
        this.mID = id;
        this.mName = event;
        this.mDoDate = start;
        this.mLength = length;
    }

    /**
     * Returns the ID of the event, used as the primary key in the database
     *
     * @return the ID of the event
     */
    public long getID() {
        return mID;
    }

    /**
     * Changes the ID of the event, useful to ensure same as primary key in DB
     *
     * @param id the new ID of the event
     */
    public void setID(long id) {
        this.mID = id;
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
     * Changes the name of the event
     *
     * @param name The new name of the event
     */
    public void setName(@NonNull String name) {
        this.mName = name;
    }

    /**
     * Returns the starting time of the event
     *
     * @return The starting time of the event
     */
    public LocalDateTime getDoDate() {
        return mDoDate;
    }

    /**
     * Changes the start time of the event
     *
     * @param doDate The new start time of the event
     */
    public void setDoDate(@NonNull LocalDateTime doDate) {
        this.mDoDate = doDate;
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
     * Changes the length (in minutes) of the event
     *
     * @param mLength The new length of the event
     */
    public void setLength(int mLength) {
        this.mLength = mLength;
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

    /**
     * Returns a positive value if this event is after the other event, negative if it's before and
     * 0 if they occur at the same time.
     *
     * @param o The other event
     * @return a positive value if this event is after the other event, negative if it's before and
     *           0 if they occur at the same time
     */
    @Override
    public int compareTo(Object o) {
        Event other = (Event) o;

        return mDoDate.compareTo(other.getDoDate());
    }
}