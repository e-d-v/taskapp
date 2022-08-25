package com.evanv.taskapp.ui.additem.recur;

import android.os.Bundle;

/**
 * Interface that defines a class that returns a Bundle containing user input about an event's
 * recurrence.
 *
 * @author Evan Voogd
 */
public interface RecurInput {
    // Represents the field representing the type of recurrence this is.
    String EXTRA_TYPE = "com.evanv.taskapp.ui.additem.recur.RecurInput.extra.TYPE";

    /**
     * A function that gets the user input about recurrence.
     *
     * @return A bundle containing user input about recurrence.
     */
    Bundle getRecurInfo();
}
