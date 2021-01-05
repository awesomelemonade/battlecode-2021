package latticebot.util;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Cache { // Cache variables that are constant throughout a turn
    private static RobotController rc;
    public static RobotInfo[] ALL_ROBOTS, ALLY_ROBOTS, ENEMY_ROBOTS;
    private static final RobotInfo[] EMPTY_ROBOTS = {};
    public static int TURN_COUNT;
    public static void init(RobotController rc) {
        Cache.rc = rc;
        TURN_COUNT = 0;
    }
    public static void loop() {
        ALL_ROBOTS = rc.senseNearbyRobots();
        if (ALL_ROBOTS.length > 0) {
            ALLY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ALLY_TEAM);
            ENEMY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ENEMY_TEAM);
        } else {
            ALLY_ROBOTS = EMPTY_ROBOTS;
            ENEMY_ROBOTS = EMPTY_ROBOTS;
        }
        TURN_COUNT++;
    }
}
