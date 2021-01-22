package mrush;

import battlecode.common.*;
import mrush.util.Cache;
import mrush.util.Constants;
import mrush.util.MapInfo;
import mrush.util.Pathfinder;
import mrush.util.Util;

public strictfp class Muckraker implements RunnableBot {
    private static RobotController rc;
    private static boolean explore;
    private static boolean targeted;
    private static MapLocation target;

    public Muckraker(RobotController rc) {
        Muckraker.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        targeted = rc.getConviction() > 5;
        if (Math.random() < 0.2 && rc.getConviction() == 1) {
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
            if (targeted && Cache.TURN_COUNT > 100) {
                if (target != null) {
                    // Invalidate bad targets that can be sensed
                    if (rc.canSenseLocation(target)) {
                        RobotInfo robot = rc.senseRobotAtLocation(target);
                        if (robot == null || robot.getType() != RobotType.ENLIGHTENMENT_CENTER || robot.getTeam() != Constants.ENEMY_TEAM) {
                            target = null;
                        }
                    }
                }
                if (target != null) {
                    // Invalidate bad communicated targets
                    if (!MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).contains(target)) {
                        target = null;
                    }
                }
                if (target == null) {
                    target = MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getRandomLocation().orElse(null);
                }
                if (target != null) {
                    rc.setIndicatorDot(target, 0, 255, 0);
                    tryECSpiral(target);
                    return;
                }
            }
            if (!explore) {
                if (tryECSpiral()) {
                    return;
                }
            }
            Util.smartExplore();
        }
    }

    public static boolean goToCommunicatedSlanderers() {
        return MapInfo.enemySlandererLocations.getClosestLocation().map(enemy -> {
            if (enemy == null) {
                return false;
            }
            return Muckraker.findSlanderer(enemy);
        }).orElse(false);
    }

    private static int[] memoryID = new int[10];
    private static double[] memoryX = new double[10];
    private static double[] memoryY = new double[10];
    private static int memoryIdx = 0;

    public static boolean findSlanderer(MapLocation suggestion) {
        int dx = suggestion.x - Cache.MY_LOCATION.x;
        int dy = suggestion.y - Cache.MY_LOCATION.y;
        double mag = Math.sqrt(dx * dx + dy * dy);
        if (!memoryContains(-1)) {
            memoryID[memoryIdx] = -1;
            memoryX[memoryIdx] = ((double)dx / mag * 4);
            memoryY[memoryIdx] = ((double)dy / mag * 4);
            memoryIdx = (memoryIdx + 1) % 10;
        } else {
            for (int i = 0; i < 10; i++) {
                if (memoryID[i] == -1) {
                    memoryX[i] = ((double)dx / mag * 4);
                    memoryY[i] = ((double)dy / mag * 4);
                }
            }
        }
        updateMemory();
        if (!suggestion.equals(MapInfo.enemySlandererLocations.firstLocation)) {
            if (Cache.MY_LOCATION.distanceSquaredTo(suggestion) > 9) {
                return Pathfinder.execute(suggestion);
            }
        }
        if (MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getClosestLocation(Cache.MY_LOCATION).orElse(null) != null) {
            return false;
        }

        double guessX = 0;
        double guessY = 0;
        for (int i = 0; i < 10; i++) {
            guessX += memoryX[i];
            guessY += memoryY[i];
        }
        mag = Math.sqrt(guessX * guessX + guessY * guessY);
        int moveX = (int)(guessX / mag * 4);
        int moveY = (int)(guessY / mag * 4);
        // System.out.println("Dense: " + guessX + " " + guessY);
        return Pathfinder.execute(Cache.MY_LOCATION.translate(moveX, moveY));
    }

    public static void updateMemory() {
        for (RobotInfo ri : Cache.ENEMY_ROBOTS) {
            if (!memoryContains(ri.getID())) {
                MapLocation loc = ri.getLocation();
                memoryX[memoryIdx] = loc.x - Cache.MY_LOCATION.x;
                memoryY[memoryIdx] = loc.y - Cache.MY_LOCATION.y;
                memoryID[memoryIdx] = ri.getID();
                memoryIdx = (memoryIdx + 1) % 10;
            }
        }
    }

    public static boolean memoryContains(int id) {
        for (int i = 0; i < 10; i++) {
            if (memoryID[i] == id) {
                return true;
            }
        }
        return false;
    }

    private static MapLocation lastECVisited = null;
    public static boolean tryECSpiral() {
        return MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getClosestLocation(Cache.MY_LOCATION).map(ec -> {
            tryECSpiral(ec);
            return true;
        }).orElse(false);
    }

    public static void tryECSpiral(MapLocation ec) {
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
