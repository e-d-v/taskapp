package com.evanv.taskapp.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.evanv.taskapp.logic.Task;

import java.util.List;

/**
 * Dao for interfacing with the task_table in the Room DB.
 *
 * @author Evan Voogd
 */
@Dao
public interface TaskDao {

    /**
     * Insert a task into the task_table
     *
     * @param task The task to insert
     * @return The ID of the new task, which serves as it's primary key
     */
    @Insert
    long insert(Task task);

    /**
     * Update a task in the task_table. Any field can be changed as long as it's ID matches one in
     * the table, as it serves as the Primary Key.
     *
     * @param task The task to be updated.
     */
    @Update
    void update(Task task);

    /**
     * Delete a task in the task_table.
     *
     * @param task The task to be deleted.
     */
    @Delete
    void deleteTask(Task task);

    /**
     * Get all tasks in the task_table.
     *
     * @return A List of Tasks in the task_table
     */
    @Query("SELECT * from task_table")
    List<Task> getTasks();
}

