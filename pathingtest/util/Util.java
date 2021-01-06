package pathingtest.util;

import java.util.*;

import battlecode.common.*;
import pathingtest.util.Cache;
import static pathingtest.util.Constants.*;
import pathingtest.util.Pair;

public class Util {
    private static RobotController rc;

    public static void init(RobotController rc) throws GameActionException {
        Util.rc = rc;
        Constants.init(rc);
        Cache.init(rc);
        MapInfo.init(rc);
    }

    public static void loop() throws GameActionException {
        Cache.loop();
        MapInfo.loop();
    }

    public static RobotType randomSpawnableRobotType() {
        return SPAWNABLE_ROBOTS[(int) (Math.random() * SPAWNABLE_ROBOTS.length)];
    }

    public static boolean tryBuildRobot(RobotType type, Direction direction, int influence) throws GameActionException {
        if (rc.canBuildRobot(type, direction, influence)) {
            rc.buildRobot(type, direction, influence);
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryBuildRobotTowards(RobotType type, Direction direction, int influence)
            throws GameActionException {
        for (Direction buildDirection : Constants.getAttemptOrder(direction)) {
            if (tryBuildRobot(type, buildDirection, influence)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryMove(Direction direction) throws GameActionException {
        if (rc.canMove(direction)) {
            rc.move(direction);
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryMoveTowards(Direction direction) throws GameActionException {
        for (Direction moveDirection : Constants.getAttemptOrder(direction)) {
            if (tryMove(moveDirection)) {
                return true;
            }
        }
        return false;
    }

    public static int squaredEuclidLength(int dx, int dy) {
        return dx * dx + dy * dy;
    }

    public static int squaredEuclidDistance(int x1, int y1, int x2, int y2) {
        return squaredEuclidLength(x1 - x2, y1 - y2);
    }

    public static int moveLength(int dx, int dy) {
        return Math.abs(dx) + Math.abs(dy);
    }

    public static int moveDistance(int x1, int y1, int x2, int y2) {
        return moveLength(x1 - x2, y1 - y2);
    }

    public static int moveDistance(MapLocation loc1, MapLocation loc2) {
        return moveDistance(loc1.x, loc1.y, loc2.x, loc2.y);
    }

    public static Direction offsetToDirection(int dx, int dy) throws GameActionException {
        switch (3 * (dx + 1) + (dy + 1)) {
            case 0:
                return Direction.SOUTHWEST;
            case 1:
                return Direction.WEST;
            case 2:
                return Direction.NORTHWEST;
            case 3:
                return Direction.SOUTH;
            case 4:
                return Direction.CENTER;
            case 5:
                return Direction.NORTH;
            case 6:
                return Direction.SOUTHEAST;
            case 7:
                return Direction.EAST;
            case 8:
                return Direction.NORTHEAST;
        }
        return null;
    }

    private static Pair<Direction, Double> findMoveSmart(MapLocation cur, MapLocation dest, int search_depth)
            throws GameActionException {
        if (search_depth == 0 || cur.equals(dest)) {
            return new Pair(null, 0.0);
        }

        double lowest_wait = 1e9;
        int cur_dis_euclid = cur.distanceSquaredTo(dest);
        int cur_dis_move = moveDistance(cur, dest);
        Direction best_dir = null;
        for (Direction dir : ORDINAL_DIRECTIONS) {
            MapLocation nextpos = cur.add(dir);
            if (nextpos.distanceSquaredTo(dest) >= cur_dis_euclid && moveDistance(nextpos, dest) >= cur_dis_move) {
                continue;
            }
            if (!rc.canSenseLocation(nextpos) || rc.isLocationOccupied(nextpos)) {
                continue;
            }

            double wait_time = 1.0 / rc.sensePassability(nextpos)
                    + findMoveSmart(nextpos, dest, search_depth - 1).second;
            if (wait_time < lowest_wait) {
                lowest_wait = wait_time;
                best_dir = dir;
            }
        }
        return new Pair(best_dir, lowest_wait);
    }

    public static boolean tryMoveSmart(MapLocation loc, int search_depth) throws GameActionException {
        Direction dir = findMoveSmart(Cache.MY_LOCATION, loc, search_depth).first;
        if (dir == null) {
            return false;
        }
        return tryMove(dir);
    }

    // does dijkstra within small range. probably too slow for practical use
    public static boolean tryMoveDijkstra(MapLocation loc) throws GameActionException {
        int range = 1;
        int boxlen = 2 * range + 1;
        boolean[][] vis = new boolean[boxlen][boxlen];
        double[][] dis = new double[boxlen][boxlen];
        Direction[][] dirTo = new Direction[boxlen][boxlen];
        for (int i = 0; i < boxlen; i++) {
            for (int j = 0; j < boxlen; j++) {
                MapLocation xyloc = Cache.MY_LOCATION.translate(i - range, j - range);
                if (!rc.canSenseLocation(xyloc))
                    continue;
                if (rc.isLocationOccupied(xyloc))
                    continue;
                dis[i][j] = 1e9;
                if (moveLength(i - range, j - range) <= 1) {
                    dirTo[i][j] = offsetToDirection(i - range, j - range);
                }
            }
        }

        PriorityQueue<Pair<Double, Integer>> pq = new PriorityQueue<>(8, new Comparator<Pair<Double, Integer>>() {
            public int compare(Pair<Double, Integer> n1, Pair<Double, Integer> n2) {
                return Double.compare(n1.first, n2.first);
            }
        });
        pq.add(new Pair<Double, Integer>(0.0, range * 64 + range));
        while (!pq.isEmpty()) {
            Pair<Double, Integer> t = pq.poll();
            double dist = t.first;
            int pos = t.second;
            int curx = pos / 64;
            int cury = pos % 64;

            if (vis[curx][cury])
                continue;
            vis[curx][cury] = true;
            for (int x = curx - 1; x <= curx + 1; x++) {
                for (int y = cury - 1; y <= cury + 1; y++) {
                    if (x < 0 || x >= boxlen)
                        continue;
                    if (y < 0 || y >= boxlen)
                        continue;
                    MapLocation xyloc = Cache.MY_LOCATION.translate(x - range, y - range);
                    if (!rc.canSenseLocation(xyloc))
                        continue;
                    if (rc.isLocationOccupied(xyloc))
                        continue;
                    if (dis[x][y] > dis[curx][cury] + 1 / MapInfo.getKnownPassability(xyloc)) {
                        dis[x][y] = dis[curx][cury] + 1 / MapInfo.getKnownPassability(xyloc);
                        if (moveLength(x - range, y - range) > 1) {
                            dirTo[x][y] = dirTo[curx][cury];
                        }
                        pq.add(new Pair(dis[x][y], 64 * x + y));
                    }
                }
            }
        }

        int bestdist = 99999;
        Direction bestDir = Direction.CENTER;
        for (int i = 0; i < boxlen; i++) {
            for (int j = 0; j < boxlen; j++)
                if (dirTo[i][j] != null) {
                    if (i == 0 && j == 0)
                        continue;
                    MapLocation xyloc = Cache.MY_LOCATION.translate(i - range, j - range);
                    if (bestdist > loc.distanceSquaredTo(xyloc)) {
                        bestdist = loc.distanceSquaredTo(xyloc);
                        bestDir = dirTo[i][j];
                    }
                }
        }
        return tryMove(bestDir);
    }

    public static boolean tryRandomMove() throws GameActionException {
        return tryMove(randomAdjacentDirection());
    }

    /**
     * Usage: Util.random(Constants.CARDINAL_DIRECTIONS)
     * 
     * @param directions
     * @return
     */
    public static Direction random(Direction[] directions) {
        return directions[(int) (Math.random() * directions.length)];
    }

    public static Direction randomAdjacentDirection() {
        return random(ORDINAL_DIRECTIONS);
    }

    public static MapLocation findSpawnEC() throws GameActionException {
        for (RobotInfo r : Cache.ALLY_ROBOTS) {
            if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
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
