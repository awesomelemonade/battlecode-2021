package ppbot.util;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Cache { // Cache variables that are constant throughout a turn
    private static RobotController rc;
    public static RobotInfo[] ENEMY_ROBOTS;
    public static void init(RobotController rc) {
        Cache.rc = rc;
    }
    public static void loop() {
        ENEMY_ROBOTS = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, Constants.OPPONENT_TEAM);
    }
}
