package bigms.util;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import bigms.RobotPlayer;

import java.util.function.Predicate;

public class Util {
    private static RobotController rc;
    private static boolean isCentral;
    private static Direction directionAwayFromSpawnEC;

    public static void init(RobotController rc) {
        Util.rc = rc;
        // in order for units to have different rngs
        for(int i = 0; i < rc.getID() % 20; i++) {
            double blah = Math.random();
        }
        Constants.init(rc);
        Cache.init(rc);
        MapInfo.init(rc);
        LatticeUtil.init(rc);
        Pathfinder.init(rc);
        isCentral = rc.getType() == RobotType.ENLIGHTENMENT_CENTER;
        if (isCentral) {
            CentralCommunication.init(rc);
        } else {
            UnitCommunication.init(rc);
        }
        for(int i = 0; i < 16; i++) {
            exploreDirs[i] = i;
        }
    }

    public static void move(Direction direction) {
        Cache.lastDirection = direction;
    }

    public static void loop() throws GameActionException {
        Cache.loop();
        MapInfo.loop();
        if (isCentral) {
            CentralCommunication.loop();
        } else {
            UnitCommunication.loop();
        }
        if (Cache.TURN_COUNT == 1 && rc.getType() != RobotType.ENLIGHTENMENT_CENTER) {
            for (RobotInfo ally : Cache.ALLY_ROBOTS) {
                if (ally.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    directionAwayFromSpawnEC = ally.getLocation().directionTo(Cache.MY_LOCATION);
                }
            }
        }
    }

    public static void postLoop() throws GameActionException {
        if (Constants.DEBUG_DRAW) {
            MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).forEachLocation(x -> {
                Util.setIndicatorDot(x, 255, 255, 255);
            });
            MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).forEachLocation(x -> {
                Util.setIndicatorDot(x, 128, 128, 128);
            });
            MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).forEachLocation(x -> {
                Util.setIndicatorDot(x, 0, 0, 0);
            });
            MapInfo.enemySlandererLocations.forEach(x -> {
                Util.setIndicatorDot(x, 0, 128, 0); // dark green
            });
        }
        if (isCentral) {
            CentralCommunication.postLoop();
        } else {
            if (Clock.getBytecodesLeft() >= 165) {
                if (Cache.lastDirection != Direction.CENTER) {
                    rc.move(Cache.lastDirection);
                }
                if (rc.getRoundNum() != RobotPlayer.currentTurn) {
                    // Do not communicate about any particular unit
                    UnitCommunication.clearFlag();
                    Util.setIndicatorDot(Cache.MY_LOCATION, 255, 0, 255);
                    Util.println("WARNING: Cleared flag (not enough bytecodes)");
                }
                UnitCommunication.postLoop();
            } else {
                Util.setIndicatorDot(Cache.MY_LOCATION, 255, 0, 255);
                Util.println("WARNING: Didn't move (not enough bytecodes)");
            }
        }
    }

    public static boolean tryBuildRobot(RobotType type, Direction direction, int influence) {
        if (rc.canBuildRobot(type, direction, influence)) {
            try {
                rc.buildRobot(type, direction, influence);
            } catch (GameActionException ex) {
                throw new IllegalStateException(ex);
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryBuildRobotTowards(RobotType type, Direction direction, int influence) {
        for (Direction buildDirection : Constants.getAttemptOrder(direction)) {
            if (tryBuildRobot(type, buildDirection, influence)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryMove(Direction direction) {
        if (rc.canMove(direction)) {
            move(direction);
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryMove(MapLocation loc) {
        return Pathfinder.execute(loc);
    }

    public static boolean tryMoveTowards(Direction direction) {
        for (Direction moveDirection : Constants.getAttemptOrder(direction)) {
            if (tryMove(moveDirection)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryRandomMove() {
        return tryMove(randomAdjacentDirection());
    }

    public static boolean tryKiteFrom(MapLocation location) {
        Util.setIndicatorLine(Cache.MY_LOCATION, location, 255, 128, 0); // orange
        return Util.tryMoveTowards(location.directionTo(rc.getLocation()));
    }

    public static boolean hasAdjacentAllyRobot(MapLocation location) {
        if (Cache.ALLY_ROBOTS.length >= 20) {
            return rc.senseNearbyRobots(location, 2, Constants.ALLY_TEAM).length > 0;
        } else {
            // loop through robot list
            for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
                if (location.isWithinDistanceSquared(Cache.ALLY_ROBOTS[i].getLocation(), 2)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static int numAllyRobotsWithin(MapLocation location, int distanceSquared) {
        if (Cache.ALLY_ROBOTS.length >= 20) {
            return rc.senseNearbyRobots(location, distanceSquared, Constants.ALLY_TEAM).length;
        } else {
            // loop through robot list
            int count = 0;
            for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
                if (location.isWithinDistanceSquared(Cache.ALLY_ROBOTS[i].getLocation(), distanceSquared)) {
                    count++;
                }
            }
            return count;
        }
    }

    public static boolean tryMoveSpacedApart(Direction direction) {
        if (rc.canMove(direction) && !Util.hasAdjacentAllyRobot(Cache.MY_LOCATION.add(direction))) {
            move(direction);
            return true;
        } else {
            return false;
        }
    }
    public static boolean tryMoveTowardsSpacedApart(Direction direction) {
        for (Direction moveDirection : Constants.getAttemptOrder(direction)) {
            if (tryMoveSpacedApart(moveDirection)) {
                return true;
            }
        }
        return Util.tryMoveTowards(direction);
    }

    private static Direction previousDirection = randomAdjacentDirection();

    public static boolean randomExplore() {
        Util.setIndicatorDot(Cache.MY_LOCATION, 255, 128, 0); // orange
        // TODO: Incorporate pathfinding algorithms?
        Direction bestDirection = null;
        int minAllies = Integer.MAX_VALUE;
        for (Direction direction : Constants.getAttemptOrder(previousDirection)) {
            if (rc.canMove(direction)) {
                MapLocation next = Cache.MY_LOCATION.add(direction);
                int numAllies = numAllyRobotsWithin(next, 10);
                if (numAllies < minAllies) {
                    bestDirection = direction;
                    minAllies = numAllies;
                }
            }
        }
        if (bestDirection != null) {
            Util.move(bestDirection);
            previousDirection = bestDirection;
            return true;
        }
        // traffic jam
        return false;
    }

    public static int findSymmetry(double dx, double dy) {
        int bestIndex = -1;
        double most = -5;
        for (int i = 0; i < 8; i++) {
            double x = Constants.ORDINAL_OFFSET_X[i];
            double y = Constants.ORDINAL_OFFSET_Y[i];
            double temp = (x * dx + y * dy) / Math.sqrt((x*x + y*y) * (dx*dx + dy*dy));
            if (temp > most) {
                most = temp;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    public static int findSymmetry2(double dx, double dy, int blacklist) {
        int bestIndex = -1;
        double most = -5;
        for (int i = 0; i < 8; i++) {
            if (i == blacklist) {
                continue;
            }
            double x = Constants.ORDINAL_OFFSET_X[i];
            double y = Constants.ORDINAL_OFFSET_Y[i];
            double temp = (x * dx + y * dy) / Math.sqrt((x*x + y*y) * (dx*dx + dy*dy));
            if (temp > most) {
                most = temp;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static int exploreDir = -1;
    private static boolean hasExplored = false;
    public static MapLocation exploreLoc;
    public static int[] exploreDirs = new int[16];

    public static boolean smartExplore() throws GameActionException {
        Util.setIndicatorDot(Cache.MY_LOCATION, 255, 128, 0); // orange
        if (exploreDir == -1 || reachedBorder(exploreDir) || goingTowardsEC(exploreDir)) {
            if (rc.getRoundNum() < 100 && !hasExplored) {
                exploreDir = directionAwayFromSpawnEC.ordinal();
                exploreLoc = new MapLocation(Cache.MY_LOCATION.x + Constants.ORDINAL_OFFSET_X[exploreDir] * 62,
                        Cache.MY_LOCATION.y + Constants.ORDINAL_OFFSET_Y[exploreDir] * 62);
                hasExplored = true;
            } else {
                // shuffle directions
                for (int i = 0; i < 15; i++) {
                    int idx = randBetween(i + 1, 15);
                    int tmp = exploreDirs[idx];
                    exploreDirs[idx] = exploreDirs[i];
                    exploreDirs[i] = tmp;
                }
                for (int i = 0; i < 16; i++) {
                    int potentialDir = exploreDirs[i];
                    MapLocation potentialLoc = new MapLocation(
                            Cache.MY_LOCATION.x + Constants.ORDINAL_OFFSET_X[potentialDir] * 62,
                            Cache.MY_LOCATION.y + Constants.ORDINAL_OFFSET_Y[potentialDir] * 62);
                    // checks that the potentialDir is not in the same or opposite direction as exploreDir
                    if (exploreDir != -1 && (potentialDir == exploreDir ||
                            (Constants.ORDINAL_OFFSET_X[potentialDir] + Constants.ORDINAL_OFFSET_X[exploreDir] == 0 &&
                                    Constants.ORDINAL_OFFSET_Y[potentialDir] + Constants.ORDINAL_OFFSET_Y[exploreDir] == 0))) {
                        continue;
                    }
                    if (reachedBorder(potentialDir) || goingTowardsEC(potentialDir)) continue;
                    exploreDir = potentialDir;
                    exploreLoc = potentialLoc;
                    break;
                }
            }
        }
        if (exploreDir == -1) return randomExplore();
        return Pathfinder.execute(borderCut(exploreLoc));
    }

    public static MapLocation borderCut(MapLocation target) {
        MapLocation ret = new MapLocation(target.x, target.y);
        if (MapInfo.mapMaxX != MapInfo.MAP_UNKNOWN_EDGE && ret.x > MapInfo.mapMaxX) {
            ret = new MapLocation(MapInfo.mapMaxX - 1, ret.y);
        } else if (MapInfo.mapMinX != MapInfo.MAP_UNKNOWN_EDGE && ret.x < MapInfo.mapMinX) {
            ret = new MapLocation(MapInfo.mapMinX + 1, ret.y);
        }
        if (MapInfo.mapMaxY != MapInfo.MAP_UNKNOWN_EDGE && ret.y > MapInfo.mapMaxY) {
            ret = new MapLocation(ret.x, MapInfo.mapMaxY - 1);
        } else if (MapInfo.mapMinY != MapInfo.MAP_UNKNOWN_EDGE && ret.y < MapInfo.mapMinY) {
            ret = new MapLocation(ret.x, MapInfo.mapMinY + 1);
        }
        return ret;
    }

    public static boolean goingTowardsEC(int dir) throws GameActionException {
        int tempX = Constants.ORDINAL_OFFSET_X[dir];
        int tempY = Constants.ORDINAL_OFFSET_Y[dir];
        MapLocation loc = Cache.MY_LOCATION.translate(tempX, tempY);
        int futureDist = MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getClosestLocationDistance(loc, 1024);
        int curDist = MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getClosestLocationDistance(Cache.MY_LOCATION, 1024);
        if(futureDist <= curDist && futureDist <= 25) {
            return true;
        }
        return false;
    }

    public static boolean reachedBorder(int dir) throws GameActionException {
        int tempX = Constants.ORDINAL_OFFSET_X[dir];
        int tempY = Constants.ORDINAL_OFFSET_Y[dir];
        if (tempX == 2 || tempX == -2) {
            tempX = tempX / 2;
        }
        if (tempY == 2 || tempY == -2) {
            tempY = tempY / 2;
        }
        MapLocation loc1 = Cache.MY_LOCATION.translate(tempX * 3, 0);
        MapLocation loc2 = Cache.MY_LOCATION.translate(0, tempY * 3);
        if (tempX != 0 && rc.canSenseLocation(loc1) && rc.onTheMap(loc1)) {
            return false;
        }
        if (tempY != 0 && rc.canSenseLocation(loc2) && rc.onTheMap(loc2)) {
            return false;
        }
        return true;
    }

    public static int randBetween(int l, int r) {
        return l + (int) (Math.random() * (r - l + 1));
    }

    /**
     * Usage: Util.random(Constants.CARDINAL_DIRECTIONS)
     *
     * @param directions
     * @return
     */
    public static Direction random(Direction[] directions) {
        return directions[(int) (Math.random() * directions.length)];
    }

    public static Direction randomAdjacentDirection() {
        return random(Constants.ORDINAL_DIRECTIONS);
    }

    public static RobotInfo getClosestRobot(RobotInfo[] robots, Predicate<RobotInfo> filter) {
        int bestDistanceSquared = Integer.MAX_VALUE;
        RobotInfo bestRobot = null;
        for (RobotInfo robot : robots) {
            if (filter.test(robot)) {
                int distanceSquared = robot.getLocation().distanceSquaredTo(rc.getLocation());
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    bestRobot = robot;
                }
            }
        }
        return bestRobot;
    }
    public static RobotInfo getClosestEnemyRobot(MapLocation location, int limit, Predicate<RobotInfo> filter) {
        int bestDistanceSquared = limit + 1;
        RobotInfo bestRobot = null;
        for (RobotInfo enemy : Cache.ENEMY_ROBOTS) { // TODO: Tie breakers, use communication
            if (filter.test(enemy)) {
                int distanceSquared = enemy.getLocation().distanceSquaredTo(location);
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    bestRobot = enemy;
                }
            }
        }
        return bestRobot;
    }

    public static RobotInfo getClosestEnemyRobot(Predicate<RobotInfo> filter) {
        return getClosestEnemyRobot(rc.getLocation(), Constants.MAX_DISTANCE_SQUARED, filter);
    }

    public static RobotInfo getClosestEnemyRobot() {
        int bestDistanceSquared = Integer.MAX_VALUE;
        RobotInfo bestRobot = null;
        for (int i = Cache.ENEMY_ROBOTS.length; --i >= 0;) {
            RobotInfo enemy = Cache.ENEMY_ROBOTS[i];
            int distanceSquared = enemy.getLocation().distanceSquaredTo(rc.getLocation());
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestRobot = enemy;
            }
        }
        return bestRobot;
    }

    public static void setIndicatorDot(MapLocation loc, int red, int green, int blue) {
        if(!Constants.DEBUG_DRAW) return;
        rc.setIndicatorDot(loc, red, green, blue);
    }

    public static void setIndicatorLine(MapLocation startLoc, MapLocation endLoc, int red, int green, int blue) {
        if(!Constants.DEBUG_DRAW) return;
        rc.setIndicatorLine(startLoc, endLoc, red, green, blue);
    }

    public static void println(String s) {
        if(!Constants.DEBUG_PRINT) return;
        System.out.println(s);
    }
}
