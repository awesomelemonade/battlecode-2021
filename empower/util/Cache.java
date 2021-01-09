package empower.util;

import battlecode.common.*;

public class Cache { // Cache variables that are constant throughout a turn
    private static RobotController rc;
    public static RobotInfo[] ALL_ROBOTS, ENEMY_ROBOTS;
    public static int TURN_COUNT;
    public static MapLocation MY_LOCATION;
    public static int[] explored;
    public static int mapMinX = -1; // -1 if not known
    public static int mapMaxX = -1; // -1 if not known
    public static int mapMinY = -1; // -1 if not known
    public static int mapMaxY = -1; // -1 if not known
    public static boolean sentMapMinX = false;
    public static boolean sentMapMaxX = false;
    public static boolean sentMapMinY = false;
    public static boolean sentMapMaxY = false;
    public static MapLocation[] enemyECs = new MapLocation[12]; // max 12 ECs in the game
    public static int enemyECCount = 0;
    public static MapLocation[] neutralECs = new MapLocation[12]; // max 12 ECs in the game
    public static int neutralECCount = 0;
    public static MapLocation[] tempECs = new MapLocation[12]; // max 12 ECs in the game

    public static void init(RobotController rc) {
        Cache.rc = rc;
        TURN_COUNT = 0;
        explored = new int[32];
    }

    public static void loop() {
        ALL_ROBOTS = rc.senseNearbyRobots();
        ENEMY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ENEMY_TEAM);
        TURN_COUNT++;
        MY_LOCATION = rc.getLocation();
        Util.setExplored(MY_LOCATION);
    }
}
