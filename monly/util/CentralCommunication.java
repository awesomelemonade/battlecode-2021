package monly.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

import monly.EnlightenmentCenter;

public class CentralCommunication {
    private static RobotController rc;
    public static MapLocation nearestEnemy;
    public static int nearestEnemyDistanceSquared = Integer.MAX_VALUE;
    public static RobotType nearestEnemyType;
    public static int nearestEnemyConviction = 0;
    private static int receivedInfoCount = 0;
    private static double guessX = 0;
    private static double guessY = 0;
    private static double centroidX = 0;
    private static double centroidY = 0;
    private static MapLocation enemyGuess;
    public static void init(RobotController rc) {
        CentralCommunication.rc = rc;
        registered = new BooleanArray();
    }
    private static BooleanArray registered;
    // Stored as singly linked list
    static class UnitListNode {
        MapLocation location;
        int id;
        UnitListNode next;
        public UnitListNode(MapLocation location, int id, UnitListNode next) {
            this.location = location;
            this.id = id;
            this.next = next;
        }
    }
    private static UnitListNode unitListHead = null;
    private static int unitListSize = 0;
    private static final int UNIT_LIST_MAX_SIZE = 120;

    public static final int DO_NOTHING_FLAG = 0b0000000_0000000_10000_10000;
    // 14 bits: rotate between [minX, maxX], [minY, maxY], [friendly ec], [enemy ec], [neutral ec]
    public static final int ROTATION_SHIFT_X = 17;
    public static final int ROTATION_SHIFT_Y = 10;
    public static final int ROTATION_MASK = 0b0111_1111;
    public static final int ROTATION_OFFSET = 64;
    // 10 bits: broadcast nearest known enemy to this EC
    public static void loop() throws GameActionException {
        rc.setFlag(0); // in case we run out of bytecodes
        // find nearest enemy
        RobotInfo enemy = Util.getClosestEnemyRobot();
        if (enemy == null) {
            nearestEnemy = null;
            nearestEnemyDistanceSquared = Integer.MAX_VALUE;
            nearestEnemyType = null;
            nearestEnemyConviction = 0;
        } else {
            nearestEnemy = enemy.getLocation();
            nearestEnemyDistanceSquared = Cache.MY_LOCATION.distanceSquaredTo(nearestEnemy);
            nearestEnemyType = enemy.getType();
            nearestEnemyConviction = enemy.getInfluence();
        }
        // read flags of each known robot (delete from list if not alive)
        UnitListNode prev = null;
        UnitListNode current = unitListHead;
        while (current != null) {
            int id = current.id;
            if (rc.canGetFlag(id)) {
                int flag = rc.getFlag(id) ^ UnitCommunication.DO_NOTHING_FLAG;
                // process info
                int dx = ((flag >> UnitCommunication.CURRENT_UNIT_OFFSET_X_SHIFT) & UnitCommunication.CURRENT_UNIT_OFFSET_MASK) - UnitCommunication.OFFSET_SHIFT;
                int dy = ((flag >> UnitCommunication.CURRENT_UNIT_OFFSET_Y_SHIFT) & UnitCommunication.CURRENT_UNIT_OFFSET_MASK) - UnitCommunication.OFFSET_SHIFT;
                if (dx == 0 && dy == 0) {
                    // TODO: communicate about map edge?
                } else {
                    MapLocation specifiedLocation = current.location.translate(dx, dy);
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
                        int info = flag & UnitCommunication.CURRENT_UNIT_INFO_MASK;
                        int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(specifiedLocation);
                        if (distanceSquared < nearestEnemyDistanceSquared) {
                            nearestEnemyDistanceSquared = distanceSquared;
                            nearestEnemy = specifiedLocation;
                            nearestEnemyType = type;
                            nearestEnemyConviction = info;
                        }
                        if (receivedInfoCount < 1 && EnlightenmentCenter.initialEC) {
                            int tempDx = specifiedLocation.x - current.location.x;
                            int tempDy = specifiedLocation.y - current.location.y;
                            guessX += tempDx;
                            guessY += tempDy;
                            int tempDx2 = (current.location.x - Cache.MY_LOCATION.x);
                            int tempDy2 = (current.location.y - Cache.MY_LOCATION.y);
                            double mag = Math.sqrt(tempDx2 * tempDx2 + tempDy2 * tempDy2);
                            double correctionX = tempDx2 / 6.0;
                            double correctionY = tempDy2 / 6.0;
                            guessX += correctionX;
                            guessY += correctionY;
                            System.out.println("VEC1: " + tempDx + " " + tempDy);
                            System.out.println("VEC2: " + correctionX + " " + correctionY);
                            System.out.println("GUESS VEC " + guessX + " " + guessY);
                            if (receivedInfoCount == 0) {
                                int symmetry = Util.findSymmetry(guessX, guessY);
                                int symmetry2 = Util.findSymmetry2(guessX, guessY, symmetry);
                                guessX = Constants.ORDINAL_OFFSET_X[symmetry] * 61;
                                guessY = Constants.ORDINAL_OFFSET_Y[symmetry] * 61;

                                MapLocation guessLocation = new MapLocation((int)(Cache.MY_LOCATION.x + guessX), (int)(Cache.MY_LOCATION.y + guessY));
                                System.out.println("GUESS " + guessX + " " + guessY);
                                MapInfo.enemySlandererLocations.add(guessLocation, 300);
                                guessX = Constants.ORDINAL_OFFSET_X[symmetry2] * 61;
                                guessY = Constants.ORDINAL_OFFSET_Y[symmetry2] * 61;

                                guessLocation = new MapLocation((int)(Cache.MY_LOCATION.x + guessX), (int)(Cache.MY_LOCATION.y + guessY));
                                System.out.println("SECOND " + guessX + " " + guessY);
                                // MapInfo.enemySlandererLocations.add(guessLocation, 300);
                            }
                            receivedInfoCount++;
                        }
                    }
                }
                // update location
                Direction dir = Direction.values()[Math.min(8, flag >> UnitCommunication.CURRENT_DIRECTION_SHIFT)];
                current.location = current.location.add(dir);
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
        }
        // register any new ally robots (either though vision or building)
        if (unitListSize < UNIT_LIST_MAX_SIZE) {
            for (RobotInfo ally : Cache.ALLY_ROBOTS) {
                // some sort of set - allocate in chunks of 4096?
                if (ally.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                    int id = ally.getID();
                    if (!registered.getAndSetTrue(id - Constants.MIN_ROBOT_ID)) {
                        // register in singly linked list
                        unitListHead = new UnitListNode(ally.getLocation(), id, unitListHead);
                        unitListSize++;
                        if (unitListSize >= UNIT_LIST_MAX_SIZE) {
                            break;
                        }
                    }
                }
            }
        }
    }
    public static final int NEAREST_ENEMY_OFFSET = 16;
    public static final int NEAREST_ENEMY_X_SHIFT = 5;
    public static final int NEAREST_ENEMY_MASK = 0b1_1111;
    public static void postLoop() throws GameActionException {
        // set flag
        int flag = 0;
        if (nearestEnemy == null) {
            // set dx, dy to be (0, 0) to signal that there are no nearestEnemies
            nearestEnemy = Cache.MY_LOCATION;
            Util.setIndicatorDot(Cache.MY_LOCATION, 255, 255, 0); // yellow
        } else {
            Util.setIndicatorDot(nearestEnemy, 0, 255, 0); // green
        }
        // 5 bits on relative x location and relative y location
        int dx = nearestEnemy.x - Cache.MY_LOCATION.x + NEAREST_ENEMY_OFFSET;
        int dy = nearestEnemy.y - Cache.MY_LOCATION.y + NEAREST_ENEMY_OFFSET;
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
            case 4: // [locations of interest]
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
