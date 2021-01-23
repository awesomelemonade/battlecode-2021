package greedybot.util;

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
    private static Comparator<RobotInfo> importantRobotComparator;

    public static void init(RobotController rc) {
        UnitCommunication.rc = rc;
        importantRobotComparator = Comparator.comparingInt(r -> {
            int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(r.getLocation());
            switch (r.getType()) {
                case ENLIGHTENMENT_CENTER:
                    if (r.getTeam() == Constants.ALLY_TEAM) {
                        return distanceSquared;
                    } else if (r.getTeam() == Constants.ENEMY_TEAM) {
                        return distanceSquared - 40000;
                    } else {
                        if (MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).contains(r.getLocation())) {
                            return distanceSquared;
                        } else {
                            return distanceSquared - 40000;
                        }
                    }
                case POLITICIAN:
                    return distanceSquared - 10000;
                case MUCKRAKER:
                    return distanceSquared - 20000;
                case SLANDERER: // Remember: Only muckrakers can see this
                    return distanceSquared - 30000;
                default:
                    throw new IllegalStateException("Unknown Type: " + r.getType());
            }
        });
    }

    public static final int OFFSET_SHIFT = 7;
    public static final int CURRENT_DIRECTION_SHIFT = 20;
    public static final int CURRENT_UNIT_OFFSET_X_SHIFT = 16;
    public static final int CURRENT_UNIT_OFFSET_Y_SHIFT = 12;
    public static final int CURRENT_UNIT_OFFSET_MASK = 0b1111;
    public static final int CURRENT_UNIT_TYPE_SHIFT = 10;
    public static final int CURRENT_UNIT_TYPE_MASK = 0b11;
    public static final int CURRENT_UNIT_INFO_MASK = 0b11_1111_1111;
    public static final int CURRENT_EC_TEAM_SHIFT = 8;
    public static final int CURRENT_EC_TEAM_MASK = 0b11;
    public static final int CURRENT_EC_CONVICTION_MASK = 0b1111_1111;
    public static final int DO_NOTHING_FLAG = 0b1000_0111_0111_0000_0000_0000;
    public static final int CLEAR_FLAG = 0b0000_0111_0111_0000_0000_0000;
    private static int currentFlag = DO_NOTHING_FLAG;
    public static MapLocation closestCommunicatedEnemyToKite; // Currently used to by slanderers
    public static int closestCommunicatedEnemyDistanceSquared;
    public static int closestCommunicatedEnemyConviction;

    private static void checkCloseEnemySlanderer(MapLocation enemy) {
        int enemyDistanceSquared = Cache.MY_LOCATION.distanceSquaredTo(enemy);
        if (enemyDistanceSquared < closestCommunicatedEnemyDistanceSquared) {
            closestCommunicatedEnemyDistanceSquared = enemyDistanceSquared;
            closestCommunicatedEnemyToKite = enemy;
        }
    }

    private static void checkCloseEnemyPolitician(MapLocation enemy, int conviction) {
        int currentDamage = rc.getConviction() - 10;
        if (currentDamage > conviction && conviction * 10 > 3 * currentDamage) {
            int enemyDistanceSquared = Cache.MY_LOCATION.distanceSquaredTo(enemy);
            if (enemyDistanceSquared < closestCommunicatedEnemyDistanceSquared) {
                closestCommunicatedEnemyDistanceSquared = enemyDistanceSquared;
                closestCommunicatedEnemyToKite = enemy;
                closestCommunicatedEnemyConviction = conviction;
            }
        }
    }

    public static final int CURRENT_DIRECTION_CENTER_SLANDERER = 9;

    public static boolean isPotentialSlanderer(RobotInfo robot) {
        try {
            return robot.getType() == RobotType.POLITICIAN && ((rc.getFlag(robot.getID())
                    ^ DO_NOTHING_FLAG) >> CURRENT_DIRECTION_SHIFT) == CURRENT_DIRECTION_CENTER_SLANDERER;
        } catch (GameActionException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static void loop() throws GameActionException {
        rc.setFlag(0); // in case we run out of bytecodes
        currentFlag = CLEAR_FLAG;
        // Prioritize
        // 1. neutral/enemy enlightenment centers
        // 2. enemy slanderers
        // 3. enemy muckrakers
        // 4. enemy politicians
        // 5. ally enlightenment centers
        if (Cache.ALLY_ROBOTS.length >= 15) {
            LambdaUtil.or(LambdaUtil.arraysStreamMin(Cache.ENEMY_ROBOTS, Cache.NEUTRAL_ROBOTS,
                    importantRobotComparator), () ->
                    // Broadcast known ally center
                    MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getClosestLocation()
                            .map(location ->
                                    Pathfinder.moveDistance(Cache.MY_LOCATION, location) <= 7 ?
                                            new RobotInfo(-1, Constants.ALLY_TEAM, RobotType.ENLIGHTENMENT_CENTER,
                                                    -1, -1, location) : null
                            )
            ).ifPresent(r -> {
                Util.setIndicatorLine(Cache.MY_LOCATION, r.getLocation(), 255, 255, 0); // yellow
                if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    MapInfo.addKnownEnlightenmentCenter(r.getTeam(), r.getLocation(), r.getConviction());
                }
                setFlag(r);
            });
        } else {
            LambdaUtil.arraysStreamMin(Cache.ALL_ROBOTS,
                    r -> r.getTeam() != Constants.ALLY_TEAM || r.getType() == RobotType.ENLIGHTENMENT_CENTER,
                    importantRobotComparator).ifPresent(r -> {
                        Util.setIndicatorLine(Cache.MY_LOCATION, r.getLocation(), 255, 255, 0); // yellow
                        if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                            MapInfo.addKnownEnlightenmentCenter(r.getTeam(), r.getLocation(), r.getConviction());
                        }
                        setFlag(r);
                    });
        }

        closestCommunicatedEnemyToKite = null;
        closestCommunicatedEnemyDistanceSquared = Integer.MAX_VALUE;
        closestCommunicatedEnemyConviction = Integer.MAX_VALUE;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
            RobotInfo ally = Cache.ALLY_ROBOTS[i];
            RobotType type = ally.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                registerOurTeamEC(ally);
            } else {
                // Don't process enemies from nearby units if slanderer is on turn count 1
                // this is turn 1 initialization bytecode optimization
                if (Cache.TURN_COUNT > 1 || type != RobotType.SLANDERER) {
                    processEnemiesFromNearbyUnits(ally);
                }
            }
        }
        processFlagsFromECs();
        if (rc.getType() == RobotType.POLITICIAN || rc.getType() == RobotType.MUCKRAKER) {
            // To save bytecodes, we don't run this for slanderers
            // TODO: Is this even necessary anymore?
            for (Team team : Team.values()) {
                MapInfo.getKnownEnlightenmentCenterList(team).removeIf(loc -> {
                    try {
                        if (MapInfo.mapMinX != MapInfo.MAP_UNKNOWN_EDGE && loc.x < MapInfo.mapMinX)
                            return true;
                        if (MapInfo.mapMinY != MapInfo.MAP_UNKNOWN_EDGE && loc.y < MapInfo.mapMinY)
                            return true;
                        if (MapInfo.mapMaxX != MapInfo.MAP_UNKNOWN_EDGE && loc.x > MapInfo.mapMaxX)
                            return true;
                        if (MapInfo.mapMaxY != MapInfo.MAP_UNKNOWN_EDGE && loc.y > MapInfo.mapMaxY)
                            return true;
                        if (rc.canSenseLocation(loc)) {
                            RobotInfo robot = rc.senseRobotAtLocation(loc);
                            if (robot == null || robot.getType() != RobotType.ENLIGHTENMENT_CENTER
                                    || robot.getTeam() != team) {
                                return true;
                            }
                        }
                    } catch (GameActionException ex) {
                        throw new IllegalStateException(ex);
                    }
                    return false;
                });
            }
        }
    }

    public static void postLoop() throws GameActionException {
        if (rc.getType() == RobotType.SLANDERER && Cache.lastDirection == Direction.CENTER) {
            rc.setFlag(
                    ((CURRENT_DIRECTION_CENTER_SLANDERER << CURRENT_DIRECTION_SHIFT) | currentFlag) ^ DO_NOTHING_FLAG);
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
            currentFlag |= unit.getTeam().ordinal() << CURRENT_EC_TEAM_SHIFT; // 2 bits
            // 8 bits - neutral ec's have max 500 conviction - divide by 2 rounded up
            currentFlag |= Math.min(CURRENT_EC_CONVICTION_MASK, (unit.getConviction() + 1) / 2);
        } else {
            // specify conviction
            currentFlag |= Math.min(unit.getConviction(), CURRENT_UNIT_INFO_MASK); // 10 bits
        }
    }

    public static void clearFlag() {
        currentFlag = CLEAR_FLAG;
    }

    public static final int UNIT_INFO_BITMASK = 0b1111_1111_1111_1111_1111; // 20 bits

    /**
     * if this is a slanderer and we see a muckraker, run away! if this is a
     * politician and we see a muckraker, run towards it!
     *
     * @param robot
     * @return a location of a muckraker retrieved from flag, otherwise null
     * @throws GameActionException
     */
    public static void processEnemiesFromNearbyUnits(RobotInfo robot) throws GameActionException {
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
                return;
            }
            MapLocation specifiedLocation = prevLocation.translate(dx, dy);
            RobotType type = RobotType.values()[(flag >> CURRENT_UNIT_TYPE_SHIFT) & CURRENT_UNIT_TYPE_MASK];
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                int teamOrdinal = (flag >> CURRENT_EC_TEAM_SHIFT) & CURRENT_EC_TEAM_MASK;
                int conviction = (flag & CURRENT_EC_CONVICTION_MASK) * 2;
                Team team = Team.values()[teamOrdinal];
                if (team == Constants.ENEMY_TEAM) {
                    if (rc.getType() == RobotType.SLANDERER) {
                        checkCloseEnemySlanderer(specifiedLocation);
                    }
                }
                if (rc.getType() != RobotType.SLANDERER) {
                    // Conserve bytecodes for slanderers
                    MapInfo.addKnownEnlightenmentCenter(team, specifiedLocation, conviction);
                }
            } else {
                int conviction = flag & CURRENT_UNIT_INFO_MASK;
                // we see type at specifiedLocation
                if (type == RobotType.MUCKRAKER) {
                    // we see enemy!!
                    if (rc.getType() == RobotType.SLANDERER) {
                        checkCloseEnemySlanderer(specifiedLocation);
                    }
                    if (rc.getType() == RobotType.POLITICIAN) {
                        checkCloseEnemyPolitician(specifiedLocation, conviction);
                    }
                }
            }
        }
    }

    public static void processFlagsFromECs() throws GameActionException {
        ECNode prev = null;
        ECNode current = ecListHead;
        while (current != null) {
            int id = current.id;
            if (rc.canGetFlag(id)) {
                int flag = rc.getFlag(id) ^ CentralCommunication.DO_NOTHING_FLAG;
                MapLocation ecLocation = current.location;
                int rotationDx = (flag >> CentralCommunication.ROTATION_SHIFT_X) - CentralCommunication.ROTATION_OFFSET;
                int rotationDy = ((flag >> CentralCommunication.ROTATION_SHIFT_Y) & CentralCommunication.ROTATION_MASK)
                        - CentralCommunication.ROTATION_OFFSET;
                int rotationInfo = flag & CentralCommunication.ROTATION_INFO_MASK;
                if (rotationDx == 0 && rotationDy == 0) {
                    current.lastHeartbeatTurn = rc.getRoundNum();
                }
                if (current.lastHeartbeatTurn != -1) {
                    MapLocation rotationLocation = ecLocation.translate(rotationDx, rotationDy);
                    switch ((rc.getRoundNum() - current.lastHeartbeatTurn) % 5) {
                        case 0: // heartbeat
                            Util.setIndicatorDot(rotationLocation, 255, 0, 255); // magenta
                            MapInfo.addKnownEnlightenmentCenter(Constants.ALLY_TEAM, rotationLocation, rotationInfo);
                            break;
                        case 1: // [ally ec]
                            if (rotationDx != -CentralCommunication.ROTATION_OFFSET
                                    && rotationDy != -CentralCommunication.ROTATION_OFFSET) {
                                MapInfo.addKnownEnlightenmentCenter(Constants.ALLY_TEAM, rotationLocation, rotationInfo);
                            }
                            break;
                        case 2: // [enemy ec]
                            if (rotationDx != -CentralCommunication.ROTATION_OFFSET
                                    && rotationDy != -CentralCommunication.ROTATION_OFFSET) {
                                MapInfo.addKnownEnlightenmentCenter(Constants.ENEMY_TEAM, rotationLocation, rotationInfo);
                            }
                            break;
                        case 3: // [neutral ec]
                            if (rotationDx != -CentralCommunication.ROTATION_OFFSET
                                    && rotationDy != -CentralCommunication.ROTATION_OFFSET) {
                                MapInfo.addKnownEnlightenmentCenter(Team.NEUTRAL, rotationLocation, rotationInfo);
                            }
                            break;
                        case 4: // [enemy slanderers]
                            if (rc.getType() == RobotType.MUCKRAKER) {
                                // Only muckrakers need this
                                if (rotationDx != -CentralCommunication.ROTATION_OFFSET
                                        && rotationDy != -CentralCommunication.ROTATION_OFFSET) {
                                    if (!MapInfo.enemySlandererLocations.contains(rotationLocation)) {
                                        MapInfo.enemySlandererLocations.add(rotationLocation, Cache.TURN_COUNT);
                                    }
                                }
                            }
                            break;
                    }
                }
                prev = current;
            } else {
                // we lost control of EC - remove from SLL - Set EC as enemy team
                MapInfo.addKnownEnlightenmentCenter(Constants.ENEMY_TEAM, current.location, -1);
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
