package com.evanv.taskapp.ui.main.recycler;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.R;
import com.evanv.taskapp.ui.main.ClickListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Adapter to interface between data in TaskItems and recyclerview in DayItem
 *
 * @author Evan Voogd
 */
public class TaskItemAdapter extends RecyclerView.Adapter<TaskItemAdapter.TaskViewHolder> {
    // Listener that allows easy completion of tasks (see ClickListener)
    private final ClickListener mListener;
    public final List<TaskItem> mTaskItemList; // List of tasks for this day
    private final int mDay;                    // Index into taskSchedule representing this day
    private final Context mContext;            // Context for resources.
    private final Activity mActivity;          // Activity for Context Menu

    /**
     * Constructs an adapter for a given DayItem's task recyclerview
     *  @param taskItemList the list of tasks for this day
     * @param listener ClickListener to handle button clicks
     * @param day Index into taskSchedule representing this day
     * @param header Header for task list for this day
     * @param workAhead true if "Work Ahead" should be displayed in header.
     */
    public TaskItemAdapter(List<TaskItem> taskItemList, ClickListener listener, int day,
                           TextView header, boolean workAhead, Activity activity) {
        mTaskItemList = taskItemList;
        mListener = listener;
        mDay = day;
        mContext = activity;
        mActivity = activity;

        // If the task list is empty, hide the "Tasks" subheader
        if (header == null) {
            return;
        }

        if (taskItemList.size() == 0) {
            header.setVisibility(View.INVISIBLE);
            header.setLayoutParams(
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        }
        else {
            header.setVisibility(View.VISIBLE);

            // Change header to work ahead if necessary.
            if (workAhead) {
                header.setText(R.string.work_ahead);
            }
            else {
                header.setText(R.string.tasks);
            }

            header.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    /**
     * Initialize an individual layout for the DayItem's recyclerview
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

        SpannableString name = new SpannableString(taskItem.getName());

        // If a task is timed, set it's name to be red.
        if (taskItem.isTimed()) {
            holder.options.setColorFilter(Color.RED);
        }
        else {
            holder.options.setColorFilter(ContextCompat.getColor(mContext, R.color.text_primary));
        }

        switch (taskItem.getPriority()) {
            case 0:
                holder.complete.setColorFilter(ContextCompat.getColor(mContext, R.color.text_primary));
                break;
            case 1:
                holder.complete.setColorFilter(ContextCompat.getColor(mContext, R.color.gold));
                break;
            case 2:
                holder.complete.setColorFilter(ContextCompat.getColor(mContext, R.color.orange));
                break;
            case 4:
                name.setSpan(new ForegroundColorSpan(Color.RED), 0, name.length(), 0);
            case 3:
                holder.complete.setColorFilter(Color.RED);
                break;
        }

        int start = name.toString().indexOf('\n');
        Log.d("START", "" + start);
        StyleSpan span = new StyleSpan(android.graphics.Typeface.BOLD);
        name.setSpan(span, start, name.length(), 0);
        RelativeSizeSpan span2 = new RelativeSizeSpan((float)(7.0/9.0));
        name.setSpan(span2, start, name.length(), 0);
        ForegroundColorSpan span3 = new ForegroundColorSpan(Color.parseColor("#B8B8B8"));
        name.setSpan(span3, start, name.length(), 0);

        // Set project color
        int[] colors = {R.color.pale_blue,
                R.color.blue,
                R.color.pale_green,
                R.color.green,
                R.color.pink,
                R.color.red,
                R.color.pale_orange,
                R.color.orange,
                R.color.lavender,
                R.color.purple,
                R.color.yellow,
                R.color.gray};

        // What color is most readable on the background
        int[] textColors = { Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.BLACK};

        // Show/hide the bar
        if (taskItem.getProject() == null && taskItem.getLabels().size() == 0) {
            holder.bar.setVisibility(View.INVISIBLE);
            holder.labels.setMinimumHeight(0);
            holder.hsv.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        }
        else {
            holder.bar.setVisibility(View.VISIBLE);
            holder.hsv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 80));
        }

        // Set project chip
        if (taskItem.getProject() != null) {
            // Show chip
            holder.project.setChipMinHeightResource(R.dimen.chip_height);
            holder.project.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            holder.project.setVisibility(View.VISIBLE);

            // Set project name
            holder.project.setText(taskItem.getProject());

            // Set colors
            holder.project.setChipBackgroundColorResource(colors[taskItem.getProjectColor()]);
            holder.project.setTextColor(textColors[taskItem.getProjectColor()]);
        }
        else {
            holder.project.setChipMinHeight(0);
            holder.project.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
            holder.project.setVisibility(View.INVISIBLE);
        }

        holder.mTaskItemName.setText(name);

        holder.complete.setOnClickListener(view -> {
            // If the view clicked was a button, tell the DayViewHolder the index of the task to be
            // completed or deleted. As the TaskViewHolder doesn't know the day index, this is -1,
            // and will be filled in by the DayViewHolder
            if (view.getId() == holder.COMPLETE_ID) {
                holder.mListenerRef.get().onButtonClick(holder.mIndex, mDay, 0);
            }
        });
        holder.options.setOnClickListener(view -> {
            // Tell MainActivity what item to perform actions on
            mListener.onButtonClick(position, mDay, 2);

            // Handle onClickListener
            mActivity.registerForContextMenu(holder.options);
            mActivity.openContextMenu(view);
            mActivity.unregisterForContextMenu(view);
        });
        holder.mIndex = taskItem.getIndex();

        holder.labels.removeAllViews();

        for (int i = 0; i < taskItem.getLabels().size(); i++) {
            Chip toAdd = new Chip(mContext);
            toAdd.setText(taskItem.getLabels().get(i).trim());
            toAdd.setChipMinHeightResource(R.dimen.text_height);
            toAdd.setClickable(false);
            toAdd.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            toAdd.setChipBackgroundColorResource(colors[taskItem.getLabelColors().get(i)]);
            toAdd.setTextColor(textColors[taskItem.getLabelColors().get(i)]);
            toAdd.setEnsureMinTouchTargetSize(false);
            toAdd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            holder.labels.addView(toAdd);
        }
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
    @SuppressWarnings("InnerClassMayBeStatic")
    public class TaskViewHolder extends RecyclerView.ViewHolder {
        final TextView mTaskItemName; // The TextView representing the name in task_item
        private final int COMPLETE_ID; // ID of the completion button.
        // Listener that allows easy completion of tasks (see ClickListener)
        final WeakReference<ClickListener> mListenerRef;
        final ImageButton complete;
        final ImageButton options;
        final Chip project;
        final ChipGroup labels;
        final View bar;
        final HorizontalScrollView hsv;
        int mIndex; // Index into taskSchedule.get(day) for this event

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

            // Sets this as the OnClickListener for the button, so when the button is clicked, we
            // can move up the ClickListener chain to mark the task as complete in MainActivity's
            // data structures and refresh the recyclerview
            complete = itemView.findViewById(R.id.buttonComplete);
            options = itemView.findViewById(R.id.buttonTaskOptions);
            project = itemView.findViewById(R.id.projectChip);
            labels = itemView.findViewById(R.id.labelChipGroup);
            bar = itemView.findViewById(R.id.bar);
            hsv = itemView.findViewById(R.id.hsvChips);
        }
    }
}
