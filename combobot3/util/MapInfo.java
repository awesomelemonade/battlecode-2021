package combobot3.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class MapInfo {
    private static RobotController rc;
    public static final int MAP_UNKNOWN_EDGE = Integer.MIN_VALUE;
    public static int mapMinX = MAP_UNKNOWN_EDGE; // not known
    public static int mapMaxX = MAP_UNKNOWN_EDGE; // not known
    public static int mapMinY = MAP_UNKNOWN_EDGE; // not known
    public static int mapMaxY = MAP_UNKNOWN_EDGE; // not known
    public static EnlightenmentCenterList[] enlightenmentCenterLocations; // indexed by Team.ordinal()
    public static SlandererQueue enemySlandererLocations;
    public static final int ENEMY_SLANDERER_RETENTION = 50;

    public static void init(RobotController rc) {
        MapInfo.rc = rc;
        enlightenmentCenterLocations = new EnlightenmentCenterList[3];
        // unrolled loop
        enlightenmentCenterLocations[0] = new EnlightenmentCenterList(Team.A);
        enlightenmentCenterLocations[1] = new EnlightenmentCenterList(Team.B);
        enlightenmentCenterLocations[2] = new EnlightenmentCenterList(Team.NEUTRAL);
        // slanderer queue
        enemySlandererLocations = new SlandererQueue(50);
    }
    public static void loop() {
        updateBoundaries();
        enemySlandererLocations.removeExpiredLocations(Cache.TURN_COUNT - ENEMY_SLANDERER_RETENTION);
    }
    // TODO: Communication
    private static void updateBoundaries() {
        if (MapInfo.mapMinX == MAP_UNKNOWN_EDGE && !rc.canSenseLocation(Cache.MY_LOCATION.translate(-Constants.SENSE_BOX_RADIUS, 0))) {
            for(int d = Constants.SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(-d, 0))) {
                    MapInfo.mapMinX = Cache.MY_LOCATION.x - d;
                    break;
                }
            }
            if(MapInfo.mapMinX == MAP_UNKNOWN_EDGE) {
                MapInfo.mapMinX = Cache.MY_LOCATION.x;
            }
        }
        if (MapInfo.mapMaxX == MAP_UNKNOWN_EDGE && !rc.canSenseLocation(Cache.MY_LOCATION.translate(Constants.SENSE_BOX_RADIUS, 0))) {
            for(int d = Constants.SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(d, 0))) {
                    MapInfo.mapMaxX = Cache.MY_LOCATION.x + d;
                    break;
                }
            }
            if(MapInfo.mapMaxX == MAP_UNKNOWN_EDGE) {
                MapInfo.mapMaxX = Cache.MY_LOCATION.x;
            }
        }
        if (MapInfo.mapMinY == MAP_UNKNOWN_EDGE && !rc.canSenseLocation(Cache.MY_LOCATION.translate(0, -Constants.SENSE_BOX_RADIUS))) {
            for(int d = Constants.SENSE_BOX_RADIUS-1; d >= 1; d--) {
                if(rc.canSenseLocation(Cache.MY_LOCATION.translate(0, -d))) {
                    MapInfo.mapMinY = Cache.MY_LOCATION.y - d;
                    break;
                }
            }
            if(MapInfo.mapMinY == MAP_UNKNOWN_EDGE) {
                MapInfo.mapMinY = Cache.MY_LOCATION.y;
            }
        }
        if (MapInfo.mapMaxY == MAP_UNKNOWN_EDGE && !rc.canSenseLocation(Cache.MY_LOCATION.translate(0, Constants.SENSE_BOX_RADIUS))) {
            for(int d = Constants.SENSE_BOX_RADIUS-1; d >= 1; d--) {
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
    public static boolean potentiallyInBounds(MapLocation loc) {
        if (MapInfo.mapMinX != MapInfo.MAP_UNKNOWN_EDGE && loc.x < MapInfo.mapMinX)
            return false;
        if (MapInfo.mapMinY != MapInfo.MAP_UNKNOWN_EDGE && loc.y < MapInfo.mapMinY)
            return false;
        if (MapInfo.mapMaxX != MapInfo.MAP_UNKNOWN_EDGE && loc.x > MapInfo.mapMaxX)
            return false;
        if (MapInfo.mapMaxY != MapInfo.MAP_UNKNOWN_EDGE && loc.y > MapInfo.mapMaxY)
            return false;
        return true;
    }
    public static void addKnownEnlightenmentCenter(Team ecTeam, MapLocation ecLocation, int conviction) {
        // loop unrolling
        switch (ecTeam.ordinal()) {
            case 0:
                enlightenmentCenterLocations[0].addOrUpdate(ecLocation, conviction);
                enlightenmentCenterLocations[1].removeIf(x -> x.equals(ecLocation));
                enlightenmentCenterLocations[2].removeIf(x -> x.equals(ecLocation));
                break;
            case 1:
                enlightenmentCenterLocations[0].removeIf(x -> x.equals(ecLocation));
                enlightenmentCenterLocations[1].addOrUpdate(ecLocation, conviction);
                enlightenmentCenterLocations[2].removeIf(x -> x.equals(ecLocation));
                break;
            case 2:
                enlightenmentCenterLocations[0].removeIf(x -> x.equals(ecLocation));
                enlightenmentCenterLocations[1].removeIf(x -> x.equals(ecLocation));
                enlightenmentCenterLocations[2].addOrUpdate(ecLocation, conviction);
                break;
        }
    }
    public static EnlightenmentCenterList getKnownEnlightenmentCenterList(Team team) {
        return enlightenmentCenterLocations[team.ordinal()];
    }
}
