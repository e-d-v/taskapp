package com.evanv.taskapp.logic;

import android.content.Context;
import android.os.Bundle;

import com.evanv.taskapp.R;
import com.evanv.taskapp.ui.additem.recur.DailyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.NoRecurFragment;
import com.evanv.taskapp.ui.additem.recur.RecurActivity;
import com.evanv.taskapp.ui.additem.recur.RecurInput;
import com.evanv.taskapp.ui.additem.recur.WeeklyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class that parses a Bundle containing recurrence information into a list of Dates the item occurs
 * on. Generalizes previous way of calculating recurrence so both events and tasks can use it, while
 * pulling large amounts of complicated logic out of a single function into it's own class.
 *
 * @author Evan Voogd
 */
public class RecurrenceParser {
    private final Context mContext; // Context required for resources

    /**
     * Create a new class to parse a recurrence bundle
     *
     * @param context Context of calling activity necessary for resource usage
     */
    public RecurrenceParser(Context context) {
        // Today's date
        mContext = context;
    }

    /**
     * Convert a bundle into a list of dates that the user has chosen to recur on.
     *
     * @param recurrenceBundle A bundle containing recurrence information from RecurActivity
     * @param itemStart Start day for item
     *
     * @return A list of dates, starting at itemStart, that follow the bundle's recurrence pattern
     */
    public List<LocalDate> parseBundle(Bundle recurrenceBundle, LocalDate itemStart) {
        // Calculate how many days past today's date this event is scheduled (used to
        // index into eventSchedule
        List<LocalDate> toReturn = new ArrayList<>();

        // If no recurrence, quit early
        if (recurrenceBundle.get(RecurInput.EXTRA_TYPE).equals(NoRecurFragment.EXTRA_VAL_TYPE)) {
            toReturn.add(itemStart);

            return toReturn;
        }

        // Check if user chose to recur until date or number of times
        boolean until = recurrenceBundle.get(RecurActivity.EXTRA_UNTIL_TYPE)
                .equals(RecurActivity.EXTRA_VAL_UNTIL);

        // Initialize until holders. Only the one associated with the user's choice
        // of recurrence is used.
        int numTimes;
        LocalDate endDate; // java doesn't understand if-else blocks apparently

        // Recur until end date
        if (until) {
            endDate = LocalDate.parse(recurrenceBundle.getString(RecurActivity.EXTRA_UNTIL),
                    Task.dateFormat);
            numTimes = Integer.MAX_VALUE; // Set unused to max value
        }
        // Recur set number of times
        else {
            numTimes = Integer.parseInt(recurrenceBundle.getString(RecurActivity.EXTRA_UNTIL));
            endDate = LocalDate.MAX; // Set unused to max value
        }

        Iterator<LocalDate> iterator = null;

        int interval;

        // Set iterator to iterator for specific recurrence type
        String recurType = recurrenceBundle.getString(RecurInput.EXTRA_TYPE);
        if (DailyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
            // Number of days between recurrences
            interval = recurrenceBundle.getInt(DailyRecurFragment.EXTRA_INTERVAL);

            // Fairly self-explanatory, interval is already the number of days between events
            iterator = new DailyIterator(itemStart, interval);
        }
        else if (WeeklyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
            // Number of weeks between recurrences
            interval = recurrenceBundle.getInt(WeeklyRecurFragment.EXTRA_INTERVAL);
            // day[0] = if recurs on sunday, day[6] if recurs on saturday
            boolean[] days = recurrenceBundle.getBooleanArray(WeeklyRecurFragment.EXTRA_DAYS);

            iterator = new WeeklyIterator(itemStart, days, interval);
        }
        // User chose to recur monthly
        else if (MonthlyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
            iterator = monthlyRecurDispatcher(recurrenceBundle, itemStart);
        }
        // The user chose to recur yearly
        else if (YearlyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
            iterator = yearlyRecurDispatcher(recurrenceBundle, itemStart);
        }

        if (iterator != null) {
            toReturn = listBuilder(iterator, endDate, numTimes);
        }

        return toReturn;
    }

    public List<LocalDateTime> parseBundle(Bundle recurrenceBundle, LocalDateTime itemStart) {
        List<LocalDate> withoutTimes = parseBundle(recurrenceBundle, itemStart.toLocalDate());
        List<LocalDateTime> toReturn = new ArrayList<>();

        for (LocalDate d : withoutTimes) {
            toReturn.add(d.atTime(itemStart.toLocalTime()));
        }

        return toReturn;
    }

    /**
     * Parse the MonthlyRecur case. Pulled from normal dispatcher as it's not one iterator like
     * No/Daily/Weekly
     *
     * @param recurrenceBundle Bundle describing recurrence from RecurActivity
     * @param itemStart Date of first occurrence
     *
     * @return An iterator describing the given recurrence logic
     */
    private Iterator<LocalDate> monthlyRecurDispatcher(Bundle recurrenceBundle, LocalDate itemStart) {
        // Get recur interval
        int interval = recurrenceBundle.getInt(MonthlyRecurFragment.EXTRA_INTERVAL);
        String intervalType = recurrenceBundle.getString(MonthlyRecurFragment.EXTRA_RECUR_TYPE);
        Iterator<LocalDate> iterator = null;

        // User chose to recur on the same day every month
        if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_STATIC)) {
            iterator = new MonthlyStaticIterator(itemStart, interval);
        }
        // If user chose to recur on the same weekday of every month (e.g. 3rd tuesday)
        if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_DYNAMIC)) {
            iterator = new MonthlyDynamicIterator(itemStart, interval);
        }
        // If user chose to recur on the same dates of every month (e.g. 2nd and 3rd)
        if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
            String[] days = recurrenceBundle.getString(MonthlyRecurFragment.EXTRA_DAYS)
                    .split(",");

            iterator = new MonthlySpecificIterator(itemStart, days, interval);
        }

        return iterator;
    }

    /**
     * Parse the Yearly case. Pulled from normal dispatcher as it's not one iterator like
     * No/Daily/Weekly
     *
     * @param recurrenceBundle Bundle describing recurrence from RecurActivity
     * @param itemStart Date of first occurrence
     *
     * @return An iterator describing the given recurrence logic
     */
    private Iterator<LocalDate> yearlyRecurDispatcher(Bundle recurrenceBundle, LocalDate itemStart) {
        // How many years between each recurrence of this event.
        int interval = recurrenceBundle.getInt(YearlyRecurFragment.EXTRA_INTERVAL);
        // How the event will recur (the 18th, 3rd monday, 18/21st etc.)
        String intervalType = recurrenceBundle.getString(YearlyRecurFragment.EXTRA_RECUR_TYPE);
        // What months to recur on if necessary.
        boolean[] months = new boolean[12];

        Iterator<LocalDate> iterator = null;

        // If necessary, see which months the user chose to recur on
        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_DYNAMIC)
                || intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_STATIC)
                || intervalType.equals(YearlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
            List<String> monthsStr = Arrays.asList(
                    recurrenceBundle.getString(YearlyRecurFragment.EXTRA_MONTHS).split(","));

            // For each month, see if the user chose to include it
            months[0] = monthsStr.contains(mContext.getString(R.string.jan));
            months[1] = monthsStr.contains(mContext.getString(R.string.feb));
            months[2] = monthsStr.contains(mContext.getString(R.string.mar));
            months[3] = monthsStr.contains(mContext.getString(R.string.apr));
            months[4] = monthsStr.contains(mContext.getString(R.string.may));
            months[5] = monthsStr.contains(mContext.getString(R.string.jun));
            months[6] = monthsStr.contains(mContext.getString(R.string.jul));
            months[7] = monthsStr.contains(mContext.getString(R.string.aug));
            months[8] = monthsStr.contains(mContext.getString(R.string.sep));
            months[9] = monthsStr.contains(mContext.getString(R.string.oct));
            months[10] = monthsStr.contains(mContext.getString(R.string.nov));
            months[11] = monthsStr.contains(mContext.getString(R.string.dec));
        }

        // If user chose to recur on same month/day
        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_STATIC)) {
            iterator = new YearlyStaticIterator(itemStart, interval);
        }
        // If user chose to recur on the same month/weekday (e.g. 3rd Mon of Sep)
        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_DYNAMIC)) {
            iterator = new YearlyDynamicIterator(itemStart, interval);
        }
        // If user chose to recur on multiple months on the same day (e.g. Jan/Feb 3rd)
        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_STATIC)) {
            iterator = new YearlyMultipleStaticIterator(itemStart, months, interval);
        }
        // If the user chose to recur on multiple months on the same weekday (e.g. 3rd
        // Monday of Jan/Feb)
        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_DYNAMIC)) {
            iterator = new YearlyMultipleDynamicIterator(itemStart, months, interval);
        }
        // If user chose to recur on specific months/days (e.g. 2nd/3rd of Jan/Feb)
        if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
            // Get the days to recur on from user input.
            String[] days = recurrenceBundle.getString(YearlyRecurFragment.EXTRA_DAYS)
                    .split(",");

            iterator = new YearlySpecificIterator(itemStart, months, days, interval);
        }

        return iterator;
    }

    /**
     * Generic list builder that takes the recurring logic from the recurrence calculation and
     * generalizes it. Takes multiple parameters that specify the exact recurrence relation.
     *
     * @param iterator An iterator that specifies the next date to recur on
     * @param endDate Latest day for recurrence
     * @param numTimes Number of times to recur
     *
     * @return A list of dates described by the given fields
     */
    private List<LocalDate> listBuilder(Iterator<LocalDate> iterator, LocalDate endDate,
                                        int numTimes) {
        List<LocalDate> toReturn = new ArrayList<>();

        LocalDate currDate = iterator.next(); // Current date to add
        int num = 0;                     // Number of times added

        // Add next date from iterator until either A) date is past endDate or B) more dates have
        // been added than the number of dates.
        while (!currDate.isAfter(endDate) && num < numTimes) {
            toReturn.add(currDate);

            currDate = iterator.next();
            num++;
        }

        return toReturn;
    }

    /**
     * Iterator that handles the daily recurrence case
     */
    private static class DailyIterator implements Iterator<LocalDate> {
        private final int mInterval; // Days between
        private LocalDate mCurrent;  // Current date

        /**
         * Construct a Iterator that generates dates for daily recurrences
         *  @param itemStart Start date to recur on
         * @param interval How many days between recurrences
         */
        public DailyIterator(LocalDate itemStart, int interval) {
            mCurrent = itemStart;
            mInterval = interval;
        }

        /**
         * Returns true as iterator calculates the next Date algorithmically
         *
         * @return true
         */
        @Override
        public boolean hasNext() {
            return true; // Always true as the iterator calculates algorithmically
        }

        /**
         * Returns the next Date in the recurrence
         *
         * @return The next date in the recurrence sequence
         */
        @Override
        public LocalDate next() {
            LocalDate toRet = mCurrent;

            mCurrent = toRet.plus(mInterval, ChronoUnit.DAYS);

            return toRet;
        }
    }

    /**
     * Iterator that handles the weekly recurrence case
     */
    private static class WeeklyIterator implements Iterator<LocalDate> {
        private LocalDate mCurrent;    // Current Date
        private final boolean[] mDays; // Days of week to recur on
        private final int mInterval;   // Weeks between recurrences
        private int mCurrDate;         // Current day of week

        /**
         * Construct a Iterator that generates dates for weekly recurrences
         *  @param itemStart Start date to recur on
         * @param interval How many weeks between recurrences
         */
        public WeeklyIterator(LocalDate itemStart, boolean[] days, int interval) {
            mCurrent = itemStart;

            mCurrDate = itemStart.get(ChronoField.DAY_OF_WEEK);

            mDays = days;
            mInterval = interval;
        }

        /**
         * Returns true as iterator calculates the next Date algorithmically
         *
         * @return true
         */
        @Override
        public boolean hasNext() {
            return true;
        }

        /**
         * Returns the next Date in the recurrence
         *
         * @return The next date in the recurrence sequence
         */
        @Override
        public LocalDate next() {
            LocalDate toRet = mCurrent;
            mCurrent = toRet.plus(1, ChronoUnit.DAYS);

            if (!mDays[mCurrDate]) {
                toRet = null;
            }

            if ((mCurrDate + 1) % 7 == 0) {
                mCurrent = mCurrent.plus(mInterval - 1, ChronoUnit.WEEKS);
            }

            mCurrDate = (mCurrDate + 1) % 7;

            return toRet == null ? next() : toRet;
        }
    }

    /**
     * Iterator that handles the monthly static case, where it's repeated monthly on the same date.
     */
    private static class MonthlyStaticIterator implements Iterator<LocalDate> {
        private LocalDate mCurrent;  // Current date
        private final int mInterval; // Number of months between recurrences

        /**
         * Construct a Iterator that generates dates for monthly static recurrences
         *  @param itemStart Start date to recur on
         * @param interval How many months between recurrences
         */
        public MonthlyStaticIterator(LocalDate itemStart, int interval) {
            mCurrent = itemStart;

            mInterval = interval;
        }

        /**
         * Returns true as iterator calculates the next Date algorithmically
         *
         * @return true
         */
        @Override
        public boolean hasNext() {
            return true; // Always true as the iterator calculates algorithmically
        }

        /**
         * Returns the next Date in the recurrence
         *
         * @return The next date in the recurrence sequence
         */
        @Override
        public LocalDate next() {
            LocalDate toRet = mCurrent;

            int date = mCurrent.get(ChronoField.DAY_OF_MONTH);

            mCurrent = toRet.plus(mInterval, ChronoUnit.MONTHS);

            int newDate = mCurrent.get(ChronoField.DAY_OF_MONTH);

            // Handle edge cases, such as recurring on the 30th in February
            while (newDate != date) {
                // Subtracting and adding allows us to set the exact date
                mCurrent = mCurrent.plus(mInterval, ChronoUnit.MONTHS);
                newDate = mCurrent.get(ChronoField.DAY_OF_MONTH);
                mCurrent = mCurrent.minus(newDate - 1, ChronoUnit.DAYS);
                mCurrent = mCurrent.plus(date - 1, ChronoUnit.DAYS);

                newDate = mCurrent.get(ChronoField.DAY_OF_MONTH);
            }

            return toRet;
        }
    }

    /**
     * Iterator that handles the case where it recurs monthly on the same dynamic date (e.g. 3rd
     * Monday)
     */
    private static class MonthlyDynamicIterator implements Iterator<LocalDate> {
        private LocalDate mCurrent;          // Current Date
        private final int mInterval;         // Months between recurrences
        private final int mDayOfWeek;        // Starting day of week
        private final int mDayOfWeekInMonth; // Starting day of week in month ("3rd" in 3rd Monday)

        /**
         * Construct a Iterator that generates dates for monthly dynamic recurrences
         *  @param itemStart Start date to recur on
         * @param interval How many months between recurrences
         */
        public MonthlyDynamicIterator(LocalDate itemStart, int interval) {
            mCurrent = itemStart;

            mDayOfWeek = mCurrent.get(ChronoField.DAY_OF_WEEK);
            mDayOfWeekInMonth = mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);

            mInterval = interval;
        }

        /**
         * Returns true as iterator calculates the next Date algorithmically
         *
         * @return true
         */
        @Override
        public boolean hasNext() {
            return true; // Always true as the iterator calculates algorithmically
        }

        /**
         * Returns the next Date in the recurrence
         *
         * @return The next date in the recurrence sequence
         */
        @Override
        public LocalDate next() {
            LocalDate toRet = mCurrent;

            mCurrent = toRet.plus(mInterval, ChronoUnit.MONTHS);

            // Set DAY_OF_WEEK
            int currDayOfWeek = mCurrent.get(ChronoField.DAY_OF_WEEK);
            mCurrent = toRet.minus(currDayOfWeek - 1, ChronoUnit.DAYS);
            mCurrent = mCurrent.plus(mDayOfWeek - 1, ChronoUnit.DAYS);

            // Set DAY_OF_WEEK_IN_MONTH
            int curDayOfWeekInMonth = mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
            mCurrent.minus(curDayOfWeekInMonth - 1, ChronoUnit.WEEKS);
            mCurrent.plus(mDayOfWeekInMonth - 1, ChronoUnit.WEEKS);

            // Handle edge cases, such as recurring on the 5th monday in February
            while (mCurrent.get(ChronoField.DAY_OF_WEEK) != mDayOfWeek ||
                    mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH) != mDayOfWeekInMonth) {
                mCurrent = toRet.plus(mInterval, ChronoUnit.MONTHS);

                // Set DAY_OF_WEEK
                currDayOfWeek = mCurrent.get(ChronoField.DAY_OF_WEEK);
                mCurrent = toRet.minus(currDayOfWeek - 1, ChronoUnit.DAYS);
                mCurrent = mCurrent.plus(mDayOfWeek - 1, ChronoUnit.DAYS);

                // Set DAY_OF_WEEK_IN_MONTH
                curDayOfWeekInMonth = mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
                mCurrent.minus(curDayOfWeekInMonth - 1, ChronoUnit.WEEKS);
                mCurrent.plus(mDayOfWeekInMonth - 1, ChronoUnit.WEEKS);
            }

            return toRet;
        }
    }

    /**
     * Iterator that handles the monthly specific case, where item recurs monthly on multiple
     * specific dates
     */
    private static class MonthlySpecificIterator implements Iterator<LocalDate> {
        private LocalDate mCurrent;        // Current Date
        private final List<Integer> mDays; // List of days to recur on
        private final int mInterval;       // Number of months between recurrences
        private int mCurrDay;              // Current day of month

        /**
         * Construct a Iterator that generates dates for monthly specific recurrences
         *  @param itemStart Start date to recur on
         * @param interval How many months between recurrences
         */
        public MonthlySpecificIterator(LocalDate itemStart, String[] days, int interval) {
            mCurrent = itemStart;

            mCurrDay = mCurrent.get(ChronoField.DAY_OF_MONTH);

            mDays = new ArrayList<>();
            for (String str : days) {
                mDays.add(Integer.parseInt(str));
            }
            Collections.sort(mDays);

            mInterval = interval;
        }

        /**
         * Returns true as iterator calculates the next Date algorithmically
         *
         * @return true
         */
        @Override
        public boolean hasNext() {
            return true;
        }

        /**
         * Returns the next Date in the recurrence
         *
         * @return The next date in the recurrence sequence
         */
        @Override
        public LocalDate next() {
            LocalDate toRet = mCurrent;

            // keep track of this so it's easier to see when month has been flipped
            int oldMonth = mCurrent.get(ChronoField.MONTH_OF_YEAR);

            mCurrent = toRet.plus(1, ChronoUnit.DAYS);
            mCurrDay = mCurrent.get(ChronoField.DAY_OF_MONTH);

            if (!mDays.contains(mCurrDay)) {
                toRet = null;
            }

            if (oldMonth != mCurrent.get(ChronoField.MONTH_OF_YEAR)) {
                mCurrent = mCurrent.plus(mInterval - 1, ChronoUnit.MONTHS);
            }

            return toRet == null ? next() : toRet;
        }
    }

    /**
     * Iterator that handles the case where item recurs on same month/date yearly
     */
    private static class YearlyStaticIterator implements Iterator<LocalDate> {
        private LocalDate mCurrent;  // Current Date
        private final int mInterval; // Number of years between items

        /**
         * Construct a Iterator that generates dates for yearly static recurrences
         *  @param itemStart Start date to recur on
         * @param interval How many years between recurrences
         */
        public YearlyStaticIterator(LocalDate itemStart, int interval) {
            mCurrent = itemStart;

            mInterval = interval;
        }

        /**
         * Returns true as iterator calculates the next Date algorithmically
         *
         * @return true
         */
        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public LocalDate next() {
            LocalDate toRet = mCurrent;

            int day = toRet.get(ChronoField.DAY_OF_MONTH);
            int month = toRet.get(ChronoField.MONTH_OF_YEAR);

            mCurrent = toRet.plus(mInterval, ChronoUnit.YEARS);

            // Handle edge cases such as when a given month doesn't have a given date
            while (mCurrent.get(ChronoField.DAY_OF_MONTH) != day ||
                    mCurrent.get(ChronoField.MONTH_OF_YEAR) != month) {
                mCurrent = mCurrent.plus(mInterval, ChronoUnit.YEARS);

                // Set Day of Month
                int newDate = mCurrent.get(ChronoField.DAY_OF_MONTH);
                mCurrent = mCurrent.minus(newDate - 1, ChronoUnit.DAYS);
                mCurrent = mCurrent.plus(day - 1, ChronoUnit.DAYS);

                // Set Month of year
                int newMonth = mCurrent.get(ChronoField.MONTH_OF_YEAR);
                mCurrent = mCurrent.minus(newMonth - 1, ChronoUnit.MONTHS);
                mCurrent = mCurrent.plus(month - 1, ChronoUnit.MONTHS);
            }

            return toRet;
        }
    }

    /**
     * Iterator that handles the case where item occurs yearly on the same dynamic date (e.g. 3rd
     * Monday)
     */
    private static class YearlyDynamicIterator implements Iterator<LocalDate> {
        private LocalDate mCurrent;          // Current Date
        private final int mInterval;         // Number of years between recurrences
        private final int mDayOfWeek;        // Starting day of week
        private final int mDayOfWeekInMonth; // Starting day of week in month ("3rd" in 3rd Monday)

        /**
         * Construct a Iterator that generates dates for yearly dynamic recurrences
         *  @param itemStart Start date to recur on
         * @param interval How many years between recurrences
         */
        public YearlyDynamicIterator(LocalDate itemStart, int interval) {
            mCurrent = itemStart;
            mDayOfWeek = mCurrent.get(ChronoField.DAY_OF_WEEK);
            mDayOfWeekInMonth = mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);

            mInterval = interval;
        }

        /**
         * Returns true as iterator calculates the next Date algorithmically
         *
         * @return true
         */
        @Override
        public boolean hasNext() {
            return true;
        }

        /**
         * Returns the next Date in the recurrence
         *
         * @return The next date in the recurrence sequence
         */
        @Override
        public LocalDate next() {
            LocalDate toRet = mCurrent;

            // Calculate next date
            mCurrent.plus(mInterval, ChronoUnit.YEARS);

            // Set DAY_OF_WEEK
            int currDayOfWeek = mCurrent.get(ChronoField.DAY_OF_WEEK);
            mCurrent = toRet.minus(currDayOfWeek - 1, ChronoUnit.DAYS);
            mCurrent = mCurrent.plus(mDayOfWeek - 1, ChronoUnit.DAYS);

            // Set DAY_OF_WEEK_IN_MONTH
            int curDayOfWeekInMonth = mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
            mCurrent.minus(curDayOfWeekInMonth - 1, ChronoUnit.WEEKS);
            mCurrent.plus(mDayOfWeekInMonth - 1, ChronoUnit.WEEKS);

            // Handle edge cases, such as recurring on the 5th monday in February
            while (mCurrent.get(ChronoField.DAY_OF_WEEK) != mDayOfWeek ||
                    mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH) != mDayOfWeekInMonth) {
                mCurrent = toRet.plus(mInterval, ChronoUnit.YEARS);

                // Set DAY_OF_WEEK
                currDayOfWeek = mCurrent.get(ChronoField.DAY_OF_WEEK);
                mCurrent = toRet.minus(currDayOfWeek - 1, ChronoUnit.DAYS);
                mCurrent = mCurrent.plus(mDayOfWeek - 1, ChronoUnit.DAYS);

                // Set DAY_OF_WEEK_IN_MONTH
                curDayOfWeekInMonth = mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
                mCurrent.minus(curDayOfWeekInMonth - 1, ChronoUnit.WEEKS);
                mCurrent.plus(mDayOfWeekInMonth - 1, ChronoUnit.WEEKS);
            }

            return toRet;
        }
    }

    /**
     * Iterator that handles the case where the item occurs on multiple months on the same date
     * yearly.
     */
    private static class YearlyMultipleStaticIterator implements Iterator<LocalDate> {
        private LocalDate mCurrent;      // Current Date
        private final boolean[] mMonths; // Months to recur on
        private final int mInterval;     // Years between recurrences
        private int mCurrMonth;          // Current month

        /**
         * Construct a Iterator that generates dates for yearly multiple static recurrences
         *  @param itemStart Start date to recur on
         * @param interval How many years between recurrences
         */
        public YearlyMultipleStaticIterator(LocalDate itemStart, boolean[] months, int interval) {
            mCurrent = itemStart;

            mMonths = months;
            mInterval = interval;
            mCurrMonth = mCurrent.get(ChronoField.MONTH_OF_YEAR);
        }

        /**
         * Returns true as iterator calculates the next Date algorithmically
         *
         * @return true
         */
        @Override
        public boolean hasNext() {
            return true;
        }

        /**
         * Returns the next Date in the recurrence
         *
         * @return The next date in the recurrence sequence
         */
        @Override
        public LocalDate next() {
            LocalDate toRet = mCurrent;

            int date = mCurrent.get(ChronoField.DAY_OF_MONTH);

            if (!mMonths[mCurrMonth]) {
                toRet = null;
            }

            if ((mCurrMonth + 1) % 12 == 0) {
                mCurrent = mCurrent.plus(mInterval - 1, ChronoUnit.YEARS);
            }

            mCurrent = mCurrent.plus(1, ChronoUnit.MONTHS);
            mCurrMonth = mCurrent.get(ChronoField.MONTH_OF_YEAR);

            // Handle edge cases such as when a given month doesn't have a given date
            while (mCurrent.get(ChronoField.DAY_OF_MONTH) != date) {
                if ((mCurrMonth + 1) % 12 == 0) {
                    mCurrent = mCurrent.plus(mInterval - 1, ChronoUnit.YEARS);
                }

                mCurrent = mCurrent.plus(1, ChronoUnit.MONTHS);
                mCurrMonth = mCurrent.get(ChronoField.MONTH_OF_YEAR);
            }

            return toRet == null ? next() : toRet;
        }
    }

    /**
     * Iterator that handles the case where the item recurs on multiple months on the same dynamic
     * date (e.g. 3rd Monday)
     */
    private static class YearlyMultipleDynamicIterator implements Iterator<LocalDate> {
        private LocalDate mCurrent;          // Current Date
        private final boolean[] mMonths;     // Months to recur on
        private final int mInterval;         // Number of years between recurrences
        private final int mDayOfWeek;        // Start date's day of week
        private final int mDayOfWeekInMonth; // Day of week in month of start date (3rd in 3rd Mon)
        private int mCurrMonth;              // Current month

        /**
         * Construct a Iterator that generates dates for yearly multiple dynamic recurrences
         *  @param itemStart Start date to recur on
         * @param interval How many years between recurrences
         */
        public YearlyMultipleDynamicIterator(LocalDate itemStart, boolean[] months, int interval) {
            mCurrent = itemStart;

            this.mMonths = months;
            this.mInterval = interval;
            mCurrMonth = mCurrent.get(ChronoField.MONTH_OF_YEAR);
            mDayOfWeek = mCurrent.get(ChronoField.DAY_OF_WEEK);
            mDayOfWeekInMonth = mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
        }

        /**
         * Returns true as iterator calculates the next Date algorithmically
         *
         * @return true
         */
        @Override
        public boolean hasNext() {
            return true;
        }

        /**
         * Returns the next Date in the recurrence
         *
         * @return The next date in the recurrence sequence
         */
        @Override
        public LocalDate next() {
            LocalDate toRet = mCurrent;

            if (!mMonths[mCurrMonth]) {
                toRet = null;
            }

            if ((mCurrMonth + 1) % 12 == 0) {
                mCurrent = mCurrent.plus(mInterval - 1, ChronoUnit.YEARS);
            }

            mCurrent = mCurrent.plus(1, ChronoUnit.MONTHS);
            mCurrMonth = mCurrent.get(ChronoField.MONTH_OF_YEAR);

            // Set DAY_OF_WEEK
            int currDayOfWeek = mCurrent.get(ChronoField.DAY_OF_WEEK);
            mCurrent = mCurrent.minus(currDayOfWeek - 1, ChronoUnit.DAYS);
            mCurrent = mCurrent.plus(mDayOfWeek - 1, ChronoUnit.DAYS);

            // Set DAY_OF_WEEK_IN_MONTH
            int curDayOfWeekInMonth = mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
            mCurrent.minus(curDayOfWeekInMonth - 1, ChronoUnit.WEEKS);
            mCurrent.plus(mDayOfWeekInMonth - 1, ChronoUnit.WEEKS);

            // Handle edge cases such as when a given month doesn't have a given date
            while (mCurrent.get(ChronoField.DAY_OF_WEEK) != mDayOfWeek ||
                    mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH) != mDayOfWeekInMonth) {
                if ((mCurrMonth + 1) % 12 == 0) {
                    mCurrent = mCurrent.plus(mInterval - 1, ChronoUnit.YEARS);
                }

                mCurrent = mCurrent.plus(1, ChronoUnit.MONTHS);
                mCurrMonth = mCurrent.get(ChronoField.MONTH_OF_YEAR);

                // Set DAY_OF_WEEK
                currDayOfWeek = mCurrent.get(ChronoField.DAY_OF_WEEK);
                mCurrent = mCurrent.minus(currDayOfWeek - 1, ChronoUnit.DAYS);
                mCurrent = mCurrent.plus(mDayOfWeek - 1, ChronoUnit.DAYS);

                // Set DAY_OF_WEEK_IN_MONTH
                curDayOfWeekInMonth = mCurrent.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
                mCurrent.minus(curDayOfWeekInMonth - 1, ChronoUnit.WEEKS);
                mCurrent.plus(mDayOfWeekInMonth - 1, ChronoUnit.WEEKS);
            }

            return toRet == null ? next() : toRet;
        }
    }

    /**
     * Iterator that handles the case where the item recurs on specific months/dates yearly
     */
    private static class YearlySpecificIterator implements Iterator<LocalDate> {
        private LocalDate mCurrent;        // Current Date
        private final boolean[] mMonths;   // Months to recur on
        private final List<Integer> mDays; // Days to recur on
        private final int mInterval;       // Number of years between recurrences
        private int mCurrMonth;            // Current month
        private int mCurrDay;              // Current date

        /**
         * Construct a Iterator that generates dates for yearly dynamic recurrences
         *  @param itemStart Start date to recur on
         * @param interval How many years between recurrences
         */
        public YearlySpecificIterator(LocalDate itemStart, boolean[] months, String[] days, int interval) {
            mCurrent = itemStart;

            mCurrMonth = mCurrent.get(ChronoField.MONTH_OF_YEAR);
            mCurrDay = mCurrent.get(ChronoField.DAY_OF_MONTH);

            mDays = new ArrayList<>();
            for (String str : days) {
                this.mDays.add(Integer.parseInt(str));
            }

            mMonths = months;
            mInterval = interval;
        }

        /**
         * Returns true as iterator calculates the next Date algorithmically
         *
         * @return true
         */
        @Override
        public boolean hasNext() {
            return true;
        }

        /**
         * Returns the next Date in the recurrence
         *
         * @return The next date in the recurrence sequence
         */
        @Override
        public LocalDate next() {
            LocalDate toRet = mCurrent;

            if (!mMonths[mCurrMonth] || !mDays.contains(mCurrDay)) {
                toRet = null;
            }

            if (mCurrMonth == 11 && mCurrDay == 31) {
                mCurrent = mCurrent.plus(mInterval - 1, ChronoUnit.YEARS);
            }

            mCurrent = mCurrent.plus(1, ChronoUnit.DAYS);
            mCurrMonth = mCurrent.get(ChronoField.MONTH_OF_YEAR);
            mCurrDay = mCurrent.get(ChronoField.DAY_OF_MONTH);

            return toRet == null ? next() : toRet;
        }
    }
}
