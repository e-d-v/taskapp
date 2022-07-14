package com.evanv.taskapp;

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
 * Adapter to interface between data in EventItems and recyclerview in DayItem
 *
 * @author Evan Voogd
 */
public class EventItemAdapter extends RecyclerView.Adapter<EventItemAdapter.EventViewHolder> {
    private final List<EventItem> mEventItemList; // List of events for this day
    // Listener that allows easy deletion of events (see ClickListener)
    private final ClickListener mListener;

    /**
     * Constructs an adapter for a given DayItem's event recyclerview
     *
     * @param eventItemList the list of events for this day
     * @param listener ClickListener to handle button clicks
     */
    public EventItemAdapter(List<EventItem> eventItemList, ClickListener listener) {
        mEventItemList = eventItemList;
        mListener = listener;
    }

    /**
     * Initialize an individual layout for the DayItem's recyclerview
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

        return new EventViewHolder(view, mListener);
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
    public class EventViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mEventItemName;     // The TextView representing the name in event_item
        TextView mEventItemTimespan; // The TextView representing the timespan in event_item
        // Listener that allows easy completion of tasks (see ClickListener)
        WeakReference<ClickListener> mListenerRef;
        private final int DELETE_ID; // The ID of the delete button;

        /**
         * Constructs a new EventViewHolder, setting its values to the views in the event_item
         *
         * @param itemView View containing the views in the event_item
         * @param listener ClickListener to handle button clicks
         */
        public EventViewHolder(@NonNull View itemView, ClickListener listener) {
            super(itemView);
            mEventItemName = itemView.findViewById(R.id.eventName);
            mEventItemTimespan = itemView.findViewById(R.id.timespan);
            mListenerRef = new WeakReference<>(listener);
            DELETE_ID = R.id.buttonDeleteEvent;

            // Sets this as the OnClickListener for the button, so when the button is clicked, we
            // can move up the ClickListener chain to mark the event as deleted in MainActivity's
            // data structures and refresh the recyclerview
            ImageButton delete = itemView.findViewById(R.id.buttonDeleteEvent);
            delete.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // If the view clicked was the button, tell the DayViewHolder the index of the event to
            // be deleted. As the TaskViewHolder doesn't know the day index, this is -1, and will be
            // filled in by the DayViewHolder
            if (view.getId() == DELETE_ID) {
                mListenerRef.get().onButtonClick(getAdapterPosition(), -1, 2);
            }
        }
    }
}
