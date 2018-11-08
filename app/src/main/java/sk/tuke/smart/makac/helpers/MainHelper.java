package sk.tuke.smart.makac.helpers;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class MainHelper {
    private static final float MpS_TO_MIpH = 2.23694f;
    private static final float KM_TO_MI = 0.62137119223734f;
    private static final float MINpKM_TO_MINpMI = 1.609344f;

    /**
     * return string of time in format HH:MM:SS
     * @param time - in seconds
     */
    public static String formatDuration(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("HH:mm:ss");
        return format.format(date);
    }

    /**
     * convert m to km and round to 2 decimal places and return as string
     */
    public static String formatDistance(double n) {
        double km = n / 1000;
        return String.valueOf(Math.round(km * 100) / 100);
    }

    /**
     * round number to 2 decimal places and return as string
     */
    public static String formatPace(double n) {
        return String.valueOf(Math.round(n * 100) / 100);
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
}
