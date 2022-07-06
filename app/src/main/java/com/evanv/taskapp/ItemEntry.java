package com.evanv.taskapp;

import android.os.Bundle;

/**
 * An interface describing a Fragment that has a method that returns a Bundle with all of it's
 * fields, allowing MainActivity to retrieve all the fields for Event/Task entry easily
 */
public interface ItemEntry {
    /**
     * A method that returns a Bundle containing all of the fields required to create an Item
     * (e.g. Task or Event). It's AddItem.EXTRA_TYPE String Extra describes the type of Item it is,
     * so MainActivity can get the Item-type-specific extras
     *
     * @return A bundle containing all the fields needed to make a specific item.
     */
    Bundle getItem();
}
