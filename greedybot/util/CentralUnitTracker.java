package greedybot.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class CentralUnitTracker {
    private static final int SIZE = 33; // odd so EC can be at center location
    private static int originX;
    private static int originY;
    private static long[][] registry;
    private static int registryCounter = 0;

    public static final int NEARBY_DISTANCE_SQUARED = 100;
    public static int numNearbySmallDefenders = 0;
    public static int numNearbySmallEnemyMuckrakers = 0;
    public static int numNearbyAllySlanderers = 0;

    public static void init(MapLocation center) {
        originX = center.x - SIZE / 2;
        originY = center.y - SIZE / 2;
        registry = new long[SIZE][SIZE];
    }
    public static void loop() {
        // Clear counters
        numNearbySmallDefenders = 0;
        numNearbySmallEnemyMuckrakers = 0;
        numNearbyAllySlanderers = 0;
        // Increment registry counter
        registryCounter++;
        // Handle enemies in vision range
        for (int i = Cache.ENEMY_ROBOTS.length; --i >= 0;) {
            RobotInfo enemy = Cache.ENEMY_ROBOTS[i];
            handleEnemyUnit(enemy.location, enemy.type, enemy.conviction);
        }
    }
    public static void handleAllyUnit(MapLocation location, RobotType type, int conviction) {
        int indexX = location.x - originX;
        int indexY = location.y - originY;
        if (indexX < 0 || indexY < 0 || indexX >= SIZE || indexY >= SIZE) {
            // out of bounds
            return;
        }
        if (type == RobotType.POLITICIAN && conviction <= 25 &&
                Cache.MY_LOCATION.isWithinDistanceSquared(location, NEARBY_DISTANCE_SQUARED)) {
            numNearbySmallDefenders++;
            Util.setIndicatorDot(location, 60, 180, 75); // green
        }
        if (type == RobotType.SLANDERER &&
                Cache.MY_LOCATION.isWithinDistanceSquared(location, NEARBY_DISTANCE_SQUARED)) {
            numNearbyAllySlanderers++;
        }
    }
    public static void handleEnemyUnit(MapLocation location, RobotType type, int conviction) {
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
        // handle enemy unit
        if (type == RobotType.MUCKRAKER && conviction <= 10 &&
                Cache.MY_LOCATION.isWithinDistanceSquared(location, NEARBY_DISTANCE_SQUARED)) {
            numNearbySmallEnemyMuckrakers++;
            Util.setIndicatorDot(location, 230, 25, 75); // red
        }
    }
}
