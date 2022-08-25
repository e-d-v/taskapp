package com.evanv.taskapp.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.evanv.taskapp.Event;
import com.evanv.taskapp.Task;

@Database(entities = {Task.class, Event.class}, version = 1, exportSchema = false)
public abstract class TaskAppRoomDatabase extends RoomDatabase {
    private static volatile TaskAppRoomDatabase INSTANCE;

    public static TaskAppRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (TaskAppRoomDatabase.class) {
                if (INSTANCE == null) {
                    // Create database here
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            TaskAppRoomDatabase.class, "taskapp_database")
                            .fallbackToDestructiveMigration().build();
                }
            }
        }

        return INSTANCE;
    }

    public abstract TaskDao taskDao();

    public abstract EventDao eventDao();
}
