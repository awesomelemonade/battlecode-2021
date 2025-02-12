package explore.util;

import battlecode.common.*;
import static explore.util.Constants.*;

import java.util.function.Predicate;

public class Util {
    private static RobotController rc;
    private static boolean isCentral;
    private static int exploreDx, exploreDy, exploreDir = -1;

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
    }

    public static void move(Direction direction) {
        try {
            rc.move(direction);
            Cache.lastDirection = direction;
        } catch (GameActionException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static void loop() throws GameActionException {
        Cache.loop();
        MapInfo.loop();
        if (isCentral) {
            CentralCommunication.loop();
        } else {
            UnitCommunication.loop();
        }
    }

    public static void postLoop() throws GameActionException {
        if (isCentral) {
            CentralCommunication.postLoop();
        } else {
            UnitCommunication.postLoop();
        }
        if (DEBUG_DRAW) {
            MapInfo.getKnownEnlightenmentCenterList(ALLY_TEAM).forEach(x -> {
                Util.setIndicatorDot(x, 255, 255, 255);
            });
            MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).forEach(x -> {
                Util.setIndicatorDot(x, 128, 128, 128);
            });
            MapInfo.getKnownEnlightenmentCenterList(ENEMY_TEAM).forEach(x -> {
                Util.setIndicatorDot(x, 0, 0, 0);
            });
            MapInfo.enemySlandererLocations.forEach(x -> {
                Util.setIndicatorDot(x, 0, 128, 0); // dark green
            });
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

    public static boolean hasAdjacentAllyRobot(MapLocation location) {
        if (Cache.ALLY_ROBOTS.length >= 20) {
            return rc.senseNearbyRobots(location, 2, ALLY_TEAM).length > 0;
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
            return rc.senseNearbyRobots(location, distanceSquared, ALLY_TEAM).length;
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

    private static MapLocation exploreDest;
    private static int randomExploreCountdown = 0;
    private static int timeSpentOnThisDestination = 0;

    // ~1200 bytecodes if we need to find a new destination, ~300 otherwise
    // public static boolean smartExplore() throws GameActionException {
    //     if(rc.getRoundNum() <= 150) {
    //         return dumbExplore();
    //     }
    //     Util.setIndicatorDot(Cache.MY_LOCATION, 255, 128, 0); // orange
    //     // if allies nearby, move away from them
    //     // if we haven't reached it for 10 moves, just assume we're blocked and can't get there
    //     if(timeSpentOnThisDestination == 10) {
    //         MapInfo.setExplored(exploreDest);
    //     }
    //     if (exploreDest == null || MapInfo.getExplored(exploreDest)) {
    //         timeSpentOnThisDestination = 0;
    //         if (Clock.getBytecodesLeft() < 2000) {
    //             return dumbExplore();
    //         }
    //         // pick a random unexplored location within a 3x3 area of the explored array
    //         // centered on current position
    //         int x = MapInfo.locationToExploreIndexX(Cache.MY_LOCATION.x);
    //         int y = MapInfo.locationToExploreIndexY(Cache.MY_LOCATION.y);
    //         MapLocation[] possibilities = new MapLocation[9];
    //         int ptr = 0;
    //         for (int newx = x - 1; newx <= x + 1; newx++) {
    //             for (int newy = y - 1; newy <= y + 1; newy++) {
    //                 MapLocation loc = MapInfo.exploreIndexToLocation(newx, newy);
    //                 if(!MapInfo.getExplored(loc)) {
    //                     possibilities[ptr++] = loc;
    //                 }
    //             }
    //         }
    //         if(ptr == 0) {
    //             return dumbExplore();
    //         } else {
    //             exploreDest = possibilities[randBetween(0, ptr-1)];
    //             return Pathfinder.execute(exploreDest);
    //         }
    //     }
    //     timeSpentOnThisDestination++;
    //     return Pathfinder.execute(exploreDest);
    // }

    public static boolean smartExplore() throws GameActionException {
        // TODO: optimize and implement 16 direction vectors instead of 8
        Util.setIndicatorDot(Cache.MY_LOCATION, 255, 128, 0); // orange
        // if allies nearby, move away from them
        // if we haven't reached it for 10 moves, just assume we're blocked and can't get there
        if (exploreDir == -1) {
            // pick a random unexplored location within a 3x3 area of the explored array
            // centered on current position
            exploreDir = randBetween(0, 7);
        }
        if (reachedBorder(exploreDir)) {
            exploreDir = -1;
            return smartExplore();
        }
        MapLocation target = new MapLocation(Cache.MY_LOCATION.x + Constants.ORDINAL_OFFSET_X[exploreDir]*4, Cache.MY_LOCATION.y + Constants.ORDINAL_OFFSET_Y[exploreDir]*4);

        if (MapInfo.mapMaxX != MapInfo.MAP_UNKNOWN_EDGE && target.x > MapInfo.mapMaxX) {
            target = new MapLocation(MapInfo.mapMaxX - 1, target.y);
        } else if (MapInfo.mapMinX != MapInfo.MAP_UNKNOWN_EDGE && target.x < MapInfo.mapMinX) {
            target = new MapLocation(MapInfo.mapMinX + 1, target.y);
        }
        if (MapInfo.mapMaxY != MapInfo.MAP_UNKNOWN_EDGE && target.y > MapInfo.mapMaxY) {
            target = new MapLocation(target.x, MapInfo.mapMaxY - 1);
        } else if (MapInfo.mapMinY != MapInfo.MAP_UNKNOWN_EDGE && target.y < MapInfo.mapMinY) {
            target = new MapLocation(target.x, MapInfo.mapMinY + 1);
        }
        return Pathfinder.execute(target);
    }

    public static boolean reachedBorder(int dir) throws GameActionException {
        int tempX = Constants.ORDINAL_OFFSET_X[dir];
        int tempY = Constants.ORDINAL_OFFSET_Y[dir];
        MapLocation loc1 = Cache.MY_LOCATION.translate(tempX * 3, 0);
        MapLocation loc2 = Cache.MY_LOCATION.translate(0, tempY * 3);
        if (tempX != 0 && rc.canDetectLocation(loc1) && rc.onTheMap(loc1)) {
            return false;
        }
        if (tempY != 0 && rc.canDetectLocation(loc2) && rc.onTheMap(loc2)) {
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
