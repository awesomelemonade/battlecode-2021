package latticebot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import latticebot.util.Pathfinder;
import latticebot.util.Util;

public strictfp class Politician implements RunnableBot {
    private RobotController rc;

    public Politician(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        RobotInfo closestEnemy = Util.getClosestEnemyRobot();
        if (closestEnemy == null) {
            Util.randomExplore();
        } else {
            int enemyDistanceSquared = rc.getLocation().distanceSquaredTo(closestEnemy.getLocation());
            int actionRadiusSquared = rc.getType().actionRadiusSquared;
            if (enemyDistanceSquared <= actionRadiusSquared && rc.canEmpower(enemyDistanceSquared)) {
                if (rc.senseNearbyRobots(enemyDistanceSquared, null).length == 1) {
                    rc.empower(enemyDistanceSquared);
                }
            } else {
                Pathfinder.execute(closestEnemy.getLocation());
            }
        }
    }
}
