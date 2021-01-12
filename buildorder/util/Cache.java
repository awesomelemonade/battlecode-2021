package buildorder.util;

import battlecode.common.*;

public class Cache { // Cache variables that are constant throughout a turn
    private static RobotController rc;
    public static RobotInfo[] ALL_ROBOTS, ALLY_ROBOTS, ENEMY_ROBOTS;
    public static RobotInfo[] EMPTY_ROBOTS = {};
    public static int TURN_COUNT;
    public static MapLocation MY_LOCATION;

    public static Direction lastDirection;
    public static void init(RobotController rc) {
        Cache.rc = rc;
        TURN_COUNT = 0;
    }

    public static void loop() {
        lastDirection = Direction.CENTER;
        ALL_ROBOTS = rc.senseNearbyRobots();
        if (ALL_ROBOTS.length == 0) {
            // save 200 bytecodes
            ALLY_ROBOTS = EMPTY_ROBOTS;
            ENEMY_ROBOTS = EMPTY_ROBOTS;
        } else {
            ALLY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ALLY_TEAM);
            ENEMY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ENEMY_TEAM);
        }
        TURN_COUNT++;
        MY_LOCATION = rc.getLocation();
    }
}
