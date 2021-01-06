package ppbot.util;

import battlecode.common.*;

public class Cache { // Cache variables that are constant throughout a turn
    private static RobotController rc;
    public static RobotInfo[] ALLY_ROBOTS, ENEMY_ROBOTS;
    public static int TURN_COUNT;
    public static MapLocation MY_LOCATION;
    public static void init(RobotController rc) {
        Cache.rc = rc;
        TURN_COUNT = 0;
    }
    public static void loop() {
        RobotInfo[] sensed_robots = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
        int num_allies = 0, num_enemies = 0;
        for (RobotInfo robot: sensed_robots) {
            if(robot.getTeam() == Constants.ALLY_TEAM) {
                num_allies++;
            } else {
                num_enemies++;
            }
        }
        ALLY_ROBOTS = new RobotInfo[num_allies];
        ENEMY_ROBOTS = new RobotInfo[num_enemies];
        int ally_ptr = 0, enemy_ptr = 0;
        for (RobotInfo robot: sensed_robots) {
            if(robot.getTeam() == Constants.ALLY_TEAM) {
                ALLY_ROBOTS[ally_ptr++] = robot;
            } else {
                ENEMY_ROBOTS[enemy_ptr++] = robot;
            }
        }
        
        TURN_COUNT++;
        MY_LOCATION = rc.getLocation();
    }
}
