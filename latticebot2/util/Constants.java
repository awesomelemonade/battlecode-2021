package latticebot2.util;

import battlecode.common.*;

public class Constants {
    public static Team ALLY_TEAM;
    public static Team ENEMY_TEAM;
    public static final int MIN_ROBOT_ID = 10000; // 14 bits to represent robot id (max (practical) id = 26384)
    public static final int MAX_MAP_SIZE = 64;
    public static final int MAX_DISTANCE_SQUARED = (MAX_MAP_SIZE - 1) * (MAX_MAP_SIZE - 1);
    public static final int POLITICIAN_EMPOWER_PENALTY = 11;
    public static int SENSE_BOX_RADIUS;
    public static MapLocation SPAWN;
    public static MapLocation SPAWNEC;

    public static void init(RobotController rc) throws GameActionException {
        ALLY_TEAM = rc.getTeam();
        ENEMY_TEAM = ALLY_TEAM.opponent();
        SPAWN = rc.getLocation();
        switch(rc.getType()) {
            case ENLIGHTENMENT_CENTER:
                SENSE_BOX_RADIUS = 6;
                break;
            case MUCKRAKER:
                SENSE_BOX_RADIUS = 5;
                break;
            case POLITICIAN:
                SENSE_BOX_RADIUS = 5;
                break;
            case SLANDERER:
                SENSE_BOX_RADIUS = 4;
        }

        // if am not EC, find the EC that spawned us
        // note that cache values are not actually initialized yet, so this is slightly inefficient bytecode-wise
        if (rc.getType() != RobotType.ENLIGHTENMENT_CENTER) {
            MapLocation my_loc = Constants.SPAWN;
            for (Direction dir : Constants.ORDINAL_DIRECTIONS) {
                MapLocation new_loc = my_loc.add(dir);
                if (rc.canSenseLocation(my_loc.add(dir))) {
                    RobotInfo neighbor = rc.senseRobotAtLocation(new_loc);
                    if (neighbor != null && neighbor.team == rc.getTeam() && neighbor.type == RobotType.ENLIGHTENMENT_CENTER) {
                        // friendly enlightenment center
                        SPAWNEC = neighbor.location;
                        Communication.updateKnownUnits(neighbor.ID);
                    }
                }
            }
        } else {
            // if i am ec, just set to myself
            SPAWNEC = SPAWN;
        }

    }
    /*public static final Direction[] CARDINAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
    };*/
    public static final Direction[] ORDINAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    /*public static final int[] ORDINAL_OFFSET_X = {0, 1, 1, 1, 0, -1, -1, -1};
    public static final int[] ORDINAL_OFFSET_Y = {-1, -1, 0, 1, 1, 1, 0, -1};
    public static final RobotType[] SPAWNABLE_ROBOTS = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };*/
    private static final Direction[][] ATTEMPT_ORDER = new Direction[][] {
            // NORTH
            {Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST, Direction.WEST, Direction.EAST, Direction.SOUTHWEST, Direction.SOUTHEAST, Direction.SOUTH},
            // NORTHEAST
            {Direction.NORTHEAST, Direction.NORTH, Direction.EAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.WEST, Direction.SOUTH, Direction.SOUTHWEST},
            // EAST
            {Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.NORTH, Direction.SOUTH, Direction.NORTHWEST, Direction.SOUTHWEST, Direction.WEST},
            // SOUTHEAST
            {Direction.SOUTHEAST, Direction.EAST, Direction.SOUTH, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.NORTH, Direction.WEST, Direction.NORTHWEST},
            // SOUTH
            {Direction.SOUTH, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.EAST, Direction.WEST, Direction.NORTHEAST, Direction.NORTHWEST, Direction.NORTH},
            // SOUTHWEST
            {Direction.SOUTHWEST, Direction.SOUTH, Direction.WEST, Direction.SOUTHEAST, Direction.NORTHWEST, Direction.EAST, Direction.NORTH, Direction.NORTHEAST},
            // WEST
            {Direction.WEST, Direction.SOUTHWEST, Direction.NORTHWEST, Direction.SOUTH, Direction.NORTH, Direction.SOUTHEAST, Direction.NORTHEAST, Direction.EAST},
            // NORTHWEST
            {Direction.NORTHWEST, Direction.WEST, Direction.NORTH, Direction.SOUTHWEST, Direction.NORTHEAST, Direction.SOUTH, Direction.EAST, Direction.SOUTHEAST},
    };
    public static Direction[] getAttemptOrder(Direction direction) {
        return ATTEMPT_ORDER[direction.ordinal()];
    }
    // dx, dy for radius squared = 20, 69 coordinates
    public static final int[] FLOOD_OFFSET_X_20 = {
            0, 0, 1, 0, -1, -1, 0, 1, 2, 1, 0, -1, -2, -2, -1, 0, 1, 2, 3, 2, 1, 0, -1, -2, -3, -3, -2, -1, 0, 1, 2, 3, 4, 3, 2, 1, 0, -1, -2, -3, -4, -4, -3, -2, -1, 1, 2, 3, 4, 4, 3, 2, 1, -1, -2, -3, -4, -4, -3, -2, 2, 3, 4, 4, 3, 2, -2, -3, -4
    };
    public static final int[] FLOOD_OFFSET_Y_20 = {
            0, -1, 0, 1, 0, -1, -2, -1, 0, 1, 2, 1, 0, -1, -2, -3, -2, -1, 0, 1, 2, 3, 2, 1, 0, -1, -2, -3, -4, -3, -2, -1, 0, 1, 2, 3, 4, 3, 2, 1, 0, -1, -2, -3, -4, -4, -3, -2, -1, 1, 2, 3, 4, 4, 3, 2, 1, -2, -3, -4, -4, -3, -2, 2, 3, 4, 4, 3, 2
    };
}