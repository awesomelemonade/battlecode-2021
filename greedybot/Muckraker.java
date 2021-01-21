package greedybot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import greedybot.util.Cache;
import greedybot.util.Constants;
import greedybot.util.LambdaUtil;
import greedybot.util.MapInfo;
import greedybot.util.Pathfinder;
import greedybot.util.Util;

import java.util.Comparator;

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
        if (Math.random() < 0.2) {
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
            // If we're near an ally enlightenment center, kite from all enemy muckrakers
            if (MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM)
                    .getClosestLocationDistance(Cache.MY_LOCATION, Integer.MAX_VALUE) <= 100) {
                // kite from closest muckraker
                if (LambdaUtil.arraysStreamMin(Cache.ENEMY_ROBOTS, r -> r.getType() == RobotType.MUCKRAKER,
                        Comparator.comparingInt(
                                r -> r.getLocation().distanceSquaredTo(Cache.MY_LOCATION))).map(r -> {
                    MapLocation rLocation = r.getLocation();
                    if (Cache.MY_LOCATION.isWithinDistanceSquared(rLocation, 5)) {
                        Util.tryKiteFrom(r.getLocation());
                        return true;
                    }
                    return false;
                }).orElse(false)) {
                    return;
                }
            }
            // If we're near a neutral enlightenment center, kite from ally big p
            if (MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL)
                    .getClosestLocationDistance(Cache.MY_LOCATION, Integer.MAX_VALUE) <= 9) {
                // kite from closest big p
                if (LambdaUtil.arraysStreamMin(Cache.ALLY_ROBOTS,
                        r -> r.getType() == RobotType.POLITICIAN && r.getConviction() >= 50,
                        Comparator.comparingInt(
                                r -> r.getLocation().distanceSquaredTo(Cache.MY_LOCATION))).map(r -> {
                    Util.tryKiteFrom(r.getLocation());
                    return true;
                }).orElse(false)) {
                    return;
                }
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
            Pathfinder.execute(enemy);
            return true;
        }).orElse(false);
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
