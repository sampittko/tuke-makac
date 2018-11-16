package sk.tuke.smart.makac.helpers;

public final class MainHelper {
    private static final float MpS_TO_MIpH = 2.23694f;
    private static final float KM_TO_MI = 0.62137119223734f;
    private static final float MINpKM_TO_MINpMI = 1.609344f;

    /**
     * return string of time in format HH:MM:SS
     * @param time - in seconds
     */
    public static String formatDuration(long time) {
        long seconds = time %60;
        time -= seconds;
        String zeroSeconds = evaluateZero(seconds);

        long minutesCount = time / 60;
        long minutes = minutesCount % 60;
        minutesCount -= minutes;
        String zeroMinutes = evaluateZero(minutes);

        long hoursCount = minutesCount / 60;
        String zeroHours = evaluateZero(hoursCount);

        return "" + zeroHours + hoursCount + ":" + zeroMinutes + minutes + ":" + zeroSeconds + seconds;
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
        double km = n / 1000;
        return String.valueOf(Math.round(km * 100) / 100) + "." + String.valueOf(String.valueOf(Math.round(km * 100) % 100));
    }

    /**
     * round number to 2 decimal places and return as string
     */
    public static String formatPace(double n) {
        return String.valueOf(Math.round(n * 100) / 100) + "." + String.valueOf(String.valueOf(Math.round(n * 100) % 100));
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
