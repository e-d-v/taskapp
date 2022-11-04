package com.evanv.taskapp.logic;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.evanv.taskapp.R;
import com.evanv.taskapp.ui.additem.recur.DailyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.MonthlyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.NoRecurFragment;
import com.evanv.taskapp.ui.additem.recur.RecurActivity;
import com.evanv.taskapp.ui.additem.recur.RecurInput;
import com.evanv.taskapp.ui.additem.recur.WeeklyRecurFragment;
import com.evanv.taskapp.ui.additem.recur.YearlyRecurFragment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
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
    private final Date MAX_DATE;    // Maximum possible date

    /**
     * Create a new class to parse a recurrence bundle
     *
     * @param context Context of calling activity necessary for resource usage
     */
    public RecurrenceParser(Context context) {
        // Today's date
        mContext = context;

        Calendar maxDateCalculator = Calendar.getInstance();
        maxDateCalculator.setTimeInMillis(Long.MAX_VALUE);
        MAX_DATE = maxDateCalculator.getTime();
    }

    /**
     * Convert a bundle into a list of dates that the user has chosen to recur on.
     *
     * @param recurrenceBundle A bundle containing recurrence information from RecurActivity
     * @param itemStart Start day for item
     *
     * @return A list of dates, starting at itemStart, that follow the bundle's recurrence pattern
     */
    public List<Date> parseBundle(Bundle recurrenceBundle, Date itemStart) {
        // Calculate how many days past today's date this event is scheduled (used to
        // index into eventSchedule
        List<Date> toReturn = new ArrayList<>();

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
        Date endDate = new Date(); // java doesn't understand if-else blocks apparently

        // Recur until end date
        if (until) {
            try {
                endDate = Task.dateFormat.parse
                        (recurrenceBundle.getString(RecurActivity.EXTRA_UNTIL));
            } catch (ParseException e) {
                Log.e("TaskApp.MainActivity", e.getMessage());
            }
            numTimes = Integer.MAX_VALUE; // Set unused to max value
        }
        // Recur set number of times
        else {
            numTimes = Integer.parseInt(recurrenceBundle.getString(RecurActivity.EXTRA_UNTIL));
            endDate = MAX_DATE; // Set unused to max value
        }

        Iterator<Date> iterator = null;

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

    /**
     * Parse the MonthlyRecur case. Pulled from normal dispatcher as it's not one iterator like
     * No/Daily/Weekly
     *
     * @param recurrenceBundle Bundle describing recurrence from RecurActivity
     * @param itemStart Date of first occurrence
     *
     * @return An iterator describing the given recurrence logic
     */
    private Iterator<Date> monthlyRecurDispatcher(Bundle recurrenceBundle, Date itemStart) {
        // Get recur interval
        int interval = recurrenceBundle.getInt(MonthlyRecurFragment.EXTRA_INTERVAL);
        String intervalType = recurrenceBundle.getString(MonthlyRecurFragment.EXTRA_RECUR_TYPE);
        Iterator<Date> iterator = null;

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
    private Iterator<Date> yearlyRecurDispatcher(Bundle recurrenceBundle, Date itemStart) {
        // How many years between each recurrence of this event.
        int interval = recurrenceBundle.getInt(YearlyRecurFragment.EXTRA_INTERVAL);
        // How the event will recur (the 18th, 3rd monday, 18/21st etc.)
        String intervalType = recurrenceBundle.getString(YearlyRecurFragment.EXTRA_RECUR_TYPE);
        // What months to recur on if necessary.
        boolean[] months = new boolean[12];

        Iterator<Date> iterator = null;

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
    private List<Date> listBuilder(Iterator<Date> iterator, Date endDate, int numTimes) {
        List<Date> toReturn = new ArrayList<>();

        Date currDate = iterator.next(); // Current date to add
        int num = 0;                     // Number of times added

        // Add next date from iterator until either A) date is past endDate or B) more dates have
        // been added than the number of dates.
        while (!currDate.after(endDate) && num < numTimes) {
            toReturn.add(currDate);

            currDate = iterator.next();
            num++;
        }

        return toReturn;
    }

    /**
     * Iterator that handles the daily recurrence case
     */
    private static class DailyIterator implements Iterator<Date> {
        private final Date mItemStart;    // Start date
        private final int mInterval;      // Days between
        private final Calendar mRecurCal; // Calendar used to calculate next day

        /**
         * Construct a Iterator that generates dates for daily recurrences
         *
         * @param itemStart Start date to recur on
         * @param interval How many days between recurrences
         */
        public DailyIterator(Date itemStart, int interval) {
            mItemStart = itemStart;
            mInterval = interval;

            mRecurCal = Calendar.getInstance();
            mRecurCal.setTime(mItemStart);
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
        public Date next() {
            Date toRet = mRecurCal.getTime();

            mRecurCal.add(Calendar.DAY_OF_YEAR, mInterval);

            return toRet;
        }
    }

    /**
     * Iterator that handles the weekly recurrence case
     */
    private static class WeeklyIterator implements Iterator<Date> {
        private final Calendar mRecurCal; // Calendar used to calculate recurrences
        private final boolean[] mDays;    // Days of week to recur on
        private final int mInterval;      // Weeks between recurrences
        private final int mStartDate;     // Starting day of week
        private int mCurrDate;            // Current day of week

        /**
         * Construct a Iterator that generates dates for weekly recurrences
         *
         * @param itemStart Start date to recur on
         * @param interval How many weeks between recurrences
         */
        public WeeklyIterator(Date itemStart, boolean[] days, int interval) {
            mRecurCal = Calendar.getInstance();
            mRecurCal.setTime(itemStart);

            mCurrDate = mRecurCal.get(Calendar.DAY_OF_WEEK);
            mStartDate = mRecurCal.get(Calendar.DAY_OF_WEEK);

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
        public Date next() {
            Date toRet = mRecurCal.getTime();

            if (!mDays[mCurrDate - 1]) {
                toRet = null;
            }

            mRecurCal.add(Calendar.DAY_OF_YEAR, 1);

            mCurrDate = mCurrDate + 1 % 7;

            if (mCurrDate == mStartDate) {
                mRecurCal.add(Calendar.WEEK_OF_YEAR, mInterval - 1);
            }

            return toRet == null ? next() : toRet;
        }
    }

    /**
     * Iterator that handles the monthly static case, where it's repeated monthly on the same date.
     */
    private static class MonthlyStaticIterator implements Iterator<Date> {
        private final Calendar mRecurCal; // Calendar used to calculate recurrences
        private final int mInterval;      // Number of months between recurrences

        /**
         * Construct a Iterator that generates dates for monthly static recurrences
         *
         * @param itemStart Start date to recur on
         * @param interval How many months between recurrences
         */
        public MonthlyStaticIterator(Date itemStart, int interval) {
            mRecurCal = Calendar.getInstance();
            mRecurCal.setTime(itemStart);

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
        public Date next() {
            Date toRet = mRecurCal.getTime();

            mRecurCal.add(Calendar.MONTH, mInterval);

            return toRet;
        }
    }

    /**
     * Iterator that handles the case where it recurs monthly on the same dynamic date (e.g. 3rd
     * Monday)
     */
    private static class MonthlyDynamicIterator implements Iterator<Date> {
        private final Calendar mRecurCal;    // Calendar used to calculate recurrences
        private final int mInterval;         // Months between recurrences
        private final int mDayOfWeek;        // Starting day of week
        private final int mDayOfWeekInMonth; // Starting day of week in month ("3rd" in 3rd Monday)

        /**
         * Construct a Iterator that generates dates for monthly dynamic recurrences
         *
         * @param itemStart Start date to recur on
         * @param interval How many months between recurrences
         */
        public MonthlyDynamicIterator(Date itemStart, int interval) {
            mRecurCal = Calendar.getInstance();
            mRecurCal.setTime(itemStart);

            mDayOfWeek = mRecurCal.get(Calendar.DAY_OF_WEEK);
            mDayOfWeekInMonth = mRecurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

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
        public Date next() {
            Date toRet = mRecurCal.getTime();

            mRecurCal.add(Calendar.MONTH, mInterval);
            mRecurCal.set(Calendar.DAY_OF_WEEK, mDayOfWeek);
            mRecurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, mDayOfWeekInMonth);

            return toRet;
        }
    }

    /**
     * Iterator that handles the monthly specific case, where item recurs monthly on multiple
     * specific dates
     */
    private static class MonthlySpecificIterator implements Iterator<Date> {
        private final Calendar mRecurCal;  // Calendar used to calculate recurrences
        private final List<Integer> mDays; // List of days to recur on
        private final int mInterval;       // Number of months between recurrences
        private int mCurrDay;              // Current day of month

        /**
         * Construct a Iterator that generates dates for monthly specific recurrences
         *
         * @param itemStart Start date to recur on
         * @param interval How many months between recurrences
         */
        public MonthlySpecificIterator(Date itemStart, String[] days, int interval) {
            mRecurCal = Calendar.getInstance();
            mRecurCal.setTime(itemStart);

            mCurrDay = mRecurCal.get(Calendar.DAY_OF_MONTH);

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
        public Date next() {
            Date toRet = mRecurCal.getTime();

            // keep track of this so it's easier to see when month has been flipped
            int oldMonth = mRecurCal.get(Calendar.MONTH);

            if (!mDays.contains(mCurrDay)) {
                toRet = null;
            }

            mRecurCal.add(Calendar.DAY_OF_YEAR, 1);
            mCurrDay = mRecurCal.get(Calendar.DAY_OF_MONTH);

            if (oldMonth != mRecurCal.get(Calendar.MONTH)) {
                mRecurCal.add(Calendar.MONTH, mInterval - 1);
            }

            return toRet == null ? next() : toRet;
        }
    }

    /**
     * Iterator that handles the case where item recurs on same month/date yearly
     */
    private static class YearlyStaticIterator implements Iterator<Date> {
        private final Calendar mRecurCal; // Calendar used to calculate recurrence
        private final int mInterval;      // Number of years between items

        /**
         * Construct a Iterator that generates dates for yearly static recurrences
         *
         * @param itemStart Start date to recur on
         * @param interval How many years between recurrences
         */
        public YearlyStaticIterator(Date itemStart, int interval) {
            mRecurCal = Calendar.getInstance();
            mRecurCal.setTime(itemStart);

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
        public Date next() {
            Date toRet = mRecurCal.getTime();
            mRecurCal.add(Calendar.YEAR, mInterval);
            return toRet;
        }
    }

    /**
     * Iterator that handles the case where item occurs yearly on the same dynamic date (e.g. 3rd
     * Monday)
     */
    private static class YearlyDynamicIterator implements Iterator<Date> {
        private final Calendar mRecurCal;    // Calendar used to calculate recurrence
        private final int mInterval;         // Number of years between recurrences
        private final int mDayOfWeek;        // Starting day of week
        private final int mDayOfWeekInMonth; // Starting day of week in month ("3rd" in 3rd Monday)

        /**
         * Construct a Iterator that generates dates for yearly dynamic recurrences
         *
         * @param itemStart Start date to recur on
         * @param interval How many years between recurrences
         */
        public YearlyDynamicIterator(Date itemStart, int interval) {
            mRecurCal = Calendar.getInstance();
            mRecurCal.setTime(itemStart);
            mDayOfWeek = mRecurCal.get(Calendar.DAY_OF_WEEK);
            mDayOfWeekInMonth = mRecurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

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
        public Date next() {
            Date toRet = mRecurCal.getTime();

            // Calculate next date
            mRecurCal.add(Calendar.YEAR, mInterval);
            mRecurCal.set(Calendar.DAY_OF_WEEK, mDayOfWeek);
            mRecurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, mDayOfWeekInMonth);

            return toRet;
        }
    }

    /**
     * Iterator that handles the case where the item occurs on multiple months on the same date
     * yearly.
     */
    private static class YearlyMultipleStaticIterator implements Iterator<Date> {
        private final Calendar mRecurCal; // Calendar used to calculate recurrence
        private final boolean[] mMonths;  // Months to recur on
        private final int mInterval;      // Years between recurrences
        private final int mStartMonth;    // Month of start date
        private int mCurrMonth;           // Current month

        /**
         * Construct a Iterator that generates dates for yearly multiple static recurrences
         *
         * @param itemStart Start date to recur on
         * @param interval How many years between recurrences
         */
        public YearlyMultipleStaticIterator(Date itemStart, boolean[] months, int interval) {
            mRecurCal = Calendar.getInstance();
            mRecurCal.setTime(itemStart);

            mMonths = months;
            mInterval = interval;
            mCurrMonth = mRecurCal.get(Calendar.MONTH);
            mStartMonth = mRecurCal.get(Calendar.MONTH);
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
        public Date next() {
            Date toRet = mRecurCal.getTime();

            if (!mMonths[mCurrMonth - 1]) {
                toRet = null;
            }

            mRecurCal.add(Calendar.MONTH,1);
            mCurrMonth = mRecurCal.get(Calendar.MONTH);

            if (mCurrMonth == mStartMonth) {
                mRecurCal.add(Calendar.YEAR, mInterval - 1);
            }

            return toRet == null ? next() : toRet;
        }
    }

    /**
     * Iterator that handles the case where the item recurs on multiple months on the same dynamic
     * date (e.g. 3rd Monday)
     */
    private static class YearlyMultipleDynamicIterator implements Iterator<Date> {
        private final Calendar mRecurCal;    // Calendar used to calculate recurrence
        private final boolean[] mMonths;     // Months to recur on
        private final int mInterval;         // Number of years between recurrences
        private final int mStartMonth;       // Start date's month
        private final int mDayOfWeek;        // Start date's day of week
        private final int mDayOfWeekInMonth; // Day of week in month of start date (3rd in 3rd Mon)
        private int mCurrMonth;              // Current month

        /**
         * Construct a Iterator that generates dates for yearly multiple dynamic recurrences
         *
         * @param itemStart Start date to recur on
         * @param interval How many years between recurrences
         */
        public YearlyMultipleDynamicIterator(Date itemStart, boolean[] months, int interval) {
            this.mRecurCal = Calendar.getInstance();
            mRecurCal.setTime(itemStart);

            this.mMonths = months;
            this.mInterval = interval;
            mCurrMonth = mRecurCal.get(Calendar.MONTH);
            mStartMonth = mRecurCal.get(Calendar.MONTH);
            mDayOfWeek = mRecurCal.get(Calendar.DAY_OF_WEEK);
            mDayOfWeekInMonth = mRecurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
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
        public Date next() {
            Date toRet = mRecurCal.getTime();

            if (!mMonths[mCurrMonth]) {
                toRet = null;
            }

            mRecurCal.add(Calendar.MONTH, 1);
            mCurrMonth = mRecurCal.get(Calendar.MONTH);

            if (mCurrMonth == mStartMonth) {
                mRecurCal.add(Calendar.YEAR, mInterval - 1);
            }

            mRecurCal.set(Calendar.DAY_OF_WEEK, mDayOfWeek);
            mRecurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, mDayOfWeekInMonth);

            return toRet == null ? next() : toRet;
        }
    }

    /**
     * Iterator that handles the case where the item recurs on specific months/dates yearly
     */
    private static class YearlySpecificIterator implements Iterator<Date> {
        private final Calendar mRecurCal;  // Calendar used to calculate recurrence
        private final boolean[] mMonths;   // Months to recur on
        private final List<Integer> mDays; // Days to recur on
        private final int mInterval;       // Number of years between recurrences
        private final int mStartMonth;     // Month of start date
        private final int mStartDay;       // Day in month of start date
        private int mCurrMonth;            // Current month
        private int mCurrDay;              // Current date

        /**
         * Construct a Iterator that generates dates for yearly dynamic recurrences
         *
         * @param itemStart Start date to recur on
         * @param interval How many years between recurrences
         */
        public YearlySpecificIterator(Date itemStart, boolean[] months, String[] days, int interval) {
            mRecurCal = Calendar.getInstance();
            mRecurCal.setTime(itemStart);

            mCurrMonth = mRecurCal.get(Calendar.MONTH);
            mCurrDay = mRecurCal.get(Calendar.DAY_OF_MONTH);
            mStartMonth = mRecurCal.get(Calendar.MONTH);
            mStartDay = mRecurCal.get(Calendar.DAY_OF_MONTH);

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
        public Date next() {
            Date toRet = mRecurCal.getTime();

            if (!mMonths[mCurrMonth - 1] || !mDays.contains(mCurrDay)) {
                toRet = null;
            }

            mRecurCal.add(Calendar.DAY_OF_YEAR, 1);
            mCurrMonth = mRecurCal.get(Calendar.MONTH);
            mCurrDay = mRecurCal.get(Calendar.DAY_OF_MONTH);

            if (mCurrMonth == mStartMonth && mCurrDay == mStartDay) {
                mRecurCal.add(Calendar.YEAR, mInterval - 1);
            }

            return toRet == null ? next() : toRet;
        }
    }
}
