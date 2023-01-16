package com.evanv.taskapp.ui.additem;

/**
 * An interface describing a Fragment that has a method that attempts to add the described item to
 * the LogicSubsystem.
 *
 * @author Evan Voogd
 */
public interface ItemEntry {
    /**
     * Add the item the user has entered into the LogicSubsystem. Return true if successful, false
     * if not.
     *
     * @return true if successful, false if not.
     */
    @SuppressWarnings("unused")
    boolean addItem();
}
