package combobot3.util;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import combobot3.EnlightenmentCenter;

public class CentralCommunication {
    private static RobotController rc;
    private static int receivedInfoCount = 0;
    private static double guessX = 0;
    private static double guessY = 0;
    private static double centroidX = 0;
    private static double centroidY = 0;
    private static MapLocation enemyGuess;
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
    private static final int UNIT_LIST_MAX_SIZE = 60;

    public static final int DO_NOTHING_FLAG = 0b0000000_0000000_10000_10000;
    // rotate between [heartbeat - self], [friendly ec], [enemy ec], [neutral ec]
    public static final int ROTATION_SHIFT_X = 17;
    public static final int ROTATION_SHIFT_Y = 10;
    public static final int ROTATION_MASK = 0b0111_1111;
    public static final int ROTATION_OFFSET = 64;
    public static final int ROTATION_INFO_MASK = 0b0001_1111_1111; // 9 bits
    public static final int ROTATION_INFO_SHIFT = 1;

    // Communicate an enemy that has no units for some distance squared
    // 1 bit differentiate, 14 bits for position, 6 bits for distance, 3 bits conviction?
    public static final int ENEMY_SHIFT_X = 17;
    public static final int ENEMY_SHIFT_Y = 10;
    public static final int ENEMY_MASK = 0b0111_1111;
    public static final int ENEMY_OFFSET = 64;
    public static final int ENEMY_DISTANCE_MASK = 0b0011_1111;
    public static final int ENEMY_DISTANCE_SHIFT = 1;


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
                        CentralUnitTracker.handleEnemyUnit(specifiedLocation, type, conviction, current.type);
                        // Enemy Prediction
                        if (receivedInfoCount < 1 && EnlightenmentCenter.initialEC) {
                            int tempDx = specifiedLocation.x - current.location.x;
                            int tempDy = specifiedLocation.y - current.location.y;
                            guessX += tempDx;
                            guessY += tempDy;
                            int tempDx2 = (current.location.x - Cache.MY_LOCATION.x);
                            int tempDy2 = (current.location.y - Cache.MY_LOCATION.y);
                            double mag = Math.sqrt(tempDx2 * tempDx2 + tempDy2 * tempDy2);
                            double correctionX = tempDx2 / 3.0;
                            double correctionY = tempDy2 / 3.0;
                            guessX += correctionX;
                            guessY += correctionY;
                            if (receivedInfoCount == 0) {
                                int symmetry = Util.findSymmetry(guessX, guessY);
                                int symmetry2 = Util.findSymmetry2(guessX, guessY, symmetry);
                                guessX = Constants.ORDINAL_OFFSET_X[symmetry] * 61;
                                guessY = Constants.ORDINAL_OFFSET_Y[symmetry] * 61;

                                MapLocation guessLocation = new MapLocation((int)(Cache.MY_LOCATION.x + guessX), (int)(Cache.MY_LOCATION.y + guessY));
                                MapInfo.enemySlandererLocations.add(guessLocation, 300);
                                guessX = Constants.ORDINAL_OFFSET_X[symmetry2] * 61;
                                guessY = Constants.ORDINAL_OFFSET_Y[symmetry2] * 61;

                                guessLocation = new MapLocation((int)(Cache.MY_LOCATION.x + guessX), (int)(Cache.MY_LOCATION.y + guessY));
                                // MapInfo.enemySlandererLocations.add(guessLocation, 300);
                            }
                            receivedInfoCount++;
                        }
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
        Util.println("Handled " + count + " robots (" + (end - start) + " bytecodes)");
        // register any new ally robots (either though vision or building)
        start = Clock.getBytecodeNum();
        if (unitListSize < UNIT_LIST_MAX_SIZE) {
            for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
                RobotInfo ally = Cache.ALLY_ROBOTS[i];
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
        Util.println("Registered Robots (" + (end - start) + " bytecodes, " + Cache.ALLY_ROBOTS.length + " robots)");
    }
    static CentralUnitTracker.EnemyInfo enemy;
    public static void postLoop() throws GameActionException {
        int start = Clock.getBytecodeNum();
        enemy = CentralUnitTracker.calculateBroadcastedEnemy();
        int end = Clock.getBytecodeNum();
        if (enemy != null) {
            System.out.println("Found Enemy (" + (end - start) + " bytecodes) at " + enemy.location + " [r^2 = " + enemy.closestAllyDistanceSquared + "]");
            Util.setIndicatorLine(Cache.MY_LOCATION, enemy.location, 0, 255, 0);
        }
        if (enemy == null) {
            setRotationFlag();
        } else {
            setEnemyFlag();
        }
    }
    public static void setEnemyFlag() throws GameActionException {
        int flag = 1;

        int enemyDx = enemy.location.x - Cache.MY_LOCATION.x + ENEMY_OFFSET;
        int enemyDy = enemy.location.y - Cache.MY_LOCATION.y + ENEMY_OFFSET;
        flag = flag | (enemyDx << ENEMY_SHIFT_X) | (enemyDy << ENEMY_SHIFT_Y);

        flag |= Math.min(ENEMY_DISTANCE_MASK, (int) Math.sqrt(enemy.closestAllyDistanceSquared)) << ENEMY_DISTANCE_SHIFT;

        rc.setFlag(flag ^ DO_NOTHING_FLAG);
    }
    public static void setRotationFlag() throws GameActionException {
        // set flag
        int flag = 0;
        // 7 bits each on relative x location and relative y location,
        // 9 bits for info (ex: conviction)
        MapLocation rotationLocation = null;
        int info = 0;
        switch (Cache.TURN_COUNT % 5) {
            case 0: // heartbeat
                rotationLocation = Cache.MY_LOCATION;
                info = Math.min(ROTATION_INFO_MASK, rc.getConviction());
                break;
            case 1: // [ally ec]
                EnlightenmentCenterList.EnlightenmentCenterListNode allyEC =
                        MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getNext().orElse(null);
                if (allyEC != null) {
                    rotationLocation = allyEC.location;
                    info = Math.min(ROTATION_INFO_MASK, allyEC.lastKnownConviction);
                }
                break;
            case 2: // [enemy ec]
                EnlightenmentCenterList.EnlightenmentCenterListNode enemyEC =
                        MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getNext().orElse(null);
                if (enemyEC != null) {
                    rotationLocation = enemyEC.location;
                    info = Math.min(ROTATION_INFO_MASK, enemyEC.lastKnownConviction);
                }
                break;
            case 3: // [neutral ec]
                EnlightenmentCenterList.EnlightenmentCenterListNode neutralEC =
                        MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).getNext().orElse(null);
                if (neutralEC != null) {
                    rotationLocation = neutralEC.location;
                    info = Math.min(ROTATION_INFO_MASK, neutralEC.lastKnownConviction);
                }
                break;
            case 4: // [enemy slanderers]
                rotationLocation = MapInfo.enemySlandererLocations.getRandomLocation().orElse(null);
                break;
        }
        flag |= (info << ROTATION_INFO_SHIFT);
        if (rotationLocation != null) {
            int rotationDx = rotationLocation.x - Cache.MY_LOCATION.x + ROTATION_OFFSET;
            int rotationDy = rotationLocation.y - Cache.MY_LOCATION.y + ROTATION_OFFSET;
            flag = flag | (rotationDx << ROTATION_SHIFT_X) | (rotationDy << ROTATION_SHIFT_Y);
        }
        rc.setFlag(flag ^ DO_NOTHING_FLAG);
    }
}
