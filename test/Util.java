package test;
import battlecode.common.*;

import static test.RobotPlayer.rc;

public class Util {
	public static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
  public static final int[] directionOffX = {0, 1, 1, 1, 0, -1, -1, -1};
  public static final int[] directionOffY = {-1, -1, 0, 1, 1, 1, 0, -1};

	public static MapLocation findSpawnEC() throws GameActionException {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
		for (RobotInfo r : nearbyRobots) {
			if (r.getType() == RobotType.ENLIGHTENMENT_CENTER && r.getTeam() == rc.getTeam()) {
				return r.getLocation();
			}
		}
		return null;
	}

	public static int packSigned128(int value) { // 8 bit
		if (value < 0) {
			return 128 - value;
		}
		return value;
	}

	public static int unpackSigned128(int value) { // 8 bit
		if ((value & 255) >= 128) {
			return 128 - (value & 255);
		}
		return (value & 255);
	}
}