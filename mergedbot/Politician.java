package mergedbot;

import battlecode.common.*;
import mergedbot.util.Cache;
import mergedbot.util.Constants;
import mergedbot.util.LambdaUtil;
import mergedbot.util.LatticeUtil;
import mergedbot.util.MapInfo;
import mergedbot.util.Pathfinder;
import mergedbot.util.UnitCommunication;
import mergedbot.util.Util;
import java.util.Optional;

import java.util.Comparator;

public strictfp class Politician implements RunnableBot {
    private static RobotController rc;
    private static int power;
    private static final int[] DEFENSE_SQUARES_X = { 2, 0, -2, 0, 2, 1, -1, -2, -2, -1, 1, 2, 2, -2, -2, 2, 3, 0, -3,
            0 };
    private static final int[] DEFENSE_SQUARES_Y = { 0, 2, 0, -2, 1, 2, 2, 1, -1, -2, -2, -1, 2, 2, -2, -2, 0, 3, 0,
            -3 };
    private static boolean defender;
    private static MapLocation nearestEC;
    private static MapLocation nearestS;

    public Politician(RobotController rc) {
        Politician.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        if (Math.random() < 0.4) {
            defender = true;
        } else {
            defender = false;
        }
    }

    private void preTurn() throws GameActionException {
        power = (int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(Constants.ALLY_TEAM, 0));

        Optional<MapLocation> tmp = MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getClosestLocation();
        nearestEC = tmp.map(x -> x).orElse(null);
        int minDist = 9999;
        for (RobotInfo robot : Cache.ALLY_ROBOTS) {
            if (UnitCommunication.isPotentialSlanderer(robot)) {
                int dist = robot.location.distanceSquaredTo(Cache.MY_LOCATION);
                if (dist < minDist) {
                    minDist = dist;
                    nearestS = robot.location;
                }
            }
        }
    }

    @Override
    public void turn() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        preTurn();
        if (power >= 30 && tryClaimEC()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 0, 255); // blue
            return;
        }
        if (tryEmpower()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 255); // cyan
            return;
        }
        if (chaseWorthwhileEnemy()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 0); // green
            return;
        }
        if (rc.getConviction() < 30 && tryDefend()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 255, 0, 255); // pink
            return;
        }
        if (power >= 30 && goToNearestEC()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 255, 255, 0); // yellow
            return;
        }
        if (Util.smartExplore()) {
            return;
        }
    }

    public int getDefenseScore(MapLocation loc) {
        if (!LatticeUtil.isLatticeLocation(loc))
            return 999999;
        return MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getClosestLocation(loc).map(ec -> {
            int dist = ec.distanceSquaredTo(loc);
            if (dist <= 16)
                return 999999;
            for (Direction d : Constants.ORDINAL_DIRECTIONS) {
                MapLocation adj = loc.add(d);
                if (rc.canSenseLocation(adj)) {
                    try {
                        RobotInfo robot = rc.senseRobotAtLocation(adj);
                        if (robot != null && UnitCommunication.isPotentialSlanderer(robot)) {
                            return 999999;
                        }
                    } catch (GameActionException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            }
            return MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getClosestLocation(Cache.MY_LOCATION)
                    .map(nearestEnemyEC -> 5000 * dist + nearestEnemyEC.distanceSquaredTo(loc)).orElse(5000 * dist);
        }).orElse(999999);
    }

    public boolean tryDefend() throws GameActionException {
        if (!defender)
            return false;
        // find best defense score
        int bestDefenseScore = 999999;
        Direction bestDir = null;
        for (Direction d : Constants.ORDINAL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(d);
            if (rc.canMove(d)) {
                int score = getDefenseScore(loc);
                if (score < bestDefenseScore) {
                    bestDefenseScore = score;
                    bestDir = d;
                }
            }
        }
        if (bestDir == null) {
            return false;
        }
        if (getDefenseScore(Cache.MY_LOCATION) <= bestDefenseScore) {
            return true;
        }
        return Util.tryMove(bestDir);
    }

    public boolean goToNearestEC() {
        return LambdaUtil
                .or(() -> MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).getClosestLocation(Cache.MY_LOCATION),
                        () -> MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM)
                                .getClosestLocation(Cache.MY_LOCATION))
                .map(ec -> {
                    Util.setIndicatorDot(ec, 255, 255, 0); // yellow
                    Pathfinder.execute(ec);
                    return true;
                }).orElse(false);
    }

    private int[] getScore(int radiusSquared) {
        RobotInfo[] robots = rc.senseNearbyRobots(radiusSquared);
        int numUnits = robots.length;
        if (numUnits == 0)
            return new int[7];
        int damage = power / numUnits;
        int mKills = 0;
        int pKills = 0;
        int ecKills = 0;
        int totalConviction = 0;
        int distMtoS = 9999;
        int distPtoEC = 9999;
        for (RobotInfo robot : robots) {
            if (robot.getTeam() != Constants.ALLY_TEAM) {
                if (robot.getType() == RobotType.POLITICIAN) {
                    totalConviction += Math.min(damage,
                            Math.max(0, robot.getConviction() - 10) + Math.max(0, robot.getInfluence() - 10));
                    if (damage > robot.getConviction()) {
                        pKills++;
                    }
                    if(nearestEC != null) distPtoEC = Math.min(distPtoEC, robot.location.distanceSquaredTo(nearestEC));
                } else if (robot.getType() == RobotType.MUCKRAKER) {
                    totalConviction += Math.min(damage, robot.getConviction());
                    if (damage > robot.getConviction()) {
                        mKills++;
                    }
                    if(nearestS != null) distMtoS = Math.min(distMtoS, robot.location.distanceSquaredTo(nearestS));
                } else if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    totalConviction += Math.min(damage, robot.getConviction());
                    if (damage > robot.getConviction()) {
                        ecKills++;
                    }
                }
            }
        }
        int numKills = ecKills + mKills + pKills;
        int score = ecKills * 100000 * 100000 + numKills * 100000 + totalConviction;

        int[] res = new int[7];
        res[0] = score;
        res[1] = ecKills;
        res[2] = mKills;
        res[3] = pKills;
        res[4] = totalConviction;
        res[5] = distPtoEC;
        res[6] = distMtoS;
        return res;
    }

    public boolean tryClaimEC() throws GameActionException {
        // if we see enemy/neutral ec, try to move closer to it
        // if can't move any closer, explode
        MapLocation bestLoc = null;
        int bestDist = Integer.MAX_VALUE;
        for (RobotInfo robot : Cache.ALL_ROBOTS) {
            if (robot.getTeam() != Constants.ALLY_TEAM && robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation loc = robot.location;
                int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        if (bestLoc == null)
            return false;

        int bestNewDist = bestDist;
        Direction bestDir = null;
        for (Direction d : Constants.ORDINAL_DIRECTIONS)
            if (rc.canMove(d)) {
                int dist = Cache.MY_LOCATION.add(d).distanceSquaredTo(bestLoc);
                if (dist < bestNewDist) {
                    bestNewDist = dist;
                    bestDir = d;
                }
            }
        if (bestDir == null) { // can't get any closer
            if (bestDist > 9)
                return false;
            if (bestDist >= 5 && rc.senseNearbyRobots(bestDist).length >= 6)
                return false;
            rc.empower(bestDist);
            return true;
        } else {
            Util.tryMove(bestDir);
            return true;
        }
    }

    public boolean shouldEmpower(int ecKills, int mKills, int pKills, int convictionGotten, int distPtoEC, int distMtoS) throws GameActionException {
        if (ecKills >= 1) {
            return true;
        }
        int numKills = mKills + pKills;
        if (rc.getConviction() <= 30) {
            if (numKills == 0)
                return false;
            if (distMtoS <= 100 && mKills >= 1)
                return true;
            if (nearestEC != null && nearestEC.distanceSquaredTo(Cache.MY_LOCATION) <= 100 && mKills >= 1)
                return true;
            if (distPtoEC <= 8 && pKills >= 1)
                return true;
            if (mKills >= 2)
                return true;
            return false;
        } else {
            int distFromEC  = nearestEC == null ? 1024 : nearestEC.distanceSquaredTo(Cache.MY_LOCATION);
            double euclidDist = Math.sqrt(distFromEC);
            double requiredRatio = 1.0 / (0.03 * (euclidDist + 5.0)) + 1;
            if (convictionGotten * requiredRatio >= rc.getConviction() - 10) {
                return true;
            }
            return false;
        }
        /*int numKills = ecKills + mKills + pKills;
        if (convictionGotten * 2 >= rc.getConviction() - 10 || rc.getConviction() <= 30 && numKills >= 2) {
            return true;
        }
        int dist = MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM)
                .getClosestLocationDistance(1024);
        if (convictionGotten * 10 >= rc.getConviction() - 10 && dist <= 64) {
            return true;
        }
        return false;*/
    }

    public boolean tryEmpower() throws GameActionException {
        if (power < 0)
            return false;
        int actionRadiusSquared = rc.getType().actionRadiusSquared;
        // if can kill something, maximize the number
        int bestRadius = -1;
        int[] bestScore = new int[5];
        for (int r = 1; r <= actionRadiusSquared; r++) {
            int[] score = getScore(r);
            if (score[0] > bestScore[0]) {
                bestScore = score;
                bestRadius = r;
            }
        }
        if (bestRadius == -1)
            return false;

        int ecKills = bestScore[1];
        int mKills = bestScore[2];
        int pKills = bestScore[3];
        int convictionGotten = bestScore[4];
        int distPtoEC = bestScore[5];
        int distMtoS = bestScore[6];
        if (shouldEmpower(ecKills, mKills, pKills, convictionGotten, distPtoEC, distMtoS)) {
            rc.empower(bestRadius);
            return true;
        } else {
            return false;
        }
    }

    private boolean shouldChase(RobotInfo robot) {
        if (robot.getType() == RobotType.POLITICIAN)
            return false;
        if (rc.getConviction() * 2 + 20 >= robot.getConviction()
                && robot.getConviction() * 2 + 20 >= rc.getConviction())
            return true;
        if (robot.getType() == RobotType.MUCKRAKER
                && ((nearestS != null && robot.location.distanceSquaredTo(nearestS) <= 81)
                        || (nearestEC != null && robot.location.distanceSquaredTo(nearestEC) <= 81))
                && robot.getConviction() * 10 + 20 >= rc.getConviction())
            return true;
        return false;
    }

    private boolean chaseWorthwhileEnemy() throws GameActionException {
        int bestDist = 9999;
        MapLocation bestLoc = null;
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (shouldChase(robot)) {
                int numNearbyPoliticians = 0;
                MapLocation loc = robot.location;
                RobotInfo[] nearbyAllies = rc.senseNearbyRobots(loc, 5, Constants.ALLY_TEAM);
                for (RobotInfo robot2 : nearbyAllies) {
                    if (robot2.getType() == RobotType.POLITICIAN)
                        numNearbyPoliticians++;
                }
                if (numNearbyPoliticians >= 2)
                    continue;
                int dist = Cache.MY_LOCATION.distanceSquaredTo(loc);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        if (bestLoc == null) {
            return false;
        }
        return Util.tryMove(bestLoc);
    }
}
