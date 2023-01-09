package com.evanv.taskapp.db;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;

import com.evanv.taskapp.logic.Event;
import com.evanv.taskapp.logic.Project;
import com.evanv.taskapp.logic.Task;

import java.util.List;

/**
 * ViewModel to interface with the repository. Only gets initial state as internal data structures
 * keep synchronized themselves, so there's no need to sync besides at the start.
 *
 * @author Evan Voogd
 */
public class TaskAppViewModel extends AndroidViewModel {
    // Repository that interfaces with the database
    private final TaskAppRepository mRepository;

    private final List<Task> mAllTasks;   // List of all tasks in DB at start of app
    private final List<Event> mAllEvents; // List of all events in DB at start of app
    private final List<Project> mAllProjects; // List of all projects in DB at start of app

    /**
     * Constructs a ViewModel to interface with a Repository
     *
     * @param application the current application
     */
    public TaskAppViewModel (Application application) {
        super(application);
        mRepository = new TaskAppRepository(application);
        mAllTasks = mRepository.getAllTasks();
        mAllEvents = mRepository.getAllEvents();
        mAllProjects = mRepository.getAllProjects();
    }

    /**
     * Gets a list of all tasks upon start of app. LiveData not used due to race conditions inherent
     * to app.
     *
     * @return a list of all tasks
     */
    public List<Task> getAllTasks() { return mAllTasks; }

    /**
     * Gets a list of all events upon start of app. LiveData not used due to race conditions
     * inherent to app.
     *
     * @return a list of all events
     */
    public List<Event> getAllEvents() { return mAllEvents; }

    /**
     * Gets a list of all projects upon start of app. LiveData not used due to race conditions
     * inherent to app.
     *
     * @return a list of all projects
     */
    public List<Project> getAllProjects() {
        return mAllProjects;
    }

    /**
     * Asynchronously inserts a task into the task_table
     *
     * @param task Task to be inserted
     */
    public void insert(Task task) { mRepository.insert(task); }

    /**
     * Asynchronously inserts an event into the event_table
     *
     * @param event Event to be inserted
     */
    public void insert(Event event) { mRepository.insert(event); }

    /**
     * Asynchronously inserts a project into the project_table
     *
     * @param project Project to be inserted
     */
    public void insert(Project project) {
        mRepository.insert(project);
    }

    /**
     * Asynchronously updates a task in the task_table
     *
     * @param task Task to be updated
     */
    public void update(Task task) { mRepository.update(task); }

    /**
     * Asynchronously updates an event in the event_table
     *
     * @param event Event to be updated
     */
    public void update(Event event) { mRepository.update(event); }


    /**
     * Asynchronously updates a project in the project_table
     *
     * @param project Project to be updated.
     */
    public void update(Project project) {
        mRepository.update(project);
    }

    /**
     * Asynchronously deletes a task in the task_table
     *
     * @param task Task to be deleted
     */
    public void delete(Task task) { mRepository.delete(task); }

    /**
     * Asynchronously deletes an event in the event_table
     *
     * @param event Event to be deleted
     */
    public void delete(Event event) { mRepository.delete(event); }

    /**
     * Asynchronously deletes a project in the project_table
     *
     * @param project Project to be deleted.
     */
    public void delete(Project project) {
        mRepository.delete(project);
    }
}
