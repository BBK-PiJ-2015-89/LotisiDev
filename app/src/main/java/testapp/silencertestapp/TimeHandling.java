package testapp.silencertestapp;

/**
 * Created by graemewilkinson on 29/09/2017.
 */

public class TimeHandling {

    /**
     * The time value is converted into human readable time, for example 739 is converted to 07:39 by
     * changing the int to a string and manipulating the value using a stringbuilder.
     *
     * @param combinedTime int e.g. 37
     * @return String e.g. 00:37
     */
    static String createFancyTime(int combinedTime) {

        StringBuilder tempString = new StringBuilder(Integer.toString(combinedTime));
        if (tempString.length() == 4) {
            tempString = tempString.insert(2, ":");
        } else if (tempString.length() == 3) {
            tempString = tempString.insert(1, ":");
            tempString = tempString.insert(0, "0");
        } else if (tempString.length() == 2) {
            tempString = tempString.insert(0, "00:");
        } else if (tempString.length() == 1) {
            tempString = tempString.insert(0, "00:0");
        }
        return tempString.toString();
    }

    /**
     * Takes a full time value from the database, such as 23:59 and returns the minute portion.
     * @param minuteCombined String time, such as "23:59"
     * @return minutes as an int, such as 59
     */
    static int getMinute(String minuteCombined) {
        minuteCombined = Integer.toString(dismantleFancyTime(minuteCombined));
        int IntMinute;
        if (minuteCombined.length() == 4) {
            IntMinute = Integer.parseInt(minuteCombined.substring(2, 4));
        } else if (minuteCombined.length() == 3) {
            IntMinute = Integer.parseInt(minuteCombined.substring(1, 3));
        } else if (minuteCombined.length() == 2) {

            IntMinute = Integer.parseInt(minuteCombined.substring(0, 2));
        } else {

            IntMinute = Integer.parseInt(minuteCombined);
        }
        return IntMinute;
    }


    /**
     * Takes standard time formatting from the database in the format of "00:00" and returns the hour value from 0-23.
     * @param hourCombined e.g. "23:56"
     * @return hour subsection from hourCombined e.g. int 23.
     */
    static int getHour(String hourCombined) {
        hourCombined = Integer.toString(dismantleFancyTime(hourCombined));
        int intHour;
        if ((hourCombined.length() == 4)) {
            intHour = Integer.parseInt(hourCombined.substring(0, 2));

        } else if (hourCombined.length() == 3) {
            intHour = Integer.parseInt(hourCombined.substring(0, 1));

        } else if (hourCombined.length() == 2) {
            intHour = 0;

        } else {
            intHour = 0;

        }
        return intHour;
    }

    /**
     * Dismantles a human readable time and coverts it to a java readable time by removing the ':'.
     * @param combinedTime String time e.g. 23:59
     * @return int time e.g. 2359
     */
    static int dismantleFancyTime(String combinedTime) {
        return Integer.parseInt(combinedTime.replace(":", ""));
    }

}
