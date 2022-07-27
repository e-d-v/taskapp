package com.evanv.taskapp;

import android.os.Bundle;

/**
 * Interface that defines a class that returns a Bundle containing user input about an event's
 * recurrence.
 *
 * @author Evan Voogd
 */
public interface RecurInput {
    // Represents the field representing the type of recurrence this is.
    public static final String EXTRA_TYPE = "com.evanv.taskapp.RecurInput.extra.TYPE";

    /**
     * A function that gets the user input about recurrence.
     *
     * @return A bundle containing user input about recurrence.
     */
    public Bundle getRecurInfo();
}
