package sk.tuke.smart.makac.helpers;

import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import sk.tuke.smart.makac.model.GpsPoint;

public final class MainHelper {
    private static final float MpS_TO_MIpH = 2.23694f;
    private static final float KM_TO_MI = 0.62137119223734f;
    private static final float MINpKM_TO_MINpMI = 1.609344f;

    /**
     * return string of time in format HH:MM:SS
     * @param time - in seconds
     */
    public static String formatDuration(long time) {
        long seconds = time % 60;
        time -= seconds;

        long minutesCount = time / 60;
        long minutes = minutesCount % 60;
        minutesCount -= minutes;

        long hoursCount = minutesCount / 60;

        return hoursCount + ":" + evaluateZero(minutes) + minutes + ":" + evaluateZero(seconds) + seconds;
    }

    private static String evaluateZero(long timeCount) {
        if (timeCount < 10)
            return "0";
        return "";
    }

    /**
     * convert m to km and round to 2 decimal places and return as string
     */
    public static String formatDistance(double n) {
        return String.valueOf(Math.round(n / 10.0) / 100.0);
    }

    /**
     * convert m to miles and round to 2 decimal places and return as string
     */
    public static String formatDistanceMiles(double n) {
        double kilometers = n / 1000;
        double miles = kilometers * KM_TO_MI;
        return String.valueOf(Math.round(miles * 100) / 100);
    }

    /**
     * round number to 2 decimal places and return as string
     */
    public static String formatPace(double n) {
        if (n == 0.0)
            return "00:00";
        int MM = (int) (n / 60);
        int SS = (int) Math.round(n % 60);
        return (String.format("%02d:%02d", MM, SS));
    }

    /**
     * round number to integer
     */
    public static String formatCalories(double n) {
        return String.valueOf((int) Math.round(n));
    }

    /* convert km to mi (multiply with corresponding constant) */
    public static double kmToMi(double n) {
        return n * KM_TO_MI;
    }

    /* convert m/s to mi/h (multiply with corresponding constant) */
    public static double mpsToMiph(double n) {
        return n * MpS_TO_MIpH;
    }

    /* convert min/km to min/mi (multiply with corresponding constant) */
    public static double minpkmToMinpmi(double n) {
        return n * MINpKM_TO_MINpMI;
    }

    /* convert kmph to miph (multiply with corresponding constant) */
    public static double kmphToMiph(double n) {
        return n * MINpKM_TO_MINpMI;
    }

    public static long msToS(long n) {
        return n / 1000;
    }

    public static String sToDate(long n) {
        return new SimpleDateFormat("HH:mm - dd.MM. yyyy", Locale.ENGLISH).format(n);
    }

    public static ArrayList<List<Location>> getFinalPositionList(List<GpsPoint> gpsPoints) {
        long index = 1;
        long currentSessionNumber;
        ArrayList<List<Location>> finalPositionList = new ArrayList<>();
        ArrayList<Location> latestPositionList = new ArrayList<>();
        Location currentLocation;

        for (GpsPoint currentGpsPoint : gpsPoints) {
            currentLocation = new Location("");
            currentSessionNumber = currentGpsPoint.getSessionNumber();
            if (index != currentSessionNumber) {
                index = currentSessionNumber;
                latestPositionList = new ArrayList<>();
                finalPositionList.add(latestPositionList);
            }
            currentLocation.setLongitude(currentGpsPoint.getLongitude());
            currentLocation.setLatitude(currentGpsPoint.getLatitude());
            latestPositionList.add(currentLocation);
        }
        finalPositionList.add(latestPositionList);
        return finalPositionList;
    }
}
