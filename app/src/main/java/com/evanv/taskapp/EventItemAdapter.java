package com.evanv.taskapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter to interface between data in EventItems and recyclerview in DayItem
 *
 * @author Evan Voogd
 */
public class EventItemAdapter extends RecyclerView.Adapter<EventItemAdapter.EventViewHolder> {
    private List<EventItem> mEventItemList; // List of events for this day

    /**
     * Constructs an adapter for a given DayItem's event recyclerview
     *
     * @param eventItemList the list of events for this day
     */
    public EventItemAdapter(List<EventItem> eventItemList) {
        mEventItemList = eventItemList;
    }

    /**
     * Initialize an individual layout for the dayitem's recyclerview
     * @param parent ViewGroup associated with the parent recyclerview
     * @param viewType not used, required by override
     * @return an EventViewHolder associated with the new layout
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates a new event_item, whose data will be filled by the EventViewHolder
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_item, parent, false);

        return new EventViewHolder(view);
    }

    /**
     * Sets the name/timespan of the the item in the recyclerview to that of it's EventItem
     * representation
     *
     * @param holder EventViewHolder that represents the item to be changed
     * @param position Index in the eventItemList to be represented
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventItem eventItem = mEventItemList.get(position);
        holder.mEventItemName.setText(eventItem.getName());
        holder.mEventItemTimespan.setText(eventItem.getTimespan());
    }

    /**
     * Gets the number of events for this day
     *
     * @return The number of events for this day
     */
    @Override
    public int getItemCount() {
        return mEventItemList.size();
    }

    /**
     * Holder that interfaces between the adapter and the event_item views
     */
    public class EventViewHolder extends RecyclerView.ViewHolder {
        TextView mEventItemName;     // The TextView representing the name in event_item
        TextView mEventItemTimespan; // The TextView representing the timespan in event_item

        /**
         * Constructs a new EventViewHolder, setting its values to the views in the event_item
         *
         * @param itemView View containing the views in the event_item
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            mEventItemName = itemView.findViewById(R.id.eventName);
            mEventItemTimespan = itemView.findViewById(R.id.timespan);
        }
    }
}
