package latticebot2;

import battlecode.common.*;
import latticebot2.util.Cache;
import latticebot2.util.Constants;
import latticebot2.util.Pathfinder;
import latticebot2.util.Util;
import java.util.function.Predicate;

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
        if (rc.isReady()) {
            if (tryExpose())
                return;
            if (campEnemyEC())
                return;
            Predicate<RobotInfo> exposable = robot -> robot.type.canBeExposed();
            RobotInfo enemy = Util.getClosestEnemyRobot(exposable);
            if (enemy == null) {
                Util.smartExplore();
            } else {
                Pathfinder.execute(enemy.getLocation());
            }
        }
    }

    public boolean tryExpose() throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        int best_influence = 0;
        MapLocation best_location = null;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, Constants.ENEMY_TEAM)) {
            if (robot.type.canBeExposed() && rc.canExpose(robot.location)) {
                if (robot.influence > best_influence) {
                    best_influence = robot.influence;
                    best_location = robot.location;
                }
            }
        }
        if (best_location != null) {
            rc.expose(best_location);
            return true;
        }
        return false;
    }

    public boolean campEnemyEC() throws GameActionException {
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                if (rc.getLocation().distanceSquaredTo(robot.location) <= 2) {
                    return true;
                }
                Pathfinder.execute(robot.getLocation());
                return true;
            }
        }
        return false;
    }
}
