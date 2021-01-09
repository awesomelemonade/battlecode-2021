package metagame.util;

import battlecode.common.*;
import static metagame.util.Constants.*;

import java.util.function.Predicate;

public class Util {
    private static RobotController rc;

    public static void init(RobotController rc) throws GameActionException {
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

    public static void loop() throws GameActionException {
        Cache.loop();
        MapInfo.loop();
        Communication.loop();
    }

    public static boolean tryBuildRobot(RobotType type, Direction direction, int influence) throws GameActionException {
        if (rc.canBuildRobot(type, direction, influence)) {
            rc.buildRobot(type, direction, influence);

            // add to list of produced units
            RobotInfo new_robot = rc.senseRobotAtLocation(Cache.MY_LOCATION.add(direction));
            if (new_robot == null) {
                System.out.println("failed to produce robot???");
            }
            Communication.updateKnownUnits(new_robot.ID);
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

    public static boolean tryMove(MapLocation loc) throws GameActionException {
        return Pathfinder.execute(loc);
    }

    public static boolean tryMoveAway(MapLocation loc) throws GameActionException {
        int cur_x = Cache.MY_LOCATION.x;
        int cur_y = Cache.MY_LOCATION.y;
        MapLocation dest = new MapLocation(2*cur_x - loc.x, 2*cur_y - loc.y);
        return Pathfinder.execute(dest);
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
        // if we haven't reached it for 10 moves, just assume we're blocked and can't get there
        if(timeSpentOnThisDestination == 10) {
            setExplored(exploreDest);
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
        return l + (int)(Math.random()*(r-l+1));
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

    // attack neutral and enemy ECs while prioritizing neutral
    public static boolean attackEC() throws GameActionException {
        if (Cache.neutralECCount == 0) {
            return attackEnemyEC();
        }
        MapLocation target = getTargetNeutralEC(); // should never be null
        if (rc.canSenseLocation(target)) {
            RobotInfo temp = rc.senseRobotAtLocation(target);
            if (temp.getTeam() == rc.getTeam()) {
                Communication.commCapturedEC(target);
                removeFromNeutralECs(target);
                return attackEC();
            }
        }
        Pathfinder.execute(target);
        return true;
    }

    // attack only enemy ECs
    public static boolean attackEnemyEC() throws GameActionException {
        if (Cache.enemyECCount == 0) {
            return false;
        }
        MapLocation target = getTargetEnemyEC(); // should never be null
        if (rc.canSenseLocation(target)) {
            RobotInfo temp = rc.senseRobotAtLocation(target);
            if (temp.getTeam() == rc.getTeam()) {
                Communication.commCapturedEC(target);
                removeFromEnemyECs(target);
                return attackEnemyEC();
            }
        }
        Pathfinder.execute(target);
        return true;
    }

    public static void addToEnemyECs(MapLocation loc) {
        if (!inEnemyECs(loc)) {
            //System.out.println("Found enemy EC at " + loc);
            for (int i = Cache.enemyECs.length - 1; i >= 0; i--) {
                if (Cache.enemyECs[i] == null) {
                    Cache.enemyECs[i] = loc;
                    Cache.enemyECCount++;
                    return;
                }
            }
        }
    }

    public static void removeFromEnemyECs(MapLocation loc) {
        for (int i = Cache.enemyECs.length - 1; i >= 0; i--) {
            if (Cache.enemyECs[i] != null && Cache.enemyECs[i].equals(loc)) {
                Cache.enemyECs[i] = null;
                Cache.enemyECCount--;
            }
        }
    }

    public static MapLocation getTargetEnemyEC() {
        for (int i = Cache.enemyECs.length - 1; i >= 0; i--) {
            if (Cache.enemyECs[i] != null) {
                return Cache.enemyECs[i];
            }
        }
        return null;
    }

    public static boolean inEnemyECs(MapLocation loc) {
        for (int i = Cache.enemyECs.length - 1; i >= 0; i--) {
            if (Cache.enemyECs[i] != null && loc.equals(Cache.enemyECs[i])) {
                // dup, dont broadcast this one
                return true;
            }
        }
        return false;
    }

    public static void addToNeutralECs(MapLocation loc) {
        if (!inNeutralECs(loc)) {
            //System.out.println("Found enemy EC at " + loc);
            for (int i = Cache.neutralECs.length - 1; i >= 0; i--) {
                if (Cache.neutralECs[i] == null) {
                    Cache.neutralECs[i] = loc;
                    Cache.neutralECCount++;
                    return;
                }
            }
        }
    }

    public static void removeFromNeutralECs(MapLocation loc) {
        for (int i = Cache.neutralECs.length - 1; i >= 0; i--) {
            if (Cache.neutralECs[i].equals(loc)) {
                Cache.neutralECs[i] = null;
                Cache.neutralECCount--;
                return;
            }
        }
    }

    public static MapLocation getTargetNeutralEC() {
        for (int i = Cache.neutralECs.length - 1; i >= 0; i--) {
            if (Cache.neutralECs[i] != null) {
                return Cache.neutralECs[i];
            }
        }
        return null;
    }

    public static boolean inNeutralECs(MapLocation loc) {
        for (int i = Cache.neutralECs.length - 1; i >= 0; i--) {
            if (Cache.neutralECs[i] != null && loc.equals(Cache.neutralECs[i])) {
                // dup, dont broadcast this one
                return true;
            }
        }
        return false;
    }

    public static int indexToEdge(int i) {
        switch (i) {
            case 0:
                return Cache.mapMaxX;
            case 1:
                return Cache.mapMaxY;
            case 2:
                return Cache.mapMinX;
            case 3:
                return Cache.mapMinY;
        }
        return -1;
    }

    public static boolean indexToEdgeBool(int i) {
        switch (i) {
            case 0:
                return Cache.sentMapMaxX;
            case 1:
                return Cache.sentMapMaxY;
            case 2:
                return Cache.sentMapMinX;
            case 3:
                return Cache.sentMapMinY;
        }
        return false;
    }

    public static void setIndexEdge(int i, int val) {
        if (indexToEdge(i) != -1) {
            // already set
            //System.out.println("already have edge "+ i + " with value " + indexToEdge(i));
            return;
        }
        //System.out.println("got edge " + i + " with value " + val);
        switch (i) {
            case 0:
                Cache.mapMaxX = val;
                Cache.sentMapMaxX = true;
                break;
            case 1:
                Cache.mapMaxY = val;
                Cache.sentMapMaxY = true;
                break;
            case 2:
                Cache.mapMinX = val;
                Cache.sentMapMinX = true;
                break;
            case 3:
                Cache.mapMinY = val;
                Cache.sentMapMinY = true;
                break;
        }
    }

    public static MapLocation closestEnemyEC() {
        int min_dist = -1;
        MapLocation closest = null;
        for (int i = Cache.enemyECs.length - 1; i >= 0; i--) {
            if (Cache.enemyECs[i] != null &&
                    (min_dist == -1 || Cache.MY_LOCATION.distanceSquaredTo(Cache.enemyECs[i]) < min_dist)) {
                min_dist = Cache.MY_LOCATION.distanceSquaredTo(Cache.enemyECs[i]);
                closest = Cache.enemyECs[i];
            }
        }
        return closest;
    }
}
