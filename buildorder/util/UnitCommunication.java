package buildorder.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

import java.util.Comparator;

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
    public static final int DO_NOTHING_FLAG = 0b1000_0111_0111_0000_0000_0000;
    public static final int CLEAR_FLAG = 0b0000_0111_0111_0000_0000_0000;
    private static int currentFlag = DO_NOTHING_FLAG;
    public static MapLocation closestCommunicatedEnemy;
    public static int closestCommunicatedEnemyDistanceSquared;
    private static void checkCloseEnemy(MapLocation enemy) {
        if (enemy != null) {
            int enemyDistanceSquared = Cache.MY_LOCATION.distanceSquaredTo(enemy);
            if (enemyDistanceSquared < closestCommunicatedEnemyDistanceSquared) {
                closestCommunicatedEnemyDistanceSquared = enemyDistanceSquared;
                closestCommunicatedEnemy = enemy;
            }
        }
    }
    public static final int CURRENT_DIRECTION_CENTER_SLANDERER = 9;
    public static boolean isPotentialSlanderer(RobotInfo robot) {
        try {
            return ((rc.getFlag(robot.getID()) ^ DO_NOTHING_FLAG) >> CURRENT_DIRECTION_SHIFT) == CURRENT_DIRECTION_CENTER_SLANDERER;
        } catch (GameActionException ex) {
            throw new IllegalStateException(ex);
        }
    }
    public static void loop() throws GameActionException {
        rc.setFlag(0); // in case we run out of bytecodes
        currentFlag = CLEAR_FLAG;
        // Prioritize
        // 1. neutral/enemy enlightenment centers
        // 2. neutral/enemy units
        // 3. ally enlightenment centers
        LambdaUtil.arraysStreamMin(Cache.ALL_ROBOTS,
                r -> r.getType() == RobotType.ENLIGHTENMENT_CENTER || r.getTeam() != Constants.ALLY_TEAM,
                Comparator.comparingInt(r -> {
            int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(r.getLocation());
            boolean isEnlightenmentCenter = r.getType() == RobotType.ENLIGHTENMENT_CENTER;
            boolean isNotAlly = r.getTeam() != Constants.ALLY_TEAM;
            if (isEnlightenmentCenter && isNotAlly) {
                return distanceSquared - 20000;
            } else if (isNotAlly) {
                return distanceSquared - 10000;
            } else {
                return distanceSquared;
            }
        })).ifPresent(r -> {
            Util.setIndicatorLine(Cache.MY_LOCATION, r.getLocation(), 255, 255, 0); // yellow
            if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapInfo.addKnownEnlightementCenter(r.getLocation(), r.getTeam());
            }
            setFlag(r);
        });


        closestCommunicatedEnemy = null;
        closestCommunicatedEnemyDistanceSquared = Integer.MAX_VALUE;
        for (RobotInfo ally : Cache.ALLY_ROBOTS) {
            if (ally.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                registerOurTeamEC(ally);
            } else {
                checkCloseEnemy(processFlagFromNearbyUnit(ally));
            }
        }
        processFlagsFromECs();
        ECNode current = ecListHead;
        while (current != null) {
            checkCloseEnemy(current.nearestEnemy);
            current = current.next;
        }
        if (rc.getType() == RobotType.POLITICIAN || rc.getType() == RobotType.MUCKRAKER) {
            // To save bytecodes, we don't run this for slanderers
            for (Team team : Team.values()) {
                MapInfo.getKnownEnlightenmentCenterList(team).removeIf(loc -> {
                    try {
                        if (MapInfo.mapMinX != MapInfo.MAP_UNKNOWN_EDGE && loc.x < MapInfo.mapMinX) return true;
                        if (MapInfo.mapMinY != MapInfo.MAP_UNKNOWN_EDGE && loc.y < MapInfo.mapMinY) return true;
                        if (MapInfo.mapMaxX != MapInfo.MAP_UNKNOWN_EDGE && loc.x > MapInfo.mapMaxX) return true;
                        if (MapInfo.mapMaxY != MapInfo.MAP_UNKNOWN_EDGE && loc.y > MapInfo.mapMaxY) return true;
                        if (rc.canSenseLocation(loc)) {
                            RobotInfo robot = rc.senseRobotAtLocation(loc);
                            if (robot == null || robot.getType() != RobotType.ENLIGHTENMENT_CENTER || robot.getTeam() != team) {
                                return true;
                            }
                        }
                    } catch (GameActionException ex) {
                        ex.printStackTrace();
                        throw new IllegalStateException();
                    }
                    return false;
                });
            }
        }
    }
    public static void postLoop() throws GameActionException {
        if (rc.getType() == RobotType.SLANDERER && Cache.lastDirection == Direction.CENTER) {
            rc.setFlag(((CURRENT_DIRECTION_CENTER_SLANDERER << CURRENT_DIRECTION_SHIFT) | currentFlag) ^ DO_NOTHING_FLAG);
        } else {
            rc.setFlag(((Cache.lastDirection.ordinal() << CURRENT_DIRECTION_SHIFT) | currentFlag) ^ DO_NOTHING_FLAG);
        }
    }
    public static void setFlag(RobotInfo unit) {
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
            currentFlag |= Math.min(unit.getInfluence(), CURRENT_UNIT_INFO_MASK);
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
        int flag = rc.getFlag(robot.getID()) ^ DO_NOTHING_FLAG;
        // check if the unit has seen anything
        if ((flag & UNIT_INFO_BITMASK) != 0) {
            Direction unitPrevDirection = Direction.values()[Math.min(8, flag >> CURRENT_DIRECTION_SHIFT)];
            MapLocation prevLocation = robot.getLocation().add(unitPrevDirection.opposite());
            int dx = ((flag >> CURRENT_UNIT_OFFSET_X_SHIFT) & CURRENT_UNIT_OFFSET_MASK) - OFFSET_SHIFT;
            int dy = ((flag >> CURRENT_UNIT_OFFSET_Y_SHIFT) & CURRENT_UNIT_OFFSET_MASK) - OFFSET_SHIFT;
            if (dx == 0 && dy == 0) {
                // communicating about map edge, ignore
                return null;
            }
            MapLocation specifiedLocation = prevLocation.translate(dx, dy);
            RobotType type = RobotType.values()[(flag >> CURRENT_UNIT_TYPE_SHIFT) & CURRENT_UNIT_TYPE_MASK];
            int info = flag & UnitCommunication.CURRENT_UNIT_INFO_MASK;
            // we see type at specifiedLocation
            if (type != RobotType.ENLIGHTENMENT_CENTER || Team.values()[info] == Constants.ENEMY_TEAM) {
                // we see enemy!!
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
                int flag = rc.getFlag(id) ^ CentralCommunication.DO_NOTHING_FLAG;
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
                if (rotationDx == 0 && rotationDy == 0) {
                    current.lastHeartbeatTurn = rc.getRoundNum();
                }
                if (current.lastHeartbeatTurn != -1) {
                    MapLocation rotationLocation = ecLocation.translate(rotationDx, rotationDy);
                    switch ((rc.getRoundNum() - current.lastHeartbeatTurn) % 4) {
                        case 0: // heartbeat
                            Util.setIndicatorDot(rotationLocation, 255, 0, 255); // magenta
                            MapInfo.addKnownEnlightementCenter(rotationLocation, Constants.ALLY_TEAM);
                            break;
                        case 1: // [ally ec]
                            if (rotationDx != -CentralCommunication.ROTATION_OFFSET && rotationDy != -CentralCommunication.ROTATION_OFFSET) {
                                MapInfo.addKnownEnlightementCenter(rotationLocation, Constants.ALLY_TEAM);
                            }
                            break;
                        case 2: // [enemy ec]
                            if (rotationDx != -CentralCommunication.ROTATION_OFFSET && rotationDy != -CentralCommunication.ROTATION_OFFSET) {
                                MapInfo.addKnownEnlightementCenter(rotationLocation, Constants.ENEMY_TEAM);
                            }
                            break;
                        case 3: // [neutral ec]
                            if (rotationDx != -CentralCommunication.ROTATION_OFFSET && rotationDy != -CentralCommunication.ROTATION_OFFSET) {
                                MapInfo.addKnownEnlightementCenter(rotationLocation, Team.NEUTRAL);
                            }
                            break;
                    }
                }
                prev = current;
            } else {
                // remove from SLL - TODO: Should we unregister some EC locations?
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

    static class ECNode {
        int id;
        MapLocation location;
        MapLocation nearestEnemy;
        int lastHeartbeatTurn = -1;
        ECNode next;
        public ECNode(int id, MapLocation location, ECNode next) {
            this.id = id;
            this.location = location;
            this.next = next;
        }
    }
}
