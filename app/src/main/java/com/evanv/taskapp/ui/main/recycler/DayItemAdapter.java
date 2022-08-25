package com.evanv.taskapp.ui.main.recycler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.R;
import com.evanv.taskapp.ui.main.ClickListener;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Adapter to interface between data in DayItems and recyclerview in MainActivity
 *
 * @author Evan Voogd
 */
public class DayItemAdapter extends RecyclerView.Adapter<DayItemAdapter.DayViewHolder> {
    // Used to share views between the Event/Task RecyclerViews and the Day RecyclerViews
    private final RecyclerView.RecycledViewPool mTaskViewPool = new RecyclerView.RecycledViewPool();
    private final RecyclerView.RecycledViewPool mEventViewPool = new RecyclerView.RecycledViewPool();
    public List<DayItem> mDayItemList; // List of days
    // Listener that allows easy completion of tasks (see ClickListener)
    private final ClickListener mListener;

    /**
     * Constructs an adapter for MainActivity's recyclerview
     *
     * @param dayItemList the list of days for this user
     */
    @SuppressWarnings("unused")
    public DayItemAdapter(List<DayItem> dayItemList, ClickListener listener) {
        mDayItemList = dayItemList;
        mListener = listener;
    }

    /**
     * Initialize an individual layout for MainActivity's recyclerview
     *
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
     * Sets the date for the day_item and gives the component RecyclerViews their data
     *
     * @param holder DayViewHolder that represents the day to be changed
     * @param position Index in the dayItemList to be represented
     */
    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        // Get the DayItem for the days position past today's date
        DayItem dayItem = mDayItemList.get(position);

        // Set the day header to the string inside DayItem
        holder.mDayItemDate.setText(dayItem.getDayString());

        // Initialize the LinearLayoutManagers for the child RecyclerViews
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
        EventItemAdapter eventItemAdapter = new EventItemAdapter(dayItem.getEvents(), holder,
                dayItem.getIndex(), holder.mEventHeader);
        TaskItemAdapter taskItemAdapter = new TaskItemAdapter(dayItem.getTasks(), holder,
                dayItem.getIndex(), holder.mTaskHeader);
        holder.mEventRecyclerView.setLayoutManager(eventLayoutManager);
        holder.mTaskRecyclerView.setLayoutManager(taskLayoutManager);
        holder.mEventRecyclerView.setAdapter(eventItemAdapter);
        holder.mTaskRecyclerView.setAdapter(taskItemAdapter);
        holder.mEventRecyclerView.setRecycledViewPool(mEventViewPool);
        holder.mTaskRecyclerView.setRecycledViewPool(mTaskViewPool);

        holder.mTaskRecyclerView.addItemDecoration(new DividerItemDecoration
                (holder.mTaskRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
        holder.mEventRecyclerView.addItemDecoration(new DividerItemDecoration
                (holder.mEventRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
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
    @SuppressWarnings("InnerClassMayBeStatic")
    public class DayViewHolder extends RecyclerView.ViewHolder implements ClickListener {
        private final TextView mDayItemDate; // The TextView that displays the date
        private final TextView mTaskHeader;  // The TextView that says "Tasks"
        private final TextView mEventHeader; // The TextView that says "Events"
        // The recyclerview that displays this day's events
        private final RecyclerView mEventRecyclerView;
        // The recyclerview that displays this day's tasks
        private final RecyclerView mTaskRecyclerView;
        // Listener that allows easy completion of tasks (see ClickListener)
        private final WeakReference<ClickListener> mListenerRef;

        /**
         * Constructs a new DayViewHolder, setting it's values to the views in the day_item
         *
         * @param itemView View containing the views in the day_item
         */
        public DayViewHolder(@NonNull View itemView, ClickListener listener) {
            super(itemView);

            // Sets the DayViewHolder's fields.
            mListenerRef = new WeakReference<>(listener);
            mDayItemDate = itemView.findViewById(R.id.dayItemDate);
            mEventRecyclerView = itemView.findViewById(R.id.eventRecycler);
            mTaskRecyclerView = itemView.findViewById(R.id.taskRecycler);
            mTaskHeader = itemView.findViewById(R.id.taskHeader);
            mEventHeader = itemView.findViewById(R.id.eventHeader);
        }

        /**
         * Sends the Button Click information up from the TaskItemHolder to MainActivity. Adds the
         * day index, which is conveniently getAdapterPosition();
         *
         * @param position The index into the taskSchedule.get(day) List that has the given Task
         * @param day Ignored as TaskItemHolder does not know it's recycler's DayRecycler's index.
         */
        @Override
        public void onButtonClick(int position, int day, int action) {
            mListenerRef.get().onButtonClick(position, day, action);
        }
    }
}
