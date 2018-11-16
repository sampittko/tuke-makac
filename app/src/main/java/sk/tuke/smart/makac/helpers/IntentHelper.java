package sk.tuke.smart.makac.helpers;

public final class IntentHelper {
    public static final String ACTION_START = "sk.tuke.smart.makac.COMMAND_START";
    public static final String ACTION_PAUSE = "sk.tuke.smart.makac.COMMAND_PAUSE";
    public static final String ACTION_CONTINUE = "sk.tuke.smart.makac.COMMAND_CONTINUE";
    public static final String ACTION_STOP = "sk.tuke.smart.makac.COMMAND_STOP";
    public static final String ACTION_TICK = "sk.tuke.smart.makac.TICK";
    public static final String ACTION_GPS = "sk.tuke.smart.makac.COMMAND_GPS";

    public static final String DATA_SPORT = "SPORT";
    public static final String DATA_DURATION = "DURATION";
    public static final String DATA_DISTANCE = "DISTANCE";
    public static final String DATA_PACE = "PACE";
    public static final String DATA_CALORIES = "CALORIES";
    public static final String DATA_POSITIONS = "POSITIONS";
    public static final String DATA_STATE = "STATE";

    public static final int STATE_STOPPED = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_PAUSED = 3;
    public static final int STATE_CONTINUE = 4;

    public static final int ACTIVITY_RUNNING = 0;
    public static final int ACTIVITY_WALKING = 1;
    public static final int ACTIVITY_CYCLING = 2;
}
