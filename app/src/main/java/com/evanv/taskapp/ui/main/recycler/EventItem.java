package com.evanv.taskapp.ui.main.recycler;

/**
 * Class representing an Event for use in the DayItem's recyclerview
 *
 * @author Evan Voogd
 */
public class EventItem {
    private final String mName;     // Name of the event (e.g. "Staff Meeting")
    private final String mTimespan; // Timespan of the event (e.g. "2-4pm")

    /**
     * Creates an item representing an Event
     *
     * @param name a string representing the name of the Event
     * @param timespan a string representing the timespan of the Event
     * @param index Index into eventSchedule.get(day) for this event
     */
    @SuppressWarnings("unused")
    public EventItem(String name, String timespan, int index) {
        mName = name;
        mTimespan = timespan;
        // Index into eventSchedule.get(day) for this event
    }

    /**
     * Returns string representing the name of the Event
     *
     * @return a string representing the name of the Event
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns string representing the timespan of the Event
     *
     * @return a string representing the timespan of the Event
     */
    public String getTimespan() {
        return mTimespan;
    }

}
