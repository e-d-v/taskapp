package com.evanv.taskapp.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.evanv.taskapp.logic.Label;
import com.evanv.taskapp.logic.Project;

import java.util.List;

/**
 * Dao for Labels, defines queries used for interfacing with the label_table
 *
 * @author Evan Voogd
 */
@Dao
public interface LabelDao {
    /**
     * Insert a label into the label_table
     *
     * @param label The label to be added
     *
     * @return The ID of the label to be added
     */
    @Insert
    long insert(Label label);

    /**
     * Update a Label in the label_table. As long as the ID is the same, any other field can be
     * changed
     *
     * @param label The updated event
     */
    @Update
    void update(Label label);

    /**
     * Delete a label in the label_table
     *
     * @param label The Label to be deleted
     */
    @Delete
    void delete(Label label);

    /**
     * Get a List of Labels in the label_table
     *
     * @return A list of Labels in the label table
     */
    @Query("SELECT * from label_table")
    List<Label> getLabels();
}
