package com.evanv.taskapp.ui.main;

/**
 * Defines a ClickListener, which reacts to one of the completion buttons being clicked. It's
 * structure is a bit weird but explainable: MainActivity is the ClickListener for the
 * DayItemAdapter's DayViewHolders, which are in turn the ClickListener for their TaskItemAdapters'
 * DayViewHolders. When the TaskViewHolder (which is the View.OnClickListener for the tasks'
 * buttons) registers a click, it sends the task's index using onButtonClick to the DayViewHolder,
 * which in turn sends the task's index and it's own index (representing how many days past the
 * start date the task is scheduled for) to the MainActivity, which can use these two values to
 * call Complete on the task, as taskSchedule.get(day).get(position) will return the task marked
 * as complete.
 *
 * @author Evan Voogd
 */
public interface ClickListener {
    /**
     * A function that sends a Button Click up to it's own ClickListener, or acts on the Button
     * Click itself if it has no ClickListener of it's own.
     *
     * @param position The index into the taskSchedule.get(day) List that has the given Task
     * @param day How many days past today's date the task is scheduled for
     * @param action 0: complete task, 1: delete task, 2: delete event
     */
    void onButtonClick(int position, @SuppressWarnings("unused") int day, int action, long id);
}
