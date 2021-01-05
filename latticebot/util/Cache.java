package latticebot.util;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Cache { // Cache variables that are constant throughout a turn
    private static RobotController rc;
    public static RobotInfo[] ALLY_ROBOTS, ENEMY_ROBOTS;
    public static int TURN_COUNT;
    public static void init(RobotController rc) {
        Cache.rc = rc;
        TURN_COUNT = 0;
    }
    public static void loop() {
        ALLY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ALLY_TEAM);
        ENEMY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ENEMY_TEAM);
        TURN_COUNT++;
    }
}
