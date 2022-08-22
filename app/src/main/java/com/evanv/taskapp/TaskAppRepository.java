package com.evanv.taskapp;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskAppRepository {
    private TaskDao mTaskDao;                 // Dao for the task table
    private EventDao mEventDao;               // Dao for the event table
    private List<Task> mAllTasks;   // List of all tasks
    private List<Event> mAllEvents; // List of all events

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
        }
        return task.getEvents();
    }

    private static class todayTimesAsyncTask implements Runnable {
        private EventDao mEventDao;
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
        private TaskDao mTaskDao;
        private Task mTask;

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
        private EventDao mEventDao;
        private Event mEvent;

        deleteEventAsyncTask(EventDao dao, Event event) {
            mEventDao = dao;
            mEvent = event;
        }

        @Override
        public void run() {
            mEventDao.deleteEvent(mEvent);
        }
    }

    private class insertTaskAsyncTask implements Runnable {
        private TaskDao mAsyncTaskDao;
        private Task mTask;

        insertTaskAsyncTask(TaskDao dao, Task task) {
            mAsyncTaskDao = dao;
            mTask = task;
        }

        @Override
        public void run() {
            mAsyncTaskDao.insert(mTask);
        }
    }

    private static class insertEventAsyncTask implements Runnable {
        private EventDao mAsyncEventDao;
        private Event mEvent;

        insertEventAsyncTask(EventDao dao, Event event) {
            mAsyncEventDao = dao;
            mEvent = event;
        }

        @Override
        public void run() {
            mAsyncEventDao.insert(mEvent);
        }
    }

    private static class updateTaskAsyncTask implements Runnable {
        private TaskDao mAsyncTaskDao;
        private Task mTask;

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
        private EventDao mAsyncEventDao;
        private Event mEvent;

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
