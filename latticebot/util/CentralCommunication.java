package latticebot.util;

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
    private static MapLocation nearestEnemy;
    private static int nearestEnemyDistanceSquared = Integer.MAX_VALUE;
    public static void init(RobotController rc) {
        CentralCommunication.rc = rc;
        registered = new BooleanArray();
    }
    private static BooleanArray registered;
    // Stored as singly linked list
    static class Node {
        MapLocation location;
        int id;
        Node next;
        public Node(MapLocation location, int id, Node next) {
            this.location = location;
            this.id = id;
            this.next = next;
        }
    }
    private static Node unitListHead = null;
    private static int unitListSize = 0;
    private static final int UNIT_LIST_MAX_SIZE = 150;
    // 14 bits: rotate between [minX, maxX], [minY, maxY], [friendly ec], [enemy ec], [neutral ec]
    public static final int ROTATION_SHIFT_X = 17;
    public static final int ROTATION_SHIFT_Y = 10;
    public static final int ROTATION_MASK = 0b0111_1111;
    public static final int ROTATION_OFFSET = 64;
    // 10 bits: broadcast nearest known enemy to this EC
    public static void loop() throws GameActionException {
        nearestEnemy = null;
        nearestEnemyDistanceSquared = Integer.MAX_VALUE;
        // find nearest enemy
        RobotInfo enemy = Util.getClosestEnemyRobot();
        if (enemy != null) {
            nearestEnemy = enemy.location;
            nearestEnemyDistanceSquared = Cache.MY_LOCATION.distanceSquaredTo(nearestEnemy);
        }
        int start = Clock.getBytecodeNum();
        // read flags of each known robot (delete from list if not alive)
        Node prev = null;
        Node current = unitListHead;
        int count = 0;
        while (current != null) {
            int id = current.id;
            if (rc.canGetFlag(id)) {
                int flag = rc.getFlag(id);
                // process info
                int dx = ((flag >> UnitCommunication.CURRENT_UNIT_OFFSET_X_SHIFT) & UnitCommunication.CURRENT_UNIT_OFFSET_MASK) - UnitCommunication.OFFSET_SHIFT;
                int dy = ((flag >> UnitCommunication.CURRENT_UNIT_OFFSET_Y_SHIFT) & UnitCommunication.CURRENT_UNIT_OFFSET_MASK) - UnitCommunication.OFFSET_SHIFT;
                if (dx == 0 && dy == 0) {
                    // TODO: communicate about map edge?
                } else {
                    MapLocation specifiedLocation = current.location.translate(dx, dy);
                    RobotType type = RobotType.values()[(flag >> UnitCommunication.CURRENT_UNIT_TYPE_SHIFT) & UnitCommunication.CURRENT_UNIT_TYPE_MASK];
                    int info = flag & UnitCommunication.CURRENT_UNIT_INFO_MASK;
                    if (type == RobotType.ENLIGHTENMENT_CENTER) {
                        Team team = Team.values()[info];
                        // TODO
                        if (team == Constants.ENEMY_TEAM) {
                            int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(specifiedLocation);
                            if (distanceSquared < nearestEnemyDistanceSquared) {
                                nearestEnemyDistanceSquared = distanceSquared;
                                nearestEnemy = specifiedLocation;
                            }
                        }
                    } else {
                        // info = influence of that particular unit
                        int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(specifiedLocation);
                        if (distanceSquared < nearestEnemyDistanceSquared) {
                            nearestEnemyDistanceSquared = distanceSquared;
                            nearestEnemy = specifiedLocation;
                        }
                    }
                }
                // update location
                Direction dir = Direction.values()[flag >> UnitCommunication.CURRENT_DIRECTION_SHIFT];
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
                        unitListHead = new Node(ally.getLocation(), id, unitListHead);
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
        if (nearestEnemy == null) {
            // set dx, dy to be (0, 0) to signal that there are no nearestEnemies
            nearestEnemy = Cache.MY_LOCATION;
            rc.setIndicatorDot(Cache.MY_LOCATION, 255, 255, 0); // yellow
        } else {
            rc.setIndicatorDot(nearestEnemy, 0, 255, 0); // green
        }
        // 5 bits on relative x location and relative y location
        int dx = Math.max(0, Math.min(NEAREST_ENEMY_MASK, nearestEnemy.x - Cache.MY_LOCATION.x + NEAREST_ENEMY_OFFSET));
        int dy = Math.max(0, Math.min(NEAREST_ENEMY_MASK, nearestEnemy.y - Cache.MY_LOCATION.y + NEAREST_ENEMY_OFFSET));
        flag = flag | (dx << NEAREST_ENEMY_X_SHIFT) | dy;
        // 7 bits on relative x location and relative y location
        MapLocation rotationLocation = null;
        switch (rc.getRoundNum() % 5) {
            case 0: // [minX, minY]
                //flag |= minX << ROTATION_SHIFT_X;
                //flag |= minY << ROTATION_SHIFT_Y;
                break;
            case 1: // [maxX, maxY]
                //flag |= maxX << ROTATION_SHIFT_X;
                //flag |= maxY << ROTATION_SHIFT_Y;
                break;
            case 2: // [friendly ec]
                break;
            case 3: // [enemy ec]
                break;
            case 4: // [neutral ec]
                break;
        }
        if (rotationLocation != null) {
            // TODO
        }
        rc.setFlag(flag);
    }
}
