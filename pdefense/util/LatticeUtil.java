package pdefense.util;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class LatticeUtil {
    private static RobotController rc;
    public static void init(RobotController rc) {
        LatticeUtil.rc = rc;
    }
    private static final int[][] bfs20DX = {
            {-1, 0, 1, 2, 1, 0, -1, -2, -3, -2, -1, 0, 1, 2, 3, 4, 3, 2, 1, 0, -1, -2, -3, -4, -4, -3, -2, 2, 3, 4, 4, 3, 2, -2, -3, -4},
            {0, 1, 0, -1, -2, -1, 0, 1, 2, 3, 2, 1, 0, -1, -2, -3, -4, -3, -2, -1, 1, 2, 3, 4, 4, 3, 2, 1, -1, -2, -3, -4}
    }; // 20 = 20 radius squared = slanderer vision
    private static final int[][] bfs20DY = {
            {-1, -2, -1, 0, 1, 2, 1, 0, -1, -2, -3, -4, -3, -2, -1, 0, 1, 2, 3, 4, 3, 2, 1, 0, -2, -3, -4, -4, -3, -2, 2, 3, 4, 4, 3, 2},
            {-1, 0, 1, 0, -1, -2, -3, -2, -1, 0, 1, 2, 3, 2, 1, 0, -1, -2, -3, -4, -4, -3, -2, -1, 1, 2, 3, 4, 4, 3, 2, 1}
    };
    public static MapLocation getClosestLatticeLocation(MapLocation location) throws GameActionException {
        int parity = (location.x + location.y) % 2;
        int[] dx = bfs20DX[parity];
        int[] dy = bfs20DY[parity];
        for (int i = 0; i < dx.length; i++) {
            MapLocation candidate = location.translate(dx[i], dy[i]);
            if (rc.onTheMap(candidate) && rc.senseRobotAtLocation(candidate) == null) {
                return candidate;
            }
        }
        return null;
    }
    public static boolean isLatticeLocation(MapLocation location) {
        return (location.x + location.y) % 2 == 0;
    }
}
