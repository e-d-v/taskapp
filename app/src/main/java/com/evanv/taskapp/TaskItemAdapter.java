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
 */
public class TaskItemAdapter extends RecyclerView.Adapter<TaskItemAdapter.TaskViewHolder> {
    private List<TaskItem> mTaskItemList; // List of tasks for this day
    private ClickListener mListener;

    /**
     * Constructs an adapter for a given DayItem's task recyclerview
     *
     * @param taskItemList the list of tasks for this day
     */
    public TaskItemAdapter(List<TaskItem> taskItemList, ClickListener listener) {
        mTaskItemList = taskItemList;
        mListener = listener;
    }

    /**
     * Initialize an individual layout for the dayitem's recyclerview
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
        WeakReference<ClickListener> mListenerRef;

        private int BUTTON_ID;

        /**
         * Constructs a new TaskViewHolder, setting it's values to the views in the task_item
         *
         * @param itemView View containing the views in the task_item
         */
        public TaskViewHolder(@NonNull View itemView, ClickListener listener) {
            super(itemView);
            mTaskItemName = itemView.findViewById(R.id.taskName);
            mListenerRef = new WeakReference<>(listener);
            BUTTON_ID = R.id.buttonComplete;

            ImageButton button = itemView.findViewById(R.id.buttonComplete);
            button.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == BUTTON_ID) {
                mListenerRef.get().onButtonClick(getAdapterPosition(), -1);
            }
        }
    }
}
