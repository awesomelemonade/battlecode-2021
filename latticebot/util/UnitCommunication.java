package latticebot.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class UnitCommunication {
    private static RobotController rc;
    public static void init(RobotController rc) {
        UnitCommunication.rc = rc;
    }
    public static final int OFFSET_SHIFT = 7;
    public static final int CURRENT_DIRECTION_SHIFT = 20;
    public static final int CURRENT_UNIT_OFFSET_X_SHIFT = 16;
    public static final int CURRENT_UNIT_OFFSET_Y_SHIFT = 12;
    public static final int CURRENT_UNIT_OFFSET_MASK = 0b1111;
    public static final int CURRENT_UNIT_TYPE_SHIFT = 10;
    public static final int CURRENT_UNIT_TYPE_MASK = 0b11;
    public static final int CURRENT_UNIT_INFO_MASK = 0b11_1111_1111;
    public static final int DEFAULT_FLAG = 0b1000_0111_0111_0000_0000_0000;
    public static final int CLEAR_FLAG = 0b0000_0111_0111_0000_0000_0000;
    private static int currentFlag = DEFAULT_FLAG;
    public static void loop() throws GameActionException {
        rc.setFlag(DEFAULT_FLAG); // in case we run out of bytecodes
        currentFlag = CLEAR_FLAG;
        RobotInfo enemy = Util.getClosestEnemyRobot();
        if (enemy != null) {
            setFlag(enemy);
        }
        for (RobotInfo ally : Cache.ALLY_ROBOTS) {
            if (ally.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                registerOurTeamEC(ally);
            } else {
                processFlagFromNearbyUnit(ally); // TODO
            }
        }
        processFlagsFromECs();
    }
    public static void postLoop() throws GameActionException {
        rc.setFlag((Cache.lastDirection.ordinal() << CURRENT_DIRECTION_SHIFT) | currentFlag);
    }
    public static void setFlag(RobotInfo unit) throws GameActionException {
        /*
        // see if there is any interesting unit
        RobotInfo unit = Util.getClosestEnemyRobot(); // TODO: but it already moved..
        if (unit == null) {
            unit = Util.getClosestRobot(Cache.ALLY_ROBOTS, x -> x.getType() == RobotType.ENLIGHTENMENT_CENTER);
        }*/
        currentFlag = 0;
        MapLocation unitLocation = unit.getLocation();
        int dx = unitLocation.x - Cache.MY_LOCATION.x + OFFSET_SHIFT; // 4 bits on relative x location
        int dy = unitLocation.y - Cache.MY_LOCATION.y + OFFSET_SHIFT; // 4 bits on relative y location
        currentFlag |= (dx << CURRENT_UNIT_OFFSET_X_SHIFT);
        currentFlag |= (dy << CURRENT_UNIT_OFFSET_Y_SHIFT);
        currentFlag |= (unit.getType().ordinal() << CURRENT_UNIT_TYPE_SHIFT);
        if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER) {
            // specify team
            currentFlag |= unit.getTeam().ordinal();
        } else {
            // specify influence
            currentFlag |= (unit.getInfluence() & CURRENT_UNIT_INFO_MASK);
        }
    }
    public static final int UNIT_INFO_BITMASK = 0b1111_1111_1111_1111_1111; // 20 bits

    /**
     * if this is a slanderer and we see a muckraker, run away!
     * if this is a politician and we see a muckraker, run towards it!
     * @param robot
     * @return a location of a muckraker retrieved from flag, otherwise null
     * @throws GameActionException
     */
    public static MapLocation processFlagFromNearbyUnit(RobotInfo robot) throws GameActionException {
        // ally robots that are nearby may have useful information
        int flag = rc.getFlag(robot.getID());
        // check if the unit has seen anything
        if ((flag & UNIT_INFO_BITMASK) != 0) {
            Direction unitPrevDirection = Direction.values()[flag >> CURRENT_DIRECTION_SHIFT];
            MapLocation prevLocation = robot.getLocation().add(unitPrevDirection.opposite());
            int dx = ((flag >> CURRENT_UNIT_OFFSET_X_SHIFT) & CURRENT_UNIT_OFFSET_MASK) - OFFSET_SHIFT;
            int dy = ((flag >> CURRENT_UNIT_OFFSET_Y_SHIFT) & CURRENT_UNIT_OFFSET_MASK) - OFFSET_SHIFT;
            MapLocation specifiedLocation = prevLocation.translate(dx, dy);
            RobotType type = RobotType.values()[(flag >> CURRENT_UNIT_TYPE_SHIFT) & CURRENT_UNIT_TYPE_MASK];
            // we see type at specifiedLocation
            if (type == RobotType.MUCKRAKER) {
                // we see muckraker!!
                return specifiedLocation;
            }
        }
        return null;
    }
    public static void processFlagsFromECs() throws GameActionException {
        ECNode prev = null;
        ECNode current = ecListHead;
        while (current != null) {
            int id = current.id;
            if (rc.canGetFlag(id)) {
                int flag = rc.getFlag(id);
                MapLocation ecLocation = current.location;
                int dx = ((flag >> CentralCommunication.NEAREST_ENEMY_X_SHIFT) & CentralCommunication.NEAREST_ENEMY_MASK) - CentralCommunication.NEAREST_ENEMY_OFFSET;
                int dy = (flag & CentralCommunication.NEAREST_ENEMY_MASK) - CentralCommunication.NEAREST_ENEMY_OFFSET;
                if (dx == 0 && dy == 0) {
                    current.nearestEnemy = null;
                } else {
                    current.nearestEnemy = ecLocation.translate(dx, dy);
                }
                int rotationDx = (flag >> CentralCommunication.ROTATION_SHIFT_X) - CentralCommunication.ROTATION_OFFSET;
                int rotationDy = ((flag >> CentralCommunication.ROTATION_SHIFT_Y) & CentralCommunication.ROTATION_MASK) - CentralCommunication.ROTATION_OFFSET;
                MapLocation rotationLocation = ecLocation.translate(rotationDx, rotationDy);
                switch (rc.getRoundNum() % 5) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                }
                prev = current;
            } else {
                // remove from SLL
                if (prev == null) {
                    ecListHead = current.next;
                } else {
                    prev.next = current.next;
                }
            }
            current = current.next;
        }
    }
    public static ECNode ecListHead;

    public static void registerOurTeamEC(RobotInfo ec) {
        int id = ec.getID();
        // check if it's in the list
        ECNode current = ecListHead;
        while (current != null) {
            if (current.id == id) {
                return;
            }
            current = current.next;
        }
        ecListHead = new ECNode(id, ec.getLocation(), ecListHead);
    }

    public static MapLocation getClosestECEnemyLocation(MapLocation location) {
        MapLocation closestEnemy = null;
        int closestDistanceSquared = Integer.MAX_VALUE;
        ECNode current = ecListHead;
        while (current != null) {
            MapLocation enemy = current.nearestEnemy;
            if (enemy != null) {
                int enemyDistanceSquared = location.distanceSquaredTo(enemy);
                if (enemyDistanceSquared < closestDistanceSquared) {
                    closestDistanceSquared = enemyDistanceSquared;
                    closestEnemy = enemy;
                }
            }
            current = current.next;
        }
        return closestEnemy;
    }

    static class ECNode {
        int id;
        MapLocation location;
        MapLocation nearestEnemy;
        ECNode next;
        public ECNode(int id, MapLocation location, ECNode next) {
            this.id = id;
            this.location = location;
            this.next = next;
        }
    }
}
