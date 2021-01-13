package latticebot;

import battlecode.common.*;
import latticebot.util.Cache;
import latticebot.util.Constants;
import latticebot.util.MapInfo;
import latticebot.util.Pathfinder;
import latticebot.util.Util;

public strictfp class Muckraker implements RunnableBot {
    private static RobotController rc;
    private static boolean explore;

    public Muckraker(RobotController rc) {
        Muckraker.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        if (Math.random() > 0.8) {
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
            RobotInfo enemy = Util.getClosestEnemyRobot(r -> r.getType().canBeExposed());
            if (enemy != null) {
                Pathfinder.execute(enemy.getLocation());
                return;
            }
            if (goToCommunicatedSlanderers()) {
                return;
            }
            if (!explore) {
                if (tryECSpiral()) {
                    return;
                }
            }
            Util.randomExplore();
        }
    }

    public static boolean goToCommunicatedSlanderers() {
        return MapInfo.enemySlandererLocations.getClosestLocation().map(enemy -> {
            Pathfinder.execute(enemy);
            return true;
        }).orElse(false);
    }

    private static MapLocation lastECVisited = null;
    public static boolean tryECSpiral() {
        return MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getClosestLocation(Cache.MY_LOCATION).map(ec -> {
            int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(ec);
            if (distanceSquared <= 9) {
                lastECVisited = ec;
            }
            if (lastECVisited == null || !lastECVisited.equals(ec)) {
                if (!Pathfinder.executeSpacedApart(ec)) {
                    Pathfinder.execute(ec);
                }
            } else {
                Direction tangent = ec.directionTo(Cache.MY_LOCATION).rotateRight().rotateRight();
                Util.tryMoveTowardsSpacedApart(tangent);
            }
            return true;
        }).orElse(false);
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
}
