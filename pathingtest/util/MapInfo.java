package pathingtest.util;

import battlecode.common.*;
import static pathingtest.util.Constants.*;

public class MapInfo {
    private static RobotController rc;
    private static MapLocation origin;
    private static int mapMinX = -1; // -1 if not known
    private static int mapMaxX = -1; // -1 if not known
    private static int mapMinY = -1; // -1 if not known
    private static int mapMaxY = -1; // -1 if not known
    private static double[][][][] knownPassability = new double[9][9][][]; // 81 initial bytecodes (array creation)
    private static final int CHUNK_SIZE = 15; // 225 bytecodes per chunk (array creation)

    public static void init(RobotController rc) throws GameActionException {
        MapInfo.rc = rc;
        // ensures that our starting location is in the middle of the middle chunk
        MapInfo.origin = rc.getLocation().translate(-67, -67);
        // sense initial passability
        int offset = (int) Math.sqrt(MY_TYPE.sensorRadiusSquared);
        for(int dx = -offset; dx <= offset; dx++) {
            for(int dy = -offset; dy <= offset; dy++) {
                if(dx*dx + dy*dy <= MY_TYPE.sensorRadiusSquared) {
                    MapLocation senseloc = rc.getLocation().translate(dx, dy);
                    if(rc.canSenseLocation(senseloc)) {
                        setKnownPassability(senseloc);
                    }
                }
            }
        }
    }

    public static double getKnownPassability(MapLocation location) throws GameActionException {
        int offsetX = location.x - origin.x;
        int offsetY = location.y - origin.y;
        int chunkX = offsetX / CHUNK_SIZE;
        int chunkY = offsetY / CHUNK_SIZE;
        int subX = offsetX % CHUNK_SIZE;
        int subY = offsetY % CHUNK_SIZE;
        return knownPassability[chunkX][chunkY][subX][subY];
    }

    public static void setKnownPassability(MapLocation location) throws GameActionException {
        int offsetX = location.x - origin.x;
        int offsetY = location.y - origin.y;
        int chunkX = offsetX / CHUNK_SIZE;
        int chunkY = offsetY / CHUNK_SIZE;
        int subX = offsetX % CHUNK_SIZE;
        int subY = offsetY % CHUNK_SIZE;
        if (knownPassability[chunkX][chunkY] == null) {
            knownPassability[chunkX][chunkY] = new double[CHUNK_SIZE][CHUNK_SIZE];
        }
        knownPassability[chunkX][chunkY][subX][subY] = rc.sensePassability(location);
    }

    public static void loop() throws GameActionException {
        /*int offset = (int) Math.sqrt(MY_TYPE.sensorRadiusSquared);
        for(int dx = -offset; dx <= offset; dx++) {
            for(int dy = -offset; dy <= offset; dy++) {
                if(dx*dx + dy*dy <= MY_TYPE.sensorRadiusSquared) {
                    MapLocation senseloc = MY_LOCATION.translate(dx, dy);
                    if(rc.canSenseLocation(senseloc)) {
                        setKnownPassability(senseloc);
                    }
                }
            }
        }*/
        // based on previous movement direction, sense known passability
        /*
         * for (controller.getType().sensorRadiusSquared) {
         * 
         * } for (int i = 0; i <
         * Constants.getOffsetLength(controller.getType().sensorRadiusSquared); i++) {
         * int dx = Constants.OFFSET_X[i]; int dy = Constants.OFFSET_Y[i];
         * 
         * }
         */
    }
}
