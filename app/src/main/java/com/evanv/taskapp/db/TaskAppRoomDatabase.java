package com.evanv.taskapp.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.evanv.taskapp.logic.Event;
import com.evanv.taskapp.logic.Label;
import com.evanv.taskapp.logic.Project;
import com.evanv.taskapp.logic.Task;

/**
 * The Room DB for TaskApp. Uses singleton design pattern to ensure only one DB exists so data
 * inconsistencies don't emerge.
 *
 * @author Evan Voogd
 */
@Database(entities = {Task.class, Event.class, Project.class, Label.class}, version = 5,
        exportSchema = false)
public abstract class TaskAppRoomDatabase extends RoomDatabase {
    private static volatile TaskAppRoomDatabase INSTANCE; // The singleton of the Room DB

    /**
     * Get the database for the app. Generates the singleton if it hasn't been already.
     *
     * @param context Context for the app
     * @return Returns the singleton of the Room DB
     */
    public static TaskAppRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (TaskAppRoomDatabase.class) {
                if (INSTANCE == null) {
                    // Create database here if not already created
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            TaskAppRoomDatabase.class, "taskapp_database")
                            .fallbackToDestructiveMigration().build();
                }
            }
        }

        return INSTANCE;
    }

    /**
     * Gets a Dao to interface with the task_table
     *
     * @return a Dao to interface with the task_table
     */
    public abstract TaskDao taskDao();

    /**
     * Gets a Dao to interface with the event_table
     *
     * @return a Dao to interface with the event_table
     */
    public abstract EventDao eventDao();

    /**
     * Gets a Dao to interface with the project_table
     *
     * @return a Dao to interface with the project_table
     */
    public abstract ProjectDao projectDao();

    /**
     * Gets a Dao to interface with the label_table
     *
     * @return a Dao to interface with the label_table
     */
    public abstract LabelDao labelDao();
}
