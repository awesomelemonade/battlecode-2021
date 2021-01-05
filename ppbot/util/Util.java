package ppbot.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

import static ppbot.util.Constants.*;

public class Util {
    private static RobotController controller;
    public static void init(RobotController controller) {
        Util.controller = controller;
        MapInfo.init(controller);
    }
    public static void loop() {
        MapInfo.loop();
    }
    public static RobotType randomSpawnableRobotType() {
        return SPAWNABLE_ROBOTS[(int) (Math.random() * SPAWNABLE_ROBOTS.length)];
    }
    public static boolean tryMove(Direction direction) throws GameActionException {
        if (controller.canMove(direction)) {
            controller.move(direction);
            return true;
        } else {
            return false;
        }
    }
    public static boolean tryRandomMove() throws GameActionException {
        return tryMove(randomMoveDirection());
    }
    /**
     * Usage: Util.random(Constants.CARDINAL_DIRECTIONS)
     * @param directions
     * @return
     */
    public static Direction random(Direction[] directions) {
        return directions[(int) (Math.random() * directions.length)];
    }
    public static Direction randomMoveDirection() {
        return random(ORDINAL_DIRECTIONS);
    }
}
