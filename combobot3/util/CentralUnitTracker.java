package combobot3.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class CentralUnitTracker {
    private static final int SIZE = 37; // odd so EC can be at center location
    private static int originX;
    private static int originY;
    private static long[][] registry;
    private static int registryCounter = 0;

    public static final int NEARBY_DISTANCE_SQUARED = 225;
    public static int numSmallDefenders = 0;
    public static int numNearbySmallEnemyMuckrakers = 0;
    public static int numNearbyAllySlanderers = 0;

    public static final int TRACKER_SIZE = 50;
    public static MapLocation[] allyDefenderLocations = new MapLocation[TRACKER_SIZE]; // Only small politicians
    public static int allyDefenderLocationsSize = 0;
    public static MapLocation[] enemyMuckrakerLocations = new MapLocation[TRACKER_SIZE];
    public static int enemyMuckrakerLocationsSize = 0;

    public static void init(MapLocation center) {
        originX = center.x - SIZE / 2;
        originY = center.y - SIZE / 2;
        registry = new long[SIZE][SIZE];
    }
    public static void loop() {
        // Clear counters
        numSmallDefenders = 0;
        numNearbySmallEnemyMuckrakers = 0;
        numNearbyAllySlanderers = 0;
        allyDefenderLocationsSize = 0;
        enemyMuckrakerLocationsSize = 0;
        // Increment registry counter
        registryCounter++;
        // Handle enemies in vision range
        for (int i = Cache.ENEMY_ROBOTS.length; --i >= 0;) {
            RobotInfo enemy = Cache.ENEMY_ROBOTS[i];
            handleEnemyUnit(enemy.location, enemy.type, enemy.conviction, RobotType.ENLIGHTENMENT_CENTER, -1);
        }
    }
    public static EnemyInfo calculateBroadcastedEnemy() {
        // broadcast the enemy muckraker scored on the following
        // distance to this ec
        // distance to nearest defender
        double bestEnemyScore = 225; // minimize
        int bestEnemyClosestAllyDistanceSquared = Integer.MAX_VALUE;
        MapLocation bestEnemy = null;
        for (int i = enemyMuckrakerLocationsSize; --i >= 0;) {
            MapLocation enemy = enemyMuckrakerLocations[i];
            int closestAllyDistanceSquared = Integer.MAX_VALUE;
            for (int j = allyDefenderLocationsSize; --j >= 0;) {
                int distanceSquared = allyDefenderLocations[j].distanceSquaredTo(enemy);
                if (distanceSquared < closestAllyDistanceSquared) {
                    closestAllyDistanceSquared = distanceSquared;
                }
            }
            int distanceSquaredToEC = enemy.distanceSquaredTo(Cache.MY_LOCATION);
            if (distanceSquaredToEC <= 225 && closestAllyDistanceSquared > 9) {
                double score = distanceSquaredToEC - Math.sqrt(closestAllyDistanceSquared);
                if (score < bestEnemyScore) {
                    bestEnemyScore = score;
                    bestEnemyClosestAllyDistanceSquared = closestAllyDistanceSquared;
                    bestEnemy = enemy;
                }
            }
        }
        if (bestEnemy == null) {
            return null;
        } else {
            return new EnemyInfo(bestEnemy, bestEnemyClosestAllyDistanceSquared);
        }
    }
    static class EnemyInfo {
        MapLocation location;
        int closestAllyDistanceSquared;
        public EnemyInfo(MapLocation location, int closestAllyDistanceSquared) {
            this.location = location;
            this.closestAllyDistanceSquared = closestAllyDistanceSquared;
        }
    }
    public static void handleAllyUnit(MapLocation location, RobotType type, int conviction) {
        switch (type) {
            case POLITICIAN:
                if (conviction <= 25) {
                    numSmallDefenders++;
                    Util.setIndicatorDot(location, 60, 180, 75); // green
                }
                if (conviction <= 50) {
                    // TODO: don't add if it sees a muckraker target?
                    // Don't add politicians with initial cooldown
                    if (!location.isWithinDistanceSquared(Cache.MY_LOCATION, 2)) {
                        allyDefenderLocations[allyDefenderLocationsSize++] = location;
                    }
                }
                break;
            case SLANDERER:
                if (Cache.MY_LOCATION.isWithinDistanceSquared(location, NEARBY_DISTANCE_SQUARED)) {
                    numNearbyAllySlanderers++;
                }
                break;
        }
    }
    public static void handleEnemyUnit(MapLocation location, RobotType type, int conviction,
                                       RobotType fromRobotType, int fromConviction) {
        int indexX = location.x - originX;
        int indexY = location.y - originY;
        if (indexX < 0 || indexY < 0 || indexX >= SIZE || indexY >= SIZE) {
            // out of bounds
            return;
        }
        if (registry[indexX][indexY] == registryCounter) {
            // already handled
            return;
        }
        registry[indexX][indexY] = registryCounter;
        if (type == RobotType.MUCKRAKER && (!(fromRobotType == RobotType.POLITICIAN && fromConviction < 50))) {
            // append location to enemyMuckrakerLocations
            if (enemyMuckrakerLocationsSize < TRACKER_SIZE) {
                enemyMuckrakerLocations[enemyMuckrakerLocationsSize++] = location;
            }
        }
        // we don't realllly care unless it fits this description
        if (!(type == RobotType.MUCKRAKER && conviction <= 10 &&
                Cache.MY_LOCATION.isWithinDistanceSquared(location, NEARBY_DISTANCE_SQUARED))) {
            return;
        }
        numNearbySmallEnemyMuckrakers++;
        Util.setIndicatorDot(location, 230, 25, 75); // red
    }
}
