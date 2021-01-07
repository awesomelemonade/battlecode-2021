package latticebot.util;

import battlecode.common.*;
import static latticebot.util.Constants.*;

public class MapInfo {
    private static RobotController rc;
    private static MapLocation origin;
    private static double[][][][] knownPassability = new double[9][9][][]; // 81 initial bytecodes (array creation)
    private static final int CHUNK_SIZE = 15; // 225 bytecodes per chunk (array creation)

    public static void init(RobotController rc) {
        MapInfo.rc = rc;
        // ensures that our starting location is in the middle of the middle chunk
        MapInfo.origin = rc.getLocation().translate(-67, -67);
        // sense initial passability
        // TODO
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
    public static void loop() {
        // based on previous movement direction, sense known passability
        /*
		for (controller.getType().sensorRadiusSquared) {

		}
		for (int i = 0; i < Constants.getOffsetLength(controller.getType().sensorRadiusSquared); i++) {
			int dx = Constants.OFFSET_X[i];
			int dy = Constants.OFFSET_Y[i];

		}
        */
        updateBoundaries();
    }
    private static void updateBoundaries() {
        if(Cache.mapMinX == -1 && !rc.canSenseLocation(Cache.MY_LOCATION.translate(-SENSE_BOX_RADIUS, 0))) {
            for(int d = SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(-d, 0))) {
                    Cache.mapMinX = Cache.MY_LOCATION.x - d;
                    break;
                }
            }
            if(Cache.mapMinX == -1) {
                Cache.mapMinX = Cache.MY_LOCATION.x;
            }
        }
        if(Cache.mapMaxX == -1 && !rc.canSenseLocation(Cache.MY_LOCATION.translate(SENSE_BOX_RADIUS, 0))) {
            for(int d = SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(d, 0))) {
                    Cache.mapMaxX = Cache.MY_LOCATION.x + d;
                    break;
                }
            }
            if(Cache.mapMaxX == -1) {
                Cache.mapMaxX = Cache.MY_LOCATION.x;
            }
        }
        if(Cache.mapMinY == -1 && !rc.canSenseLocation(Cache.MY_LOCATION.translate(0, -SENSE_BOX_RADIUS))) {
            for(int d = SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(0, -d))) {
                    Cache.mapMinY = Cache.MY_LOCATION.y - d;
                    break;
                }
            }
            if(Cache.mapMinY == -1) {
                Cache.mapMinY = Cache.MY_LOCATION.y;
            }
        }
        if(Cache.mapMaxY == -1 && !rc.canSenseLocation(Cache.MY_LOCATION.translate(0, SENSE_BOX_RADIUS))) {
            for(int d = SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(0, d))) {
                    Cache.mapMaxY = Cache.MY_LOCATION.y + d;
                    break;
                }
            }
            if(Cache.mapMaxY == -1) {
                Cache.mapMaxY = Cache.MY_LOCATION.y;
            }
        }
    }
}
