package com.evanv.taskapp;

/**
 * Represents a Date and/or a time, simpler than Java DateTime object for this project
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
public class MyTime {
    private final long datetime; // The number of minutes past 1/1/1970 this MyTime represents

    /**
     * Initializes an immutable representation of Date/Time, where Time is not needed
     *
     * @param month The month of the year (1-12)
     * @param day The day in the year (1-31)
     * @param year The year (Gregorian calendar) formatted like 2022 (must be >=1970 AD)
     */
    public MyTime(int month, int day, int year) {
        // Use more complicated constructor for ease of code reuse
        this(month, day, year, 0, 0);
    }

    /**
     * Initializes an immutable representation of Date/Time based on the given long representation
     *
     * @param datetime A long representation of datetime, based on another MyTime's GetDateTime()
     */
    public MyTime(long datetime) {
        this.datetime = datetime;
    }

    /**
     * Initializes an immutable representation of Date/Time, where Time is needed
     *
     * @param month The month of the year (1-12)
     * @param day The day in the year (1-31)
     * @param year The year (Gregorian calendar) formatted like 2022 (must be >=1970 AD)
     * @param hour The hour of the time (24-hour clock where 0 is midnight)
     * @param minute The minute of the time
     */
    public MyTime(int month, int day, int year, int hour, int minute) {
        // Sets date to 0, which will be added to within the function.
        long date = 0;

        // Calculates the year. Algorithm is very simple, the number of leap years since 1970 until
        // the given year is the number of years divisible by 4, minus the number of years divisible
        // by 100 (as there was no leap year then), plus the number of years divisible by 400 (as
        // that is the third exception. Finally, the number of days since 01/01/1970 until
        // 01/01/year is calculated by taking 365 * the number of "normal" years and adding it to
        // the number of leap years (as each one only adds a single extra day)
        int yearDiff = year-1970;
        int numLeapYears = ((yearDiff + 2) / 4) - ((yearDiff + 70)/100) + ((yearDiff+370)/400);
        date += (365L * yearDiff) + numLeapYears;

        // True if year is a leap year or false if it's not.
        boolean leap = (year % 4 == 0) && (year % 400 == 0 || year % 100 != 0);
        // If it's currently a leap year, than our date calculation below already accounts for this
        // added day. This is also necessary to handle dates from 1/1-2/28 properly.
        date -= leap ? 1 : 0;

        // Calculates the date. Idea is fairly simple, without breaks, switch statements fall
        // through to the next one. Therefore, by not including any breaks until the default case,
        // we can use this for all months. E.g. if month is April, we add the number of days for
        // January (case 2), February (case 3), March (case 4), and what day it is in April
        // (default). The reason the cases are monthNum+1 is that we're calculating how many days
        // came *before* the first day in the month, so we should start with the number of days in
        // the prior month.
        switch (month) {
            case 12:
                date += 30;
            case 11:
                date += 31;
            case 10:
                date += 30;
            case 9:
                date += 31;
            case 8:
                date += 31;
            case 7:
                date += 30;
            case 6:
                date += 31;
            case 5:
                date += 30;
            case 4:
                date += 31;
            case 3:
                date += leap ? 29 : 28;
            case 2:
                date += 31;
            default:
                // As datetime is number of minutes since 1/1/1970, it follows that we need to
                // subtract 1 from date to ensure that this holds.
                date += day - 1;
                break;
        }

        // Calculates datetime (number of minutes since 1/1/1970. date is number of days since 1970,
        // so it follows that we should multiply it by 1440 (number of minutes in a day).
        long time = (short) ((60 * hour) + minute);
        datetime = (date * 1440) + time;
    }

    /**
     * Returns either the year of the object (yearRemLeap == 0), the number of days past
     * the first day of the year of the object (yearRemLeap == 1), or if the year is a
     * leap year or not (yearRemLeap == 2)
     *
     * @param yearRemLeap if 0: returns year, if 1: returns day, if 2: returns 0 if not leap year, 1
     *                    if leap year
     * @return See yearRemLeap parameter
     */
    private short yearHelper(short yearRemLeap) {
        int date = (int)(datetime/1440);

        // The idea behind this is to create a rough estimate for the year, and then adjust it if
        // it's off due to leap years.
        int estimate = date/365;
        boolean leap = ((estimate+2) % 4 == 0) && ((estimate+370) % 400 == 0 ||
                (estimate+70) % 100 != 0);

        // Note about the - (leap ? 1 : 0): if it's a leap year, the addition term will add an extra
        // day to it, however this is incorrect, as the extra day doesn't occur until 2/29, so we
        // subtract a day off to account for this
        int estimateInDays = (estimate * 365) + ((estimate + 2)/4) - ((estimate + 70)/100) +
                ((estimate+370)/400) - (leap ? 1 : 0);

        // If our estimate overshoots it, we adjust it to the correct value, being off by as many
        // years as "amountOff".
        if (estimateInDays > date) {
            int amountOff = (365 + estimateInDays-date)/365;
            estimate -= amountOff;
            leap = ((estimate+2) % 4 == 0) && ((estimate+370) % 400 == 0 ||
                    (estimate+70) % 100 != 0);
            estimateInDays = (estimate * 365) + ((estimate + 2)/4) - ((estimate + 70)/100)
                    + ((estimate+370)/400) - (leap ? 1 : 0);
        }

        // Returns the specific state variable asked for. Kinda gross, but it's well documented and
        // is only used as a private function so it doesn't irk me too much.
        if (yearRemLeap == 0) {
            return (short)(estimate + 1970);
        }
        else if (yearRemLeap == 1) {
            return (short) (date - estimateInDays);
        }
        else {
            return (short) (leap ? 1 : 0);
        }
    }

    /**
     * Gets the year of the object
     *
     * @return The year of the object
     */
    public short getYear() {
        return yearHelper((short) 0);
    }

    /**
     * Returns either the month of the object (monthOrRem == true),
     * or the date in the month (monthOrRem == false). Ex: 4/01 - false: 1, true: 4
     *
     * @param monthOrRem if false: Returns date in month, if true: Returns month
     * @return See monthOrRem parameter
     */
    private short monthHelper(boolean monthOrRem) {
        // Calculates initial state.
        short restDate = yearHelper((short) 1);
        short year = yearHelper((short) 0);
        boolean leapYear = yearHelper((short) 2) == 1;
        short currMonth = 1;
        short nextMonthDays = 31;

        // Uses a loop to calculate month. There's certainly other ways to do this, and while I
        // purposely avoided loops in other date functions, this has a maximum number of iterations
        // of 12, so the maximum number of operations isn't significantly higher than using, for
        // instance, an if-else block, so I decided to take the more elegant (and interesting) approach.
        while (restDate >= nextMonthDays) {
            restDate -= nextMonthDays;
            currMonth++;

            // Ensures nextMonthDays is correct.
            if (currMonth == 2 && leapYear) {
                nextMonthDays = 29;
            }
            else if (currMonth == 2) {
                nextMonthDays = 28;
            }
            else if (currMonth == 3 || currMonth == 5 || currMonth == 7 || currMonth == 8 ||
                    currMonth == 10 || currMonth == 12) {
                nextMonthDays = 31;
            }
            else {
                nextMonthDays = 30;
            }
        }

        // Returns required value.
        if (monthOrRem) {
            return currMonth;
        }
        else {
            return (short) (restDate + 1);
        }
    }

    /**
     * Gets the month of the object
     *
     * @return The month of the object
     */
    public short getMonth() {
        return monthHelper(true);
    }

    /**
     * Gets the date of the object
     *
     * @return The date of the object
     */
    public short getDate() {
        return monthHelper(false);
    }

    /**
     * Gets the hour of the object
     *
     * @return The hour of the object
     */
    public short getHour() {
        return (short) ((datetime % 1440)/60);
    }

    /**
     * Gets the minute of the object
     *
     * @return The minute of the object
     */
    public short getMinute() {
        return (short) ((datetime % 1440) - (getHour() * 60));
    }

    /**
     * Gets the minute of the object
     *
     * @return The minute of the object
     */
    public long getDateTime() {
        return datetime;
    }


}
