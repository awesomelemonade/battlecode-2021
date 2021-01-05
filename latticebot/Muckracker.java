package latticebot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import latticebot.util.Constants;
import latticebot.util.Util;
import ppbot.util.Pathfinder;

public strictfp class Muckracker implements RunnableBot {
    private RobotController rc;

    public Muckracker(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        if (tryExpose()) {
            return;
        }
        RobotInfo enemy = Util.getClosestEnemyRobot();
        if (enemy == null) {
            Util.randomExplore();
        } else {
            Pathfinder.execute(enemy.getLocation());
        }
    }

    public boolean tryExpose() throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, Constants.ENEMY_TEAM)) {
            if (robot.type.canBeExposed() && rc.canExpose(robot.location)) {
                rc.expose(robot.location);
                return true;
            }
        }
        return false;
    }
}
