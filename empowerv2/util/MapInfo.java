package empowerv2.util;

import battlecode.common.*;

import static empowerv2.util.Constants.*;

public class MapInfo {
    private static RobotController rc;
    private static MapLocation origin;
    private static double[][][][] knownPassability = new double[9][9][][]; // 81 initial bytecodes (array creation)
    private static final int CHUNK_SIZE = 15; // 225 bytecodes per chunk (array creation)
    public static final int MAP_UNKNOWN_EDGE = Integer.MIN_VALUE;
    public static int mapMinX = MAP_UNKNOWN_EDGE; // not known
    public static int mapMaxX = MAP_UNKNOWN_EDGE; // not known
    public static int mapMinY = MAP_UNKNOWN_EDGE; // not known
    public static int mapMaxY = MAP_UNKNOWN_EDGE; // not known
    public static MapLocationList[] enlightenmentCenterLocations; // indexed by Team.ordinal()
    public static int[] explored = new int[32];

    public static void init(RobotController rc) {
        MapInfo.rc = rc;
        // ensures that our starting location is in the middle of the middle chunk
        MapInfo.origin = rc.getLocation().translate(-67, -67);
        // sense initial passability
        // TODO
        enlightenmentCenterLocations = new MapLocationList[Team.values().length];
        for (int i = 0; i < enlightenmentCenterLocations.length; i++) {
            enlightenmentCenterLocations[i] = new MapLocationList();
        }
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
        setExplored(Cache.MY_LOCATION);
        updateBoundaries();
    }
    // TODO: Communication
    private static void updateBoundaries() {
        if(MapInfo.mapMinX == MAP_UNKNOWN_EDGE && !rc.canSenseLocation(Cache.MY_LOCATION.translate(-SENSE_BOX_RADIUS, 0))) {
            for(int d = SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(-d, 0))) {
                    MapInfo.mapMinX = Cache.MY_LOCATION.x - d;
                    break;
                }
            }
            if(MapInfo.mapMinX == MAP_UNKNOWN_EDGE) {
                MapInfo.mapMinX = Cache.MY_LOCATION.x;
            }
        }
        if(MapInfo.mapMaxX == MAP_UNKNOWN_EDGE && !rc.canSenseLocation(Cache.MY_LOCATION.translate(SENSE_BOX_RADIUS, 0))) {
            for(int d = SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(d, 0))) {
                    MapInfo.mapMaxX = Cache.MY_LOCATION.x + d;
                    break;
                }
            }
            if(MapInfo.mapMaxX == MAP_UNKNOWN_EDGE) {
                MapInfo.mapMaxX = Cache.MY_LOCATION.x;
            }
        }
        if(MapInfo.mapMinY == MAP_UNKNOWN_EDGE && !rc.canSenseLocation(Cache.MY_LOCATION.translate(0, -SENSE_BOX_RADIUS))) {
            for(int d = SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(0, -d))) {
                    MapInfo.mapMinY = Cache.MY_LOCATION.y - d;
                    break;
                }
            }
            if(MapInfo.mapMinY == MAP_UNKNOWN_EDGE) {
                MapInfo.mapMinY = Cache.MY_LOCATION.y;
            }
        }
        if(MapInfo.mapMaxY == MAP_UNKNOWN_EDGE && !rc.canSenseLocation(Cache.MY_LOCATION.translate(0, SENSE_BOX_RADIUS))) {
            for(int d = SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(0, d))) {
                    MapInfo.mapMaxY = Cache.MY_LOCATION.y + d;
                    break;
                }
            }
            if(MapInfo.mapMaxY == MAP_UNKNOWN_EDGE) {
                MapInfo.mapMaxY = Cache.MY_LOCATION.y;
            }
        }
    }
    public static void addKnownEnlightementCenter(MapLocation ecLocation, Team ecTeam) {
        for (Team team : Team.values()) {
            MapLocationList list = enlightenmentCenterLocations[team.ordinal()];
            if (team == ecTeam) {
                if (!list.contains(ecLocation)) {
                    list.add(ecLocation);
                }
            } else {
                list.removeIf(x -> x.equals(ecLocation));
            }
        }
    }
    public static MapLocationList getKnownEnlightenmentCenterList(Team team) {
        return enlightenmentCenterLocations[team.ordinal()];
    }
    // Exploration

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
        explored[x] |= (1 << y);
    }

    public static boolean getExplored(MapLocation loc) {
        if(Math.abs(loc.x-SPAWN.x) > 63) return true;
        if(Math.abs(loc.y-SPAWN.y) > 63) return true;
        if (MapInfo.mapMinX != MapInfo.MAP_UNKNOWN_EDGE && loc.x < MapInfo.mapMinX)
            return true;
        if (MapInfo.mapMinY != MapInfo.MAP_UNKNOWN_EDGE && loc.y < MapInfo.mapMinY)
            return true;
        if (MapInfo.mapMaxX != MapInfo.MAP_UNKNOWN_EDGE && loc.x > MapInfo.mapMaxX)
            return true;
        if (MapInfo.mapMaxY != MapInfo.MAP_UNKNOWN_EDGE && loc.y > MapInfo.mapMaxY)
            return true;
        int x = locationToExploreIndexX(loc.x);
        int y = locationToExploreIndexY(loc.y);
        return (explored[x] & (1 << y)) != 0;
    }
}
