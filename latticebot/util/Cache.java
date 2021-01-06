package latticebot.util;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Cache { // Cache variables that are constant throughout a turn
    private static RobotController rc;
    public static RobotInfo[] ALL_ROBOTS, ENEMY_ROBOTS;
    public static int TURN_COUNT;
    public static void init(RobotController rc) {
        Cache.rc = rc;
        TURN_COUNT = 0;
    }
    public static void loop() {
        if (rc.getType() == RobotType.POLITICIAN) {
            // Bytecode Optimization: Only Politicians need all robots
            ALL_ROBOTS = rc.senseNearbyRobots();
        }
        ENEMY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ENEMY_TEAM);
        TURN_COUNT++;
    }
}
