package com.evanv.taskapp.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.evanv.taskapp.logic.Event;

import java.util.List;

/**
 * Dao for Events, defines queries used for interfacing with the Event_Table
 *
 * @author Evan Voogd
 */
@Dao
public interface EventDao {

    /**
     * Insert an event into the event_table
     *
     * @param event The event to be added
     * @return The ID of the event to be added
     */
    @Insert
    long insert(Event event);

    /**
     * Update an event in the event_table. As long as the ID is the same, any other field can be
     * changed
     *
     * @param event The updated event
     */
    @Update
    void update(Event event);

    /**
     * Delete an event in the event_table
     *
     * @param event The event to be deleted
     */
    @Delete
    void deleteEvent(Event event);

    /**
     * Get a List of Events in the event_table
     *
     * @return A list of Events in the event_table
     */
    @Query("SELECT * from event_table ORDER BY do_date ASC")
    List<Event> getEvents();
}
