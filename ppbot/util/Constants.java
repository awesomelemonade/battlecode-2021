package ppbot.util;
import battlecode.common.*;

public class Constants {
    public static Team ALLY_TEAM;
    public static Team ENEMY_TEAM;
    public static final int MIN_ROBOT_ID = 10000; // 14 bits to represent robot id (max id = 26384)
    public static RobotType MY_TYPE;
    public static MapLocation MY_LOCATION;
    public static void init(RobotController rc) {
        ALLY_TEAM = rc.getTeam();
        MY_TYPE = rc.getType();
        MY_LOCATION = rc.getLocation();
        ENEMY_TEAM = ALLY_TEAM.opponent();
    }
    public static final Direction[] CARDINAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
    };
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
    public static final int[] ORDINAL_OFFSET_X = {0, 1, 1, 1, 0, -1, -1, -1};
    public static final int[] ORDINAL_OFFSET_Y = {-1, -1, 0, 1, 1, 1, 0, -1};
    public static final RobotType[] SPAWNABLE_ROBOTS = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };
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
}