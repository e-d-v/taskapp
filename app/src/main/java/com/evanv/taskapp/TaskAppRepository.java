package com.evanv.taskapp;

import android.app.Application;
import android.util.Log;

import java.util.List;

public class TaskAppRepository {
    private final TaskDao mTaskDao;                 // Dao for the task table
    private final EventDao mEventDao;               // Dao for the event table
    private final List<Task> mAllTasks;   // List of all tasks
    private final List<Event> mAllEvents; // List of all events

    TaskAppRepository(Application application) {
        TaskAppRoomDatabase db = TaskAppRoomDatabase.getDatabase(application);
        mTaskDao = db.taskDao();
        mEventDao = db.eventDao();
        mAllTasks = mTaskDao.getTasks();
        mAllEvents = mEventDao.getEvents();
    }

    List<Task> getAllTasks() {
        return mAllTasks;
    }

    List<Event> getAllEvents() {
        return mAllEvents;
    }

    public void insert(Task task) {
        (new Thread(new insertTaskAsyncTask(mTaskDao, task))).start();
    }

    public void insert(Event event) {
        (new Thread(new insertEventAsyncTask(mEventDao, event))).start();
    }

    public void update(Task task) {
        (new Thread(new updateTaskAsyncTask(mTaskDao, task))).start();
    }

    public void update(Event event) {
        (new Thread(new updateEventAsyncTask(mEventDao, event))).start();
    }

    public void delete(Task task) {
        (new Thread(new deleteTaskAsyncTask(mTaskDao, task))).start();
    }

    public void delete(Event event) {
        (new Thread(new deleteEventAsyncTask(mEventDao, event))).start();
    }

    public List<Event> getTodayTimes() {
        todayTimesAsyncTask task = new todayTimesAsyncTask(mEventDao);
        Thread t = (new Thread(task));
        t.start();
        try {
            t.join();
        }
        catch (Exception e) {
            Log.e(TaskAppRepository.class.getSimpleName(), e.toString());
        }
        return task.getEvents();
    }

    private static class todayTimesAsyncTask implements Runnable {
        private final EventDao mEventDao;
        private List<Event> mEvents;

        todayTimesAsyncTask(EventDao dao) {
            mEventDao = dao;
        }

        @Override
        public void run() { mEvents = mEventDao.getTodayTimes(); }

        public List<Event> getEvents() {
            return mEvents;
        }

    }

    private static class deleteTaskAsyncTask implements Runnable {
        private final TaskDao mTaskDao;
        private final Task mTask;

        deleteTaskAsyncTask(TaskDao dao, Task task) {
            mTaskDao = dao;
            mTask = task;
        }

        @Override
        public void run() {
            mTaskDao.deleteTask(mTask);
        }
    }

    private static class deleteEventAsyncTask implements Runnable {
        private final EventDao mEventDao;
        private final Event mEvent;

        deleteEventAsyncTask(EventDao dao, Event event) {
            mEventDao = dao;
            mEvent = event;
        }

        @Override
        public void run() {
            mEventDao.deleteEvent(mEvent);
        }
    }

    private static class insertTaskAsyncTask implements Runnable {
        private final TaskDao mAsyncTaskDao;
        private final Task mTask;

        insertTaskAsyncTask(TaskDao dao, Task task) {
            mAsyncTaskDao = dao;
            mTask = task;
        }

        @Override
        public void run() {
            long id = mAsyncTaskDao.insert(mTask);
            mTask.setID(id);
        }
    }

    private static class insertEventAsyncTask implements Runnable {
        private final EventDao mAsyncEventDao;
        private final Event mEvent;

        insertEventAsyncTask(EventDao dao, Event event) {
            mAsyncEventDao = dao;
            mEvent = event;
        }

        @Override
        public void run() {
            long id = mAsyncEventDao.insert(mEvent);
            mEvent.setID(id);
        }
    }

    private static class updateTaskAsyncTask implements Runnable {
        private final TaskDao mAsyncTaskDao;
        private final Task mTask;

        updateTaskAsyncTask(TaskDao dao, Task task) {
            mAsyncTaskDao = dao;
            mTask = task;
        }

        @Override
        public void run() {
            mAsyncTaskDao.update(mTask);
        }
    }

    private static class updateEventAsyncTask implements Runnable {
        private final EventDao mAsyncEventDao;
        private final Event mEvent;

        updateEventAsyncTask(EventDao dao, Event event) {
            mAsyncEventDao = dao;
            mEvent = event;
        }

        @Override
        public void run() {
            mAsyncEventDao.update(mEvent);
        }
    }
}
