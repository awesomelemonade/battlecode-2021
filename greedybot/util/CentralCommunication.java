package greedybot.util;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class CentralCommunication {
    private static RobotController rc;
    public static void init(RobotController rc) {
        CentralCommunication.rc = rc;
        registered = new BooleanArray();
        CentralUnitTracker.init(rc.getLocation());
    }
    private static BooleanArray registered;
    // Stored as singly linked list
    static class UnitListNode {
        MapLocation location;
        int id;
        RobotType type;
        int lastKnownConviction;
        UnitListNode next;
        public UnitListNode(RobotInfo robot, UnitListNode next) {
            this.location = robot.location;
            this.id = robot.ID;
            this.type = robot.type;
            this.lastKnownConviction = robot.conviction;
            this.next = next;
        }
    }
    private static UnitListNode unitListHead = null;
    private static int unitListSize = 0;
    private static final int UNIT_LIST_MAX_SIZE = 80;

    public static final int DO_NOTHING_FLAG = 0b0000000_0000000_10000_10000;
    // 14 bits: rotate between [minX, maxX], [minY, maxY], [friendly ec], [enemy ec], [neutral ec]
    public static final int ROTATION_SHIFT_X = 17;
    public static final int ROTATION_SHIFT_Y = 10;
    public static final int ROTATION_MASK = 0b0111_1111;
    public static final int ROTATION_OFFSET = 64;
    // 10 bits: broadcast nearest known enemy to this EC
    public static void loop() throws GameActionException {
        rc.setFlag(0); // in case we run out of bytecodes
        CentralUnitTracker.loop();
        int start = Clock.getBytecodeNum();
        // read flags of each known robot (delete from list if not alive)
        UnitListNode prev = null;
        UnitListNode current = unitListHead;
        int count = 0;
        while (current != null) {
            int id = current.id;
            if (rc.canGetFlag(id)) {
                int flag = rc.getFlag(id) ^ UnitCommunication.DO_NOTHING_FLAG;
                MapLocation prevUnitLocation = current.location;
                // process info
                int dx = ((flag >> UnitCommunication.CURRENT_UNIT_OFFSET_X_SHIFT) & UnitCommunication.CURRENT_UNIT_OFFSET_MASK) - UnitCommunication.OFFSET_SHIFT;
                int dy = ((flag >> UnitCommunication.CURRENT_UNIT_OFFSET_Y_SHIFT) & UnitCommunication.CURRENT_UNIT_OFFSET_MASK) - UnitCommunication.OFFSET_SHIFT;
                if (dx == 0 && dy == 0) {
                    // TODO: communicate about map edge?
                } else {
                    MapLocation specifiedLocation = prevUnitLocation.translate(dx, dy);
                    RobotType type = RobotType.values()[(flag >> UnitCommunication.CURRENT_UNIT_TYPE_SHIFT) & UnitCommunication.CURRENT_UNIT_TYPE_MASK];
                    if (type == RobotType.SLANDERER) {
                        if (!MapInfo.enemySlandererLocations.contains(specifiedLocation)) {
                            MapInfo.enemySlandererLocations.add(specifiedLocation, Cache.TURN_COUNT);
                        }
                    }
                    if (type == RobotType.ENLIGHTENMENT_CENTER) {
                        int teamOrdinal = (flag >> UnitCommunication.CURRENT_EC_TEAM_SHIFT) & UnitCommunication.CURRENT_EC_TEAM_MASK;
                        int conviction = (flag & UnitCommunication.CURRENT_EC_CONVICTION_MASK) * 2;
                        Team ecTeam = Team.values()[teamOrdinal];
                        if (!specifiedLocation.equals(Cache.MY_LOCATION)) {
                            MapInfo.addKnownEnlightenmentCenter(ecTeam, specifiedLocation, conviction);
                        }
                    } else {
                        int conviction = flag & UnitCommunication.CURRENT_UNIT_INFO_MASK;
                        // Handle Enemy Unit
                        CentralUnitTracker.handleEnemyUnit(specifiedLocation, type, conviction);
                    }
                }
                // update location
                Direction dir = Direction.values()[Math.min(8, flag >> UnitCommunication.CURRENT_DIRECTION_SHIFT)];
                MapLocation unitLocation = prevUnitLocation.add(dir);
                current.location = unitLocation;
                // Handle Ally Unit
                CentralUnitTracker.handleAllyUnit(unitLocation, current.type, current.lastKnownConviction);
                // continue to next item in linked list
                prev = current; // prev is shifted one over
            } else {
                if (prev == null) {
                    unitListHead = current.next;
                    // prev remains null
                } else {
                    prev.next = current.next;
                    // prev remains the same
                }
                unitListSize--;
            }
            current = current.next;
            count++;
        }
        int end = Clock.getBytecodeNum();
        System.out.println("Handled " + count + " robots (" + (end - start) + " bytecodes)");
        // register any new ally robots (either though vision or building)
        start = Clock.getBytecodeNum();
        if (unitListSize < UNIT_LIST_MAX_SIZE) {
            for (RobotInfo ally : Cache.ALLY_ROBOTS) {
                // some sort of set - allocate in chunks of 4096?
                if (ally.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                    int id = ally.getID();
                    if (!registered.getAndSetTrue(id - Constants.MIN_ROBOT_ID)) {
                        // register in singly linked list
                        unitListHead = new UnitListNode(ally, unitListHead);
                        unitListSize++;
                        if (unitListSize >= UNIT_LIST_MAX_SIZE) {
                            break;
                        }
                    }
                }
            }
        }
        end = Clock.getBytecodeNum();
        System.out.println("Registered Robots (" + (end - start) + " bytecodes, " + Cache.ALLY_ROBOTS.length + " robots)");
    }
    public static final int NEAREST_ENEMY_OFFSET = 16;
    public static final int NEAREST_ENEMY_X_SHIFT = 5;
    public static final int NEAREST_ENEMY_MASK = 0b1_1111;
    public static void postLoop() throws GameActionException {
        // set flag
        int flag = 0;
        // TODO: Unused 10 bits
        int dx = NEAREST_ENEMY_OFFSET;
        int dy = NEAREST_ENEMY_OFFSET;
        if (dx < 0 || dx > NEAREST_ENEMY_MASK || dy < 0 || dy > NEAREST_ENEMY_MASK) {
            // underflow/overflow: set to current location to mark no enemy nearby
            dx = NEAREST_ENEMY_OFFSET;
            dy = NEAREST_ENEMY_OFFSET;
            Util.setIndicatorDot(Cache.MY_LOCATION, 255, 128, 0); // orange
        }
        flag = flag | (dx << NEAREST_ENEMY_X_SHIFT) | dy;
        // 7 bits on relative x location and relative y location
        MapLocation rotationLocation = null;
        switch (Cache.TURN_COUNT % 5) {
            case 0: // heartbeat
                rotationLocation = Cache.MY_LOCATION;
                break;
            case 1: // [ally ec]
                rotationLocation = MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getRandomLocation().orElse(null);
                break;
            case 2: // [enemy ec]
                rotationLocation = MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getRandomLocation().orElse(null);
                break;
            case 3: // [neutral ec]
                rotationLocation = MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).getRandomLocation().orElse(null);
                break;
            case 4: // [enemy slanderers]
                rotationLocation = MapInfo.enemySlandererLocations.getRandomLocation().orElse(null);
                break;
        }
        if (rotationLocation != null) {
            int rotationDx = rotationLocation.x - Cache.MY_LOCATION.x + ROTATION_OFFSET;
            int rotationDy = rotationLocation.y - Cache.MY_LOCATION.y + ROTATION_OFFSET;
            flag = flag | (rotationDx << ROTATION_SHIFT_X) | (rotationDy << ROTATION_SHIFT_Y);
        }
        rc.setFlag(flag ^ DO_NOTHING_FLAG);
    }
}
