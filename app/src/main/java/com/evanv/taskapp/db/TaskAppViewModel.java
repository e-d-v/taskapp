package com.evanv.taskapp.db;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;

import com.evanv.taskapp.Event;
import com.evanv.taskapp.Task;

import java.util.List;

public class TaskAppViewModel extends AndroidViewModel {
    private final TaskAppRepository mRepository;

    private final List<Task> mAllTasks;
    private final List<Event> mAllEvents;

    public TaskAppViewModel (Application application) {
        super(application);
        mRepository = new TaskAppRepository(application);
        mAllTasks = mRepository.getAllTasks();
        mAllEvents = mRepository.getAllEvents();
    }

    public void update(Task task) { mRepository.update(task); }

    public void update(Event event) { mRepository.update(event); }

    public void delete(Task task) { mRepository.delete(task); }

    public void delete(Event event) { mRepository.delete(event); }

    public List<Task> getAllTasks() { return mAllTasks; }

    public List<Event> getAllEvents() { return mAllEvents; }

    public void insert(Task task) { mRepository.insert(task); }

    public void insert(Event event) { mRepository.insert(event); }

    public List<Event> getTodayTimes() {
        return mRepository.getTodayTimes();
    }
}
