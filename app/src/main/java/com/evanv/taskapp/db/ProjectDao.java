package com.evanv.taskapp.db;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.evanv.taskapp.logic.Project;

import java.util.List;

/**
 * Dao for Projects, defines queries used for interfacing with the Project_Table
 *
 * @author Evan Voogd
 */
public interface ProjectDao {

    /**
     * Insert a project into the project_table
     *
     * @param project The project to be added
     * @return The ID of the project to be added
     */
    @Insert
    long insert(Project project);

    /**
     * Update a Project in the project_table. As long as the ID is the same, any other field can be
     * changed
     *
     * @param project The updated event
     */
    @Update
    void update(Project project);

    /**
     * Delete a project in the project_table
     *
     * @param project The Project to be deleted
     */
    @Delete
    void deleteProject(Project project);

    /**
     * Get a List of Projects in the project_table
     *
     * @return A list of Projects in the project_table`
     */
    @Query("SELECT * from project_table")
    List<Project> getProjects();
}
