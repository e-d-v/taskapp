package com.evanv.taskapp.db;

import android.app.Application;

import com.evanv.taskapp.logic.Event;
import com.evanv.taskapp.logic.Project;
import com.evanv.taskapp.logic.Task;

import java.util.List;

/**
 * Repository to interface with Room DB.
 *
 * @author Evan Voogd
 */
public class TaskAppRepository {
    private final TaskDao mTaskDao;       // Dao for the task table
    private final EventDao mEventDao;     // Dao for the event table
    private final ProjectDao mProjectDao; // Dao for the project table
    private final List<Task> mAllTasks;   // List of all tasks
    private final List<Event> mAllEvents; // List of all events
    private final List<Project> mAllProjects; // List of all projects

    /**
     * Constructs a new repository
     *
     * @param application The current application
     */
    TaskAppRepository(Application application) {
        TaskAppRoomDatabase db = TaskAppRoomDatabase.getDatabase(application);
        mTaskDao = db.taskDao();
        mEventDao = db.eventDao();
        mProjectDao = db.projectDao();
        mAllTasks = mTaskDao.getTasks();
        mAllEvents = mEventDao.getEvents();
        mAllProjects = mProjectDao.getProjects();
    }

    /**
     * Gets a list of all tasks upon start of app. LiveData not used due to race conditions inherent
     * to app.
     *
     * @return a list of all tasks
     */
    List<Task> getAllTasks() {
        return mAllTasks;
    }

    /**
     * Gets a list of all events upon start of app. LiveData not used due to race conditions
     * inherent to app.
     *
     * @return a list of all events
     */
    List<Event> getAllEvents() {
        return mAllEvents;
    }

    /**
     * Gets a list of all projects upon start of app. LiveData not used due to race conditions
     * inherent to app.
     *
     * @return a list of all projects
     */
    List<Project> getAllProjects() {
        return mAllProjects;
    }

    /**
     * Asynchronously inserts a task into the task_table
     *
     * @param task Task to be inserted
     */
    public void insert(Task task) {
        (new Thread(new insertTaskAsyncTask(mTaskDao, task))).start();
    }

    /**
     * Asynchronously inserts an event into the event_table
     *
     * @param event Event to be inserted
     */
    public void insert(Event event) {
        (new Thread(new insertEventAsyncTask(mEventDao, event))).start();
    }

    /**
     * Asynchronously inserts a project into the project_table
     *
     * @param project Project to be inserted
     */
    public void insert(Project project) {
        (new Thread(new insertProjectAsyncTask(mProjectDao, project))).start();
    }

    /**
     * Asynchronously updates a task in the task_table
     *
     * @param task Task to be updated
     */
    public void update(Task task) {
        (new Thread(new updateTaskAsyncTask(mTaskDao, task))).start();
    }

    /**
     * Asynchronously updates an event in the event_table
     *
     * @param event Event to be updated
     */
    public void update(Event event) {
        (new Thread(new updateEventAsyncTask(mEventDao, event))).start();
    }

    /**
     * Asynchronously updates a project in the project_table
     *
     * @param project Project to be updated
     */
    public void update(Project project) {
        (new Thread(new updateProjectAsyncTask(mProjectDao, project))).start();
    }

    /**
     * Asynchronously deletes a task in the task_table
     *
     * @param task Task to be deleted
     */
    public void delete(Task task) {
        (new Thread(new deleteTaskAsyncTask(mTaskDao, task))).start();
    }

    /**
     * Asynchronously deletes an event in the event_table
     *
     * @param event Event to be deleted
     */
    public void delete(Event event) {
        (new Thread(new deleteEventAsyncTask(mEventDao, event))).start();
    }

    /**
     * Asynchronously deletes a project in the project_table
     *
     * @param project Project to be deleted
     */
    public void delete(Project project) {
        (new Thread(new deleteProjectAsyncTask(mProjectDao, project))).start();
    }

    /**
     * Runnable that inserts a task into the db
     */
    private static class insertTaskAsyncTask implements Runnable {
        private final TaskDao mAsyncTaskDao; // The Dao used to interface with the database
        private final Task mTask;            // The Task to be inserted into the database

        /**
         * Initializes the thread with the task to be added and the Dao to interface with
         *
         * @param dao The Dao to use to interface with the database
         * @param task The task to add to the database
         */
        insertTaskAsyncTask(TaskDao dao, Task task) {
            mAsyncTaskDao = dao;
            mTask = task;
        }

        /**
         * The code to be ran asynchronously - insert a task and update the id.
         */
        @Override
        public void run() {
            long id = mAsyncTaskDao.insert(mTask);

            // Due to the complex relationships between Tasks (and in the future Projects/Labels) we
            // must keep the internal data structure separate from the Room DB. To ensure tasks can
            // still be deleted/updated despite this, we must manually update the id after it is
            // inserted into the database.
            mTask.setID(id);
        }
    }

    /**
     * Runnable that inserts an Event into the DB
     */
    private static class insertEventAsyncTask implements Runnable {
        private final EventDao mAsyncEventDao; // The Dao used to interface with the database
        private final Event mEvent;            // The Event to be inserted into the database

        /**
         * Initializes the thread with the event to be added and the Dao to interface with
         *
         * @param dao The Dao used to interface with the database
         * @param event The Event to be inserted into the database
         */
        insertEventAsyncTask(EventDao dao, Event event) {
            mAsyncEventDao = dao;
            mEvent = event;
        }

        /**
         * The code to be ran asynchronously - insert an Event and update the id.
         */
        @Override
        public void run() {
            long id = mAsyncEventDao.insert(mEvent);
            mEvent.setID(id);
        }
    }

    /**
     * Runnable that inserts an Event into the DB
     */
    private static class insertProjectAsyncTask implements Runnable {
        private final ProjectDao mAsyncProjectDao; // The Dao used to interface with the database
        private final Project mProject;            // The Project to be inserted into the database

        /**
         * Initializes the thread with the event to be added and the Dao to interface with.
         *
         * @param dao The Dao used to interface with the database.
         * @param project The Project to be inserted into the database.
         */
        public insertProjectAsyncTask(ProjectDao dao, Project project) {
            mAsyncProjectDao = dao;
            mProject = project;
        }

        /**
         * The code to be run asynchronously - insert a Project and update the ID.
         */
        public void run() {
            long id = mAsyncProjectDao.insert(mProject);
            mProject.setID(id);
        }
    }

    /**
     * Runnable that updates a Task currently in the DB
     */
    private static class updateTaskAsyncTask implements Runnable {
        private final TaskDao mAsyncTaskDao; // The Dao used to interface with the database
        private final Task mTask;            // The Task to be updated in the database

        /**
         * Initializes the thread with the task to be updated and the Dao to interface with
         *
         * @param dao The Dao to use to interface with the database
         * @param task The task to updated in the database
         */
        updateTaskAsyncTask(TaskDao dao, Task task) {
            mAsyncTaskDao = dao;
            mTask = task;
        }

        /**
         * The code to be ran asynchronously - update a task
         */
        @Override
        public void run() {
            mAsyncTaskDao.update(mTask);
        }
    }

    /**
     * Runnable that updates an Event currently in the DB
     */
    private static class updateEventAsyncTask implements Runnable {
        private final EventDao mAsyncEventDao; // The Dao used to interface with the database
        private final Event mEvent;            // The Event to be updated in the database

        /**
         * Initializes the thread with the event to be updated and the Dao to interface with
         *
         * @param dao The Dao used to interface with the database
         * @param event The Event to be updated in the database
         */
        updateEventAsyncTask(EventDao dao, Event event) {
            mAsyncEventDao = dao;
            mEvent = event;
        }

        /**
         * The code to be ran asynchronously - update an event
         */
        @Override
        public void run() {
            mAsyncEventDao.update(mEvent);
        }
    }

    /**
     * Runnable that updates a Project currently in the DB
     */
    private static class updateProjectAsyncTask implements Runnable {
        private final ProjectDao mAsyncProjectDao; // The Dao used to interface with the database
        private final Project mProject;            // The Project to be updated in the database

        /**
         * Initializes the thread with the event to be updated and the Dao to interface with.
         *
         * @param dao The Dao used to interface with the database.
         * @param project The Project to be updated in the database.
         */
        public updateProjectAsyncTask(ProjectDao dao, Project project) {
            mAsyncProjectDao = dao;
            mProject = project;
        }

        /**
         * The code to be run asynchronously - insert a Project and update the ID.
         */
        public void run() {
            mAsyncProjectDao.update(mProject);
        }
    }

    /**
     * Runnable that deletes a task from the task_table
     */
    private static class deleteTaskAsyncTask implements Runnable {
        private final TaskDao mAsyncTaskDao; // The Dao used to interface with the database
        private final Task mTask;            // The Task to be deleted from the database

        /**
         * Initializes the thread with the task to be deleted and the Dao to interface with
         *
         * @param dao The Dao to use to interface with the database
         * @param task The task to deleted in the database
         */
        deleteTaskAsyncTask(TaskDao dao, Task task) {
            mAsyncTaskDao = dao;
            mTask = task;
        }

        /**
         * The code to be ran asynchronously - delete a task
         */
        @Override
        public void run() {
            mAsyncTaskDao.deleteTask(mTask);
        }
    }

    /**
     * Runnable that deletes an Event currently in the DB
     */
    private static class deleteEventAsyncTask implements Runnable {
        private final EventDao mAsyncEventDao; // The Dao used to interface with the database
        private final Event mEvent;            // The Event to be deleted from into the database

        /**
         * Initializes the thread with the event to be deleted and the Dao to interface with
         *
         * @param dao The Dao used to interface with the database
         * @param event The Event to be deleted in the database
         */
        deleteEventAsyncTask(EventDao dao, Event event) {
            mAsyncEventDao = dao;
            mEvent = event;
        }

        /**
         * The code to be ran asynchronously - delete an event
         */
        @Override
        public void run() {
            mAsyncEventDao.deleteEvent(mEvent);
        }
    }

    /**
     * Runnable that deletes an
     */
    private static class deleteProjectAsyncTask implements Runnable {
        private final ProjectDao mAsyncProjectDao; // The Dao used to interface with the database
        private final Project mProject;            // The Project to be deleted from the database

        /**
         * Initializes the thread with the event to be deleted and the Dao to interface with.
         *
         * @param dao The Dao used to interface with the database.
         * @param project The Project to be deleted from the database.
         */
        public deleteProjectAsyncTask(ProjectDao dao, Project project) {
            mAsyncProjectDao = dao;
            mProject = project;
        }

        /**
         * The code to be run asynchronously - insert a Project and update the ID.
         */
        public void run() {
            mAsyncProjectDao.deleteProject(mProject);
        }
    }
}
