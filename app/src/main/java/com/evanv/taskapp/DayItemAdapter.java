package com.evanv.taskapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.List;

public class DayItemAdapter extends RecyclerView.Adapter<DayItemAdapter.DayViewHolder> {
    // Used to share views between the Event/Task recyclerviews and the Day recyclerviews
    private RecyclerView.RecycledViewPool mTaskViewPool = new RecyclerView.RecycledViewPool();
    private RecyclerView.RecycledViewPool mEventViewPool = new RecyclerView.RecycledViewPool();
    public List<DayItem> mDayItemList; // List of days
    private ClickListener mListener;

    /**
     * Constructs an adapter for MainActivity's recyclerview
     *
     * @param dayItemList the list of days for this user
     */
    public DayItemAdapter(List<DayItem> dayItemList, ClickListener listener) {
        mDayItemList = dayItemList;
        mListener = listener;
    }

    /**
     * Initialize an individual layout for MainActivity's recyclerview
     * @param parent ViewGroup associated with the parent recyclerview
     * @param viewType not used, required by override
     * @return a DayViewHolder associated with the new layout
     */
    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates a new day_item, whose data will be filled by the DayViewHolder
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.day_item, parent, false);

        return new DayViewHolder(view, mListener);
    }

    /**
     * Sets the date for the day_item and gives the component recyclerviews their data
     *
     * @param holder DayViewHolder that represents the day to be changed
     * @param position Index in the dayItemList to be represented
     */
    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        DayItem dayItem = mDayItemList.get(position);

        holder.mDayItemDate.setText(dayItem.getDayString());

        // Hide/show headers depending on if any events/tasks are scheduled for that day
        if (dayItem.getTasks().size() == 0) {
            holder.mTaskHeader.setVisibility(View.INVISIBLE);
            holder.mTaskHeader.setHeight(0);
        }
        if (dayItem.getEvents().size() == 0) {
            holder.mEventHeader.setVisibility(View.INVISIBLE);
            holder.mEventHeader.setHeight(0);
        }

        // Initialize the LinearLayoutManagers for the child recyclerviews
        LinearLayoutManager eventLayoutManager = new LinearLayoutManager(
                holder.mEventRecyclerView.getContext(), LinearLayoutManager.VERTICAL,
                false);
        LinearLayoutManager taskLayoutManager = new LinearLayoutManager(
                holder.mTaskRecyclerView.getContext(), LinearLayoutManager.VERTICAL,
                false);

        // Tell the layout manager the number of elements in this day's events/layout
        eventLayoutManager.setInitialPrefetchItemCount(dayItem.getEvents().size());
        taskLayoutManager.setInitialPrefetchItemCount(dayItem.getTasks().size());

        // Initialize the Event/Task Item Adapters
        EventItemAdapter eventItemAdapter = new EventItemAdapter(dayItem.getEvents());
        TaskItemAdapter taskItemAdapter = new TaskItemAdapter(dayItem.getTasks(), holder);

        holder.mEventRecyclerView.setLayoutManager(eventLayoutManager);
        holder.mTaskRecyclerView.setLayoutManager(taskLayoutManager);

        holder.mEventRecyclerView.setAdapter(eventItemAdapter);
        holder.mTaskRecyclerView.setAdapter(taskItemAdapter);

        holder.mEventRecyclerView.setRecycledViewPool(mEventViewPool);
        holder.mTaskRecyclerView.setRecycledViewPool(mTaskViewPool);
    }

    /**
     * Gets the number of days currently represented by the associated recyclerview
     *
     * @return the number of days currently represented by the associated recyclerview
     */
    @Override
    public int getItemCount() {
        return mDayItemList.size();
    }

    /**
     * Holder that interfaces between the adapter and the day_item views
     */
    public class DayViewHolder extends RecyclerView.ViewHolder implements ClickListener {
        private TextView mDayItemDate;           // The TextView that displays the date
        private RecyclerView mEventRecyclerView; // The recyclerview that displays this day's events
        private RecyclerView mTaskRecyclerView;  // The recyclerview that displays this day's tasks
        private TextView mTaskHeader;            // The TextView that says "Tasks"
        private TextView mEventHeader;           // The TextView that says "Events"
        private Context mContext;                 // The Context of the given DayViewHolder

        private WeakReference<ClickListener> mListenerRef;

        /**
         * Constructs a new DayViewHolder, setting it's values to the views in the day_item
         *
         * @param itemView View containing the views in the day_item
         */
        public DayViewHolder(@NonNull View itemView, ClickListener listener) {
            super(itemView);

            mListenerRef = new WeakReference<>(listener);

            mDayItemDate = itemView.findViewById(R.id.dayItemDate);
            mEventRecyclerView = itemView.findViewById(R.id.eventRecycler);
            mTaskRecyclerView = itemView.findViewById(R.id.taskRecycler);
            mTaskHeader = itemView.findViewById(R.id.taskHeader);
            mEventHeader = itemView.findViewById(R.id.eventHeader);

            // We have to keep track of context so TextAppearance can be changed in onBindViewHolder
            mContext = itemView.getContext();
        }

        @Override
        public void onButtonClick(int position, int day) {
            mListenerRef.get().onButtonClick(position, getAdapterPosition());
        }
    }
}
