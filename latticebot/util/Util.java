package latticebot.util;

import battlecode.common.*;
import static latticebot.util.Constants.*;

import java.util.function.Predicate;

public class Util {
    private static RobotController rc;

    public static void init(RobotController rc) {
        Util.rc = rc;
        // in order for units to have different rngs
        for(int i = 0; i < rc.getID()%20; i++) {
            double blah = Math.random();
        }
        Constants.init(rc);
        Cache.init(rc);
        MapInfo.init(rc);
        LatticeUtil.init(rc);
        Pathfinder.init(rc);
        Communication.init(rc);
    }

    public static void loop() {
        Cache.loop();
        MapInfo.loop();
    }

    public static boolean tryBuildRobot(RobotType type, Direction direction, int influence) throws GameActionException {
        if (rc.canBuildRobot(type, direction, influence)) {
            rc.buildRobot(type, direction, influence);

            // add to list of produced units
            RobotInfo new_robot = rc.senseRobotAtLocation(Cache.MY_LOCATION.add(direction));
            if (new_robot == null) {
                System.out.println("failed to produce robot???");
            }
            Communication.update_known_units(new_robot.ID);
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryBuildRobotTowards(RobotType type, Direction direction, int influence)
            throws GameActionException {
        for (Direction buildDirection : Constants.getAttemptOrder(direction)) {
            if (tryBuildRobot(type, buildDirection, influence)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryMove(Direction direction) throws GameActionException {
        if (rc.canMove(direction)) {
            rc.move(direction);
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryMoveTowards(Direction direction) throws GameActionException {
        for (Direction moveDirection : Constants.getAttemptOrder(direction)) {
            if (tryMove(moveDirection)) {
                return true;
            }
        }
        return false;
    }

    public static int exploreIndexToLocationX(int idx) {
        return SPAWN.x - 1 + (idx - 15) * 4;
    }

    public static int exploreIndexToLocationY(int idx) {
        return SPAWN.y - 1 + (idx - 15) * 4;
    }

    public static MapLocation exploreIndexToLocation(int x, int y) {
        return new MapLocation(exploreIndexToLocationX(x), exploreIndexToLocationY(y));
    }

    public static int locationToExploreIndexX(int idx) {
        return (idx - SPAWN.x + 63) >> 2;
    }

    public static int locationToExploreIndexY(int idx) {
        return (idx - SPAWN.y + 63) >> 2;
    }

    public static void setExplored(MapLocation loc) {
        int x = locationToExploreIndexX(loc.x);
        int y = locationToExploreIndexY(loc.y);
        Cache.explored[x] |= (1 << y);
    }

    public static boolean getExplored(MapLocation loc) {
        if(Math.abs(loc.x-SPAWN.x) > 63) return true;
        if(Math.abs(loc.y-SPAWN.y) > 63) return true;
        if (Cache.mapMinX != -1 && loc.x < Cache.mapMinX)
            return true;
        if (Cache.mapMinY != -1 && loc.y < Cache.mapMinY)
            return true;
        if (Cache.mapMaxX != -1 && loc.x > Cache.mapMaxX)
            return true;
        if (Cache.mapMaxY != -1 && loc.y > Cache.mapMaxY)
            return true;
        int x = locationToExploreIndexX(loc.x);
        int y = locationToExploreIndexY(loc.y);
        return (Cache.explored[x] & (1 << y)) != 0;
    }

    public static boolean tryRandomMove() throws GameActionException {
        return tryMove(randomAdjacentDirection());
    }

    private static Direction previousDirection = randomAdjacentDirection();

    public static boolean randomExplore() throws GameActionException {
        if (tryMove(previousDirection)) {
            return true;
        }
        previousDirection = randomAdjacentDirection();
        return false;
    }

    private static MapLocation exploreDest;
    private static int randomExploreCountdown = 0;
    private static int timeSpentOnThisDestination = 0;

    // ~1200 bytecodes if we need to find a new destination, ~300 otherwise
    public static boolean smartExplore() throws GameActionException {
        if(randomExploreCountdown > 0) {
            randomExploreCountdown--;
            return randomExplore();
        }
        if(timeSpentOnThisDestination == 50) {
            timeSpentOnThisDestination = 0;
            randomExploreCountdown = 20;
            return randomExplore();
        }
        if (exploreDest == null || getExplored(exploreDest)) {
            timeSpentOnThisDestination = 0;
            if (Clock.getBytecodesLeft() < 2000) {
                return randomExplore();
            }
            // pick a random unexplored location within a 3x3 area of the explored array
            // centered on current position
            int x = locationToExploreIndexX(Cache.MY_LOCATION.x);
            int y = locationToExploreIndexY(Cache.MY_LOCATION.y);
            MapLocation[] possibilities = new MapLocation[9];
            int ptr = 0;
            for (int newx = x - 1; newx <= x + 1; newx++) {
                for (int newy = y - 1; newy <= y + 1; newy++) {
                    MapLocation loc = exploreIndexToLocation(newx, newy);
                    if(!getExplored(loc)) {
                        possibilities[ptr++] = loc;
                    }
                }
            }
            if(ptr == 0) {
                return randomExplore();
            } else {
                exploreDest = possibilities[randBetween(0, ptr-1)];
                return Pathfinder.execute(exploreDest);
            }
        }
        timeSpentOnThisDestination++;
        return Pathfinder.execute(exploreDest);
    }

    public static int randBetween(int l, int r) {
        return l + (int)(Math.random()*(r-l));
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
}
