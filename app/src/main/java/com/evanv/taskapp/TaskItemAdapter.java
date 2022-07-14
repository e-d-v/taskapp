package com.evanv.taskapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Adapter to interface between data in TaskItems and recyclerview in DayItem
 *
 * @author Evan Voogd
 */
public class TaskItemAdapter extends RecyclerView.Adapter<TaskItemAdapter.TaskViewHolder> {
    private final List<TaskItem> mTaskItemList; // List of tasks for this day
    // Listener that allows easy completion of tasks (see ClickListener)
    private final ClickListener mListener;

    /**
     * Constructs an adapter for a given DayItem's task recyclerview
     *
     * @param taskItemList the list of tasks for this day
     * @param listener ClickListener to handle button clicks
     */
    public TaskItemAdapter(List<TaskItem> taskItemList, ClickListener listener) {
        mTaskItemList = taskItemList;
        mListener = listener;
    }

    /**
     * Initialize an individual layout for the dayitem's recyclerview
     *
     * @param parent ViewGroup associated with the parent recyclerview
     * @param viewType not used, required by override
     * @return TaskViewHolder associated with the new layout
     */
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates a new task_item, whose data will be filled by the TaskViewHolder
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);

        return new TaskViewHolder(view, mListener);
    }

    /**
     * Sets the name of the the item in the recyclerview to that of it's TaskItem representation
     *
     * @param holder TaskViewHolder that represents the item to be changed
     * @param position Index in the taskItemList to be represented
     */
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem taskItem = mTaskItemList.get(position);
        holder.mTaskItemName.setText(taskItem.getName());
    }

    /**
     * Gets the number of tasks for this day
     *
     * @return The number of tasks for this day
     */
    @Override
    public int getItemCount() {
        return mTaskItemList.size();
    }

    /**
     * Holder that interfaces between the adapter and the task_item views
     */
    public class TaskViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mTaskItemName; // The TextView representing the name in task_item
        private final int COMPLETE_ID; // ID of the completion button.
        private final int DELETE_ID; // ID of the deletion button.
        // Listener that allows easy completion of tasks (see ClickListener)
        WeakReference<ClickListener> mListenerRef;


        /**
         * Constructs a new TaskViewHolder, setting it's values to the views in the task_item
         *
         * @param itemView View containing the views in the task_item
         */
        public TaskViewHolder(@NonNull View itemView, ClickListener listener) {
            super(itemView);
            // Sets the needed fields
            mTaskItemName = itemView.findViewById(R.id.taskName);
            mListenerRef = new WeakReference<>(listener);
            COMPLETE_ID = R.id.buttonComplete;
            DELETE_ID = R.id.buttonDeleteTask;

            // Sets this as the OnClickListener for the button, so when the button is clicked, we
            // can move up the ClickListener chain to mark the task as complete in MainActivity's
            // data structures and refresh the recyclerview
            ImageButton complete = itemView.findViewById(R.id.buttonComplete);
            complete.setOnClickListener(this);
            ImageButton delete = itemView.findViewById(R.id.buttonDeleteTask);
            delete.setOnClickListener(this);

        }

        /**
         * Uses the ClickListener chain to mark the task as complete in MainActivity
         *
         * @param v the view that was clicked
         */
        @Override
        public void onClick(View v) {
            // If the view clicked was a button, tell the DayViewHolder the index of the task to be
            // completed or deleted. As the TaskViewHolder doesn't know the day index, this is -1,
            // and will be filled in by the DayViewHolder
            if (v.getId() == COMPLETE_ID) {
                mListenerRef.get().onButtonClick(getAdapterPosition(), -1, 0);
            }
            if (v.getId() == DELETE_ID) {
                mListenerRef.get().onButtonClick(getAdapterPosition(), -1, 1);
            }
        }
    }
}
