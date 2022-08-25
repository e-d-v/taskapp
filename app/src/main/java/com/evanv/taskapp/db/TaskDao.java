package com.evanv.taskapp.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.evanv.taskapp.Task;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void deleteTask(Task task);

    @Query("SELECT * from task_table")
    List<Task> getTasks();
}

