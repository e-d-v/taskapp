package com.evanv.taskapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EventDao {
    @Insert
    void insert(Event event);

    @Update
    void update(Event event);

    @Delete
    void deleteEvent(Event event);

    @Query("SELECT * from event_table ORDER BY do_date ASC")
    List<Event> getEvents();

    @Query("SELECT * from event_table WHERE name='' OR name is NULL")
    List<Event> getTodayTimes();
}
