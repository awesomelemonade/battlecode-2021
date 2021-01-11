package latticebot;

import battlecode.common.*;
import latticebot.util.Cache;
import latticebot.util.Constants;
import latticebot.util.MapInfo;
import latticebot.util.Pathfinder;
import latticebot.util.UnitCommunication;
import latticebot.util.Util;

public strictfp class Politician implements RunnableBot {
    private static RobotController rc;
    private static int power;

    public Politician(RobotController rc) {
        Politician.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        power = (int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(Constants.ALLY_TEAM, 0));
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
        if (power >= 30 && goToNearestEC()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 255, 255, 0); // yellow
            return;
        }
        if (Util.smartExplore()) {
            return;
        }
    }

    public boolean goToNearestEC() throws GameActionException {
        RobotInfo[] neutralECs = rc.senseNearbyRobots(-1, Team.NEUTRAL);
        MapLocation ec = Util.getFirst(() -> Util.mapToLocation(Util.getClosestRobot(neutralECs, x -> true)),
                () -> MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).getClosestLocation(Cache.MY_LOCATION),
                () -> Util.mapToLocation(Util.getClosestEnemyRobot(x -> x.getType() == RobotType.ENLIGHTENMENT_CENTER)),
                () -> MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM)
                        .getClosestLocation(Cache.MY_LOCATION));
        if (ec != null) {
            Util.setIndicatorDot(ec, 255, 255, 0); // yellow
            Pathfinder.execute(ec);
            return true;
        } else {
            return false;
        }
    }

    private int getScore(int radiusSquared) {
        RobotInfo[] robots = rc.senseNearbyRobots(radiusSquared);
        int numUnits = robots.length;
        if (numUnits == 0)
            return 0;
        int damage = power / numUnits;
        int numKills = 0;
        int totalConviction = 0;
        for (RobotInfo robot : robots) {
            if (robot.getTeam() != Constants.ALLY_TEAM && robot.getConviction() < damage) {
                numKills++;
                int transferredConviction = robot.getConviction();
                if (robot.getType() == RobotType.POLITICIAN) {
                    transferredConviction += robot.getInfluence();
                    transferredConviction -= 10;
                } else if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    transferredConviction += robot.getInfluence();
                }
                transferredConviction = Math.min(damage, transferredConviction);
                totalConviction += transferredConviction;
            }
        }
        return numKills * 1000000 + totalConviction;
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

    public boolean tryEmpower() throws GameActionException {
        if (power < 0)
            return false;
        int actionRadiusSquared = rc.getType().actionRadiusSquared;
        // if can kill something, maximize the number
        int bestRadius = -1;
        int bestScore = 0;
        for (int r = 1; r <= actionRadiusSquared; r++) {
            int score = getScore(r);
            if (score > bestScore) {
                bestScore = score;
                bestRadius = r;
            }
        }
        if (bestRadius == -1)
            return false;

        int numKills = bestScore / 1000000;
        int convictionGotten = bestScore % 1000000;
        if (convictionGotten * 2 + 10 >= rc.getConviction() - 10) {
            rc.empower(bestRadius);
            return true;
        }
        return false;
    }

    private boolean chaseWorthwhileEnemy() throws GameActionException {
        int bestDist = 9999;
        MapLocation bestLoc = null;
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (rc.getConviction() * 2 + 20 >= robot.getConviction()
                    && robot.getConviction() * 2 + 20 >= rc.getConviction()) {
                MapLocation loc = robot.location;
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
