package experiment;

import battlecode.common.*;
import experiment.util.*;

import java.nio.file.Path;
import java.util.Comparator;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private static RobotController rc;

    private static int slandererCount = 0;
    private static int muckrakerCount = 0;
    private static int politicianCount = 0;
    private static int lastInfluence = 0;
    private static int perTurnProfit = 0;
    private static int turnsSinceSelfEmpowerer = 0;
    private static int turnsSinceDanger = 0;
    private static boolean initialEC;
    private static ECCaptureTracker ecTracker;
    private static int perTurnRation;

    private static MapLocation enemyDirection;

    public EnlightenmentCenter(RobotController rc) {
        EnlightenmentCenter.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        enemyDirection = rc.getLocation();
        lastInfluence = rc.getInfluence();
        initialEC = rc.getRoundNum() == 1;
        ecTracker = new ECCaptureTracker();
    }

    public void preTurn() {
        turnsSinceSelfEmpowerer++;
        turnsSinceDanger++;
        perTurnProfit = rc.getInfluence() - lastInfluence;
        if (CentralCommunication.nearestEnemy != null) {
            Direction directionToNearestEnemy = Cache.MY_LOCATION.directionTo(CentralCommunication.nearestEnemy);
            enemyDirection = enemyDirection.add(directionToNearestEnemy);
            if(CentralCommunication.nearestEnemyType == RobotType.MUCKRAKER && CentralCommunication.nearestEnemyDistanceSquared <= 64) turnsSinceDanger = 0;
        }
        perTurnRation = rc.getInfluence()/(1500 - rc.getRoundNum());
    }

    public void postTurn() {
        lastInfluence = rc.getInfluence();
    }

    @Override
    public void turn() throws GameActionException {
        preTurn();
        buildUnit();
        bid();
        postTurn();
    }

    public static void buildUnit() {
        if (!rc.isReady()) return;
        if (reactBuild()) return;
        int numUnits = slandererCount + politicianCount + muckrakerCount;
        if (numUnits < 30) {
            switch(numUnits%4) {
                case 0:
                    tryBuildSlanderer(rc.getInfluence()-10);
                    break;
                case 1:
                    tryBuild(RobotType.MUCKRAKER, 1);
                    break;
                case 2:
                    tryBuild(RobotType.MUCKRAKER, 1);
                    break;
                case 3:
                    if(buildNeutralECClaimer()) return;
                    tryBuild(RobotType.POLITICIAN, Util.randBetween(14, 20));
                    break;
            }
        }
        if (buildNeutralECClaimer()) return;
        if (buildSelfEmpowerer()) return;
        if (Cache.TURN_COUNT <= 30 || turnsSinceDanger <= 10 || rc.getInfluence() >= 50000) {
            if(buildNonEco()) return;
        } else {
            if(buildEco()) return;
        }
        if (tryBuild(RobotType.MUCKRAKER, 1)) return;
    }

    public static void bid() throws GameActionException {
        if (rc.getTeamVotes() == 751) {
            return;
        }
        if (rc.getInfluence() <= 100) {
            return;
        }
        double r = Math.random();
        int amount;
        if (r < 0.2) {
            amount = Math.max(Util.randBetween(1, 3), (int) (0.1 * perTurnRation));
        } else if (r < 0.4) {
            amount = Math.min((int) (0.01 * rc.getInfluence()), Math.max(1000, (int) (0.2*perTurnRation)));
        } else {
            amount = Math.min((int) (0.02 * rc.getInfluence()), Math.max(1000, (int)(0.3*perTurnRation)));
        }
        rc.bid(amount);
    }

    public static int getSaveAmount() {
        // Save if we see enemy EC or enemy politician
        int res = LambdaUtil.arraysStreamSum(Cache.ALL_ROBOTS,
                r -> r.getTeam() == Constants.ENEMY_TEAM &&
                        (r.getType() == RobotType.ENLIGHTENMENT_CENTER || r.getType() == RobotType.POLITICIAN),
                r -> Math.max(r.getConviction() - Constants.POLITICIAN_EMPOWER_PENALTY + 1, 0));
        //if(perTurnProfit >= 50) res = Math.max(res, 2 * perTurnProfit);
        return res;
    }

    public static boolean tryBuildRobot(RobotType type, Direction direction, int influence) {
        if (rc.canBuildRobot(type, direction, influence)) {
            switch (type) {
                case SLANDERER:
                    slandererCount++;
                    break;
                case POLITICIAN:
                    politicianCount++;
                    break;
                case MUCKRAKER:
                    muckrakerCount++;
                    break;
            }
            try {
                rc.buildRobot(type, direction, influence);
            } catch (GameActionException ex) {
                throw new IllegalStateException(ex);
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryBuildCardinal(RobotType type, int influence) {
        for (Direction buildDirection : Constants.CARDINAL_DIRECTIONS) {
            if (tryBuildRobot(type, buildDirection, influence)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryBuildRobotTowards(RobotType type, Direction direction, int influence) {
        for (Direction buildDirection : Constants.getAttemptOrder(direction)) {
            if (tryBuildRobot(type, buildDirection, influence)) {
                return true;
            }
        }
        return false;
    }

    // for when we don't specify a direction
    public static boolean tryBuild(RobotType type, int influence) {
        if (turnsSinceSelfEmpowerer <= 10 && Cache.TURN_COUNT > 10) return tryBuildCardinal(type, influence);
        return tryBuildRobotTowards(type, Util.randomAdjacentDirection(), influence);
    }

    public static boolean reactBuild() {
        // If there are enemy slanderers in sensing radius, build muckrakers
        if (LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> r.getType() == RobotType.SLANDERER)) {
            if (tryBuild(RobotType.MUCKRAKER, 1)) return true;
        }
        boolean muckrakerNear = CentralCommunication.nearestEnemyType == RobotType.MUCKRAKER &&
                CentralCommunication.nearestEnemyDistanceSquared <= 64;
        // ignore small politicians's unless they're really close
        boolean politicianNear = CentralCommunication.nearestEnemyType == RobotType.POLITICIAN &&
                CentralCommunication.nearestEnemyDistanceSquared <= 25;
        // do we have slanderers? is there danger (muckrakers)? build defender politicians
        if ((slandererCount > 0 && muckrakerNear) || politicianNear) {
            int cost = 5 * CentralCommunication.nearestEnemyConviction + Constants.POLITICIAN_EMPOWER_PENALTY;
            cost = Math.min(cost, 3 * CentralCommunication.nearestEnemyConviction + 20);
            cost = Math.min(cost, 2 * CentralCommunication.nearestEnemyConviction + 30);
            cost = Math.min(cost, CentralCommunication.nearestEnemyConviction + 50);
            if (rc.getInfluence() >= cost) {
                if (tryBuild(RobotType.POLITICIAN, cost)) return true;
            } else {
                // save for politician - build 1 cost muckraker
                if (tryBuild(RobotType.MUCKRAKER, 1)) return true;
            }
        }
        return false;
    }

    public static boolean buildNeutralECClaimer() {
        // find neutral EC communicated
        if (MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL)
                .min(Comparator.comparingInt(x -> {
                    int lastRoundSent = ecTracker.getRoundSent(x.location);
                    int roundsSince = rc.getRoundNum() - lastRoundSent;
                    System.out.println("Neutral EC at "+ x.location.x +","+x.location.y+": " + "lastRoundSent="+lastRoundSent+", roundsSince="+roundsSince);
                    if(lastRoundSent != -1 && roundsSince <= 10 + 3*Pathfinder.moveDistance(x.location, Cache.MY_LOCATION)) return 100000;
                    return x.lastKnownConviction == -1 ? 500 : x.lastKnownConviction;
                }))
                .map(x -> {
                    MapLocation location = x.location;
                    int lastRoundSent = ecTracker.getRoundSent(location);
                    int roundsSince = rc.getRoundNum() - lastRoundSent;
                    if(lastRoundSent != -1 && roundsSince <= 10 + Pathfinder.moveDistance(x.location, Cache.MY_LOCATION)) return false;
                    int conviction = x.lastKnownConviction == -1 ? 500 : x.lastKnownConviction;
                    int cost = conviction + Constants.POLITICIAN_EMPOWER_PENALTY + 20;
                    if(tryBuild(RobotType.POLITICIAN, cost)) {
                        ecTracker.addOrUpdate(location, rc.getRoundNum());
                        System.out.println("SENT POLI TO (" + location.x + "," + location.y + ")");
                        return true;
                    } else {
                        return false;
                    }
                }).orElse(false)) {
            return true;
        }
        return false;
    }

    public static boolean buildEco() {
        double r = Math.random();
        if(r < 0.4) {
            if (Math.random() < 0.8) {
                if (tryBuildSlanderer(rc.getInfluence()-10)) return true;
            }
            if (tryBuild(RobotType.POLITICIAN, Util.randBetween(14, 20))) return true;
        } else if(r < 0.8) {
            if(rc.getInfluence() >= 200) {
                if(tryBuild(RobotType.POLITICIAN, Math.min(rc.getInfluence()/2, Math.max(1000, (int)(0.5*perTurnRation))))) return true;
            } else {
                if(tryBuild(RobotType.POLITICIAN, Util.randBetween(14, 20))) return true;
            }
        } else {
            if(rc.getInfluence() >= 200 && Math.random() < 0.5) {
                if (tryBuild(RobotType.MUCKRAKER, Math.min(rc.getInfluence()/2, Math.max(1000, (int)(0.5*perTurnRation))))) return true;
            } else {
                if (tryBuild(RobotType.MUCKRAKER, 1)) return true;
            }
        }
        return false;
    }

    public static boolean buildNonEco() {
        double r = Math.random();
        if(r < 0.5) {
            if(rc.getInfluence() >= 200) {
                if(tryBuild(RobotType.POLITICIAN, Math.min(rc.getInfluence()-10, Math.max(1000, (int)(0.5*perTurnRation))))) return true;
            } else {
                if(tryBuild(RobotType.POLITICIAN, Util.randBetween(14, 20))) return true;
            }
        } else {
            if(rc.getInfluence() >= 200 && Math.random() < 0.5) {
                if (tryBuild(RobotType.MUCKRAKER, Math.min(rc.getInfluence()-10, Math.max(1000, (int)(0.5*perTurnRation))))) return true;
            } else {
                if (tryBuild(RobotType.MUCKRAKER, 1)) return true;
            }
        }
        return false;
    }

    public static boolean buildSelfEmpowerer() {
        if(turnsSinceSelfEmpowerer <= 10) return false;
        if(rc.getEmpowerFactor(Constants.ALLY_TEAM, 11) < 1.5) return false;
        int cost = rc.getInfluence()/2;
        cost = cost + 6 - (cost % 10);
        if (cost <= 50) return false;
        if (tryBuildCardinal(RobotType.POLITICIAN, cost)) {
            turnsSinceSelfEmpowerer = 0;
            return true;
        }
        return false;
    }

    public static boolean tryBuildSlanderer(int cost) {
        if(cost > 463) cost = 463;
        cost = SlandererBuild.getBuildInfluence(cost);
        return tryBuild(RobotType.SLANDERER, cost);
    }
}
