package buildorderv3;

import battlecode.common.*;
import buildorderv3.util.Cache;
import buildorderv3.util.Constants;
import buildorderv3.util.LambdaUtil;
import buildorderv3.util.LatticeUtil;
import buildorderv3.util.MapInfo;
import buildorderv3.util.Pathfinder;
import buildorderv3.util.UnitCommunication;
import buildorderv3.util.Util;
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
    private static boolean selfempowerer;

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
        if (Math.random() < 0.3) {
            selfempowerer = true;
        } else {
            selfempowerer = false;
        }
    }

    private void preTurn() throws GameActionException {
        power = (int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(Constants.ALLY_TEAM, 0));

        nearestEC = MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getClosestLocation().map(x -> x).orElse(null);
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
        if (rc.getConviction() <= 10) { // useless; best thing to do is try to block an enemy ec
            if(campEnemyEC()) {
                return;
            }
            Util.smartExplore();
            return;
        }
        if (power >= 30 && tryClaimEC()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 0, 255); // blue
            return;
        }
        if (rc.getConviction() >= 30 && tryHealEC()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 102, 51, 0); // brown
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
        if (selfempowerer && rc.getConviction() >= 30 && trySelfBuff()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 102, 102, 153); // bluish purple
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

    public boolean tryEmpowerAt(MapLocation loc) throws GameActionException {
        int bestDist = loc.distanceSquaredTo(Cache.MY_LOCATION);
        Direction bestDir = null;
        for (Direction d : Constants.ORDINAL_DIRECTIONS)
            if (rc.canMove(d)) {
                int dist = Cache.MY_LOCATION.add(d).distanceSquaredTo(loc);
                if (dist < bestDist) {
                    bestDist = dist;
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

    public boolean shouldHeal(RobotInfo ec) throws GameActionException {
        //if(selfempowerer && rc.getEmpowerFactor(Constants.ALLY_TEAM, 0) >= 1.5) return true;
        if(Constants.SPAWN.distanceSquaredTo(ec.location) <= 16) return false;
        // heal if ec is low and surrounded by enemy ps
        int enemyConviction = 0;
        RobotInfo[] enemies = rc.senseNearbyRobots(ec.location, 64, Constants.ENEMY_TEAM);
        for(RobotInfo robot : enemies) {
            if(robot.getType() == RobotType.POLITICIAN) {
                enemyConviction += robot.getConviction() - 10;
            }
        }
        if(2*enemyConviction > ec.getConviction()) {
            return true;
        }
        return false;
    }

    public boolean trySelfBuff() throws GameActionException {
        if(rc.getEmpowerFactor(Constants.ALLY_TEAM, 0) < 1.5) return false;
        int bestDist = 9999;
        MapLocation bestLoc = null;
        for(RobotInfo robot : Cache.ALLY_ROBOTS) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation loc = robot.location;
                int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
                if(dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        if(bestLoc == null) return false;
        return tryEmpowerAt(bestLoc);
    }

    public boolean tryHealEC() throws GameActionException {
        int bestDist = 9999;
        MapLocation bestLoc = null;
        for(RobotInfo robot : Cache.ALLY_ROBOTS) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && shouldHeal(robot)) {
                MapLocation loc = robot.location;
                int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
                if(dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        if(bestLoc == null) return false;
        return tryEmpowerAt(bestLoc);
    }

    // if sees empty square next to ec, go to it
    public boolean campEnemyEC() throws GameActionException {
        MapLocation enemyECLoc = MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getClosestLocation().map(x -> x).orElse(null);
        if(enemyECLoc == null) return false;
        if (rc.getLocation().distanceSquaredTo(enemyECLoc) >= 25) {
            return Util.tryMove(enemyECLoc);
        }
        if (rc.getLocation().distanceSquaredTo(enemyECLoc) <= 2) {
            return true;
        }
        for (Direction d : Constants.ORDINAL_DIRECTIONS) {
            MapLocation loc = enemyECLoc.add(d);
            if(rc.canSenseLocation(loc) && !rc.isLocationOccupied(loc)) {
                Util.tryMove(loc);
                return true;
            }
        }
        return false;
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
            return new int[8];
        int damage = power / numUnits;
        int mKills = 0;
        int pKills = 0;
        int ecKills = 0;
        int mecConviction = 0;
        int pConviction = 0;
        int distMtoS = 9999;
        int distPtoEC = 9999;
        for (RobotInfo robot : robots) {
            if (robot.getTeam() != Constants.ALLY_TEAM) {
                if (robot.getType() == RobotType.POLITICIAN) {
                    pConviction += Math.min(damage,
                            Math.max(0, robot.getConviction() - 10) + Math.max(0, robot.getInfluence() - 10));
                    if (damage > robot.getConviction()) {
                        pKills++;
                    }
                    if(nearestEC != null) distPtoEC = Math.min(distPtoEC, robot.location.distanceSquaredTo(nearestEC));
                } else if (robot.getType() == RobotType.MUCKRAKER) {
                    mecConviction += Math.min(damage, robot.getConviction());
                    if (damage > robot.getConviction()) {
                        mKills++;
                    }
                    if(nearestS != null) distMtoS = Math.min(distMtoS, robot.location.distanceSquaredTo(nearestS));
                } else if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    mecConviction += Math.min(damage, robot.getConviction());
                    if (damage > robot.getConviction()) {
                        ecKills++;
                    }
                }
            }
        }
        int numKills = ecKills + mKills + pKills;
        int score = ecKills * 100000 * 100000 + numKills * 100000 + mecConviction;

        int[] res = new int[8];
        res[0] = score;
        res[1] = ecKills;
        res[2] = mKills;
        res[3] = pKills;
        res[4] = mecConviction;
        res[5] = pConviction;
        res[6] = distPtoEC;
        res[7] = distMtoS;
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
        return tryEmpowerAt(bestLoc);
    }

    public boolean shouldEmpower(int ecKills, int mKills, int pKills, int mecConviction, int pConviction, int distPtoEC, int distMtoS) throws GameActionException {
        /*Util.println("ecKills = " + ecKills);
        Util.println("mKills = " + mKills);
        Util.println("pKills = " + pKills);
        Util.println("distPtoEC = " + distPtoEC);
        Util.println("distMtoS = " + distMtoS);*/
        if (ecKills >= 1) {
            return true;
        }
        int numKills = mKills + pKills;
        if (rc.getConviction() <= 30) {
            if (numKills == 0)
                return false;
            if (distMtoS <= 100 && mKills >= 1)
                return true;
            if (distPtoEC <= 8 && pKills >= 1)
                return true;
            if (mKills >= 2)  {
                return true;
            }
            if (nearestEC != null && nearestEC.distanceSquaredTo(Cache.MY_LOCATION) <= 100 && mKills >= 1)
                return true;
            return false;
        } else {
            if(numKills*15 >= rc.getConviction()-10) return true;
            int distFromEC = nearestEC == null ? 1024 : nearestEC.distanceSquaredTo(Cache.MY_LOCATION);
            double euclidDist = Math.sqrt(distFromEC);
            double requiredRatio = 1.0 / (0.03 * (euclidDist + 5.0)) + 1;
            if (mecConviction * requiredRatio >= rc.getConviction() - 10) {
                return true;
            }
            if(distPtoEC <= 2 && pConviction * 8 >= rc.getConviction() - 10) {
                return true;
            }
            return false;
        }
    }

    public boolean tryEmpower() throws GameActionException {
        if (power < 0)
            return false;
        int actionRadiusSquared = rc.getType().actionRadiusSquared;
        // if can kill something, maximize the number
        int bestRadius = -1;
        int[] bestScore = new int[1];
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
        int mecConviction = bestScore[4];
        int pConviction = bestScore[5];
        int distPtoEC = bestScore[6];
        int distMtoS = bestScore[7];
        if (shouldEmpower(ecKills, mKills, pKills, mecConviction, pConviction, distPtoEC, distMtoS)) {
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
        int totx = 0;
        int toty = 0;
        int tot = 0;
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
                totx += loc.x;
                toty += loc.y;
                tot++;
            }
        }
        if (tot == 0) {
            return false;
        }
        MapLocation desired = new MapLocation(totx/tot, toty/tot);
        return Util.tryMove(desired);
    }
}
