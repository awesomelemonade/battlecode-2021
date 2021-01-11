package latticebot;

import battlecode.common.*;
import latticebot.util.Cache;
import latticebot.util.Constants;
import latticebot.util.MapInfo;
import latticebot.util.Pathfinder;
import latticebot.util.Util;
import java.util.function.Predicate;

public strictfp class Muckracker implements RunnableBot {
    private RobotController rc;
    private boolean explore;

    public Muckracker(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        if (Math.random() > 0.5) {
            explore = true;
        } else {
            explore = false;
        }
    }

    @Override
    public void turn() throws GameActionException {
        if (rc.isReady()) {
            if (tryExpose()) {
                return;
            }
            if (campEnemyEC()) {
                return;
            }
            Predicate<RobotInfo> exposable = robot -> robot.type.canBeExposed();
            RobotInfo enemy = Util.getClosestEnemyRobot(exposable);
            if (enemy == null) {
                if (explore || !goToNearestEC()) {
                    Util.smartExplore();
                }
            } else {
                Pathfinder.execute(enemy.getLocation());
            }
        }
    }

    public boolean goToNearestEC() throws GameActionException {
        MapLocation ec = Util.getFirst(
                () -> Util.mapToLocation(Util.getClosestEnemyRobot(x -> x.getType() == RobotType.ENLIGHTENMENT_CENTER)),
                () -> MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getClosestLocation(Cache.MY_LOCATION)
        );
        if (ec != null) {
            rc.setIndicatorDot(ec, 255, 255, 0); // yellow
            Pathfinder.execute(ec);
            return true;
        } else {
            return false;
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

    public boolean goToEnemyEC() throws GameActionException {
        MapLocation closest = Util.closestEnemyEC();
        if(closest == null) return false;
        return Util.tryMove(closest);
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
