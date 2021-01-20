package monly;

import battlecode.common.*;
import monly.util.Cache;
import monly.util.CentralCommunication;
import monly.util.Constants;
import monly.util.LambdaUtil;
import monly.util.MapInfo;
import monly.util.SlandererBuild;
import monly.util.Util;

import java.util.Comparator;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private static RobotController rc;

    private static int slandererCount = 0;
    private static int muckrakerCount = 0;
    private static int politicianCount = 0;
    private static int lastInfluence = 0;
    private static int perTurnProfit = 0;
    private static int turnsSinceSelfEmpowerer = 0;
    public static boolean initialEC;

    private static MapLocation enemyDirection;

    public EnlightenmentCenter(RobotController rc) {
        EnlightenmentCenter.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        enemyDirection = rc.getLocation();
        lastInfluence = rc.getInfluence();
        initialEC = rc.getRoundNum() == 1;
    }

    public void preTurn() {
        turnsSinceSelfEmpowerer++;
        perTurnProfit = rc.getInfluence() - lastInfluence;
        Util.println("Profit = " + perTurnProfit);
    }

    public void postTurn() {
        lastInfluence = rc.getInfluence();
    }

    @Override
    public void turn() throws GameActionException {
        preTurn();
        if (CentralCommunication.nearestEnemy != null) {
            Direction directionToNearestEnemy = Cache.MY_LOCATION.directionTo(CentralCommunication.nearestEnemy);
            enemyDirection = enemyDirection.add(directionToNearestEnemy);
        }
        int saveAmount = getSaveAmount();
        Util.println("EC Turn: save=" + saveAmount + ", buff=" + rc.getEmpowerFactor(Constants.ALLY_TEAM, 0));
        int influenceLeft = rc.getInfluence() - saveAmount - 1;
        buildUnit(influenceLeft);
        influenceLeft = rc.getInfluence() - saveAmount - 1;
        bid(influenceLeft);
        postTurn();
    }

    public static void buildUnit(int influence) {
        if (rc.getEmpowerFactor(Constants.ALLY_TEAM, 10) >= 1.5) {
            if (buildSelfEmpowerer(influence - 1)) return;
        }
        if (rc.getRoundNum() % 4 == 2) {
            buildMuckraker(Util.randBetween(30, 40));
        } else {
            buildCheapMuckraker();
        }
    }

    public static void bid(int influence) throws GameActionException {
        if (rc.getTeamVotes() == 751) {
            return;
        }
        int amount = Math.max(Util.randBetween(1, 3), (int) (0.1 * rc.getInfluence() / (1500 - rc.getRoundNum())));
        if(rc.getInfluence() <= 100) {
            amount = 1;
        }
        rc.bid(amount);
    }

    public static int getSaveAmount() {
        // Save if we see enemy EC or enemy politician
        int res = LambdaUtil.arraysStreamSum(Cache.ALL_ROBOTS,
                r -> r.getTeam() != Constants.ALLY_TEAM &&
                        (r.getType() == RobotType.ENLIGHTENMENT_CENTER || r.getType() == RobotType.POLITICIAN),
                r -> Math.max(r.getConviction() - Constants.POLITICIAN_EMPOWER_PENALTY + 1, 0));
        //if(perTurnProfit >= 50) res = Math.max(res, 2 * perTurnProfit);
        return res;
    }

    public static boolean reactBuild(int influence) {
        // If there are enemy slanderers in sensing radius, build muckrakers
        if (LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> r.getType() == RobotType.SLANDERER)) {
            if (buildCheapMuckraker()) return true;
        }
        // TODO: Loop through all communicated units to figure this out
        boolean muckrakerNear = CentralCommunication.nearestEnemyType == RobotType.MUCKRAKER &&
                (CentralCommunication.nearestEnemyDistanceSquared <= 64 ||
                        CentralCommunication.nearestEnemyDistanceSquared <= 100 && Math.random() < 0.5);
        // ignore small politicians's unless they're really close
        boolean politicianNear = CentralCommunication.nearestEnemyType == RobotType.POLITICIAN &&
                CentralCommunication.nearestEnemyDistanceSquared <= 25;
        // do we have slanderers? is there danger (muckrakers)? build defender politicians
        if ((slandererCount > 0 && muckrakerNear) || politicianNear) {
            int cost = 5 * CentralCommunication.nearestEnemyConviction + Constants.POLITICIAN_EMPOWER_PENALTY;
            cost = Math.min(cost, 3 * CentralCommunication.nearestEnemyConviction + 20);
            cost = Math.min(cost, 2 * CentralCommunication.nearestEnemyConviction + 30);
            cost = Math.min(cost, CentralCommunication.nearestEnemyConviction + 50);
            if (influence >= cost) {
                buildPolitician(cost);
            } else {
                // save for politician - build 1 cost muckraker
                buildCheapMuckraker();
            }
            return true;
        }
        /*// find smallest neutral/enemy EC (in vision)
        if (LambdaUtil.arraysStreamMin(Cache.ALL_ROBOTS,
                r -> r.getTeam() != Constants.ALLY_TEAM && r.getType() == RobotType.ENLIGHTENMENT_CENTER,
                r -> r.getConviction() + Constants.POLITICIAN_EMPOWER_PENALTY).map(cost -> {
            if (influence >= cost) {
                buildPolitician(cost);
            } else {
                buildCheapMuckraker();
            }
            return true;
        }).orElse(false)) {
            return false;
        }
        // find neutral EC communicated
        if (MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL)
                .min(Comparator.comparingInt(x -> x.location.distanceSquaredTo(Cache.MY_LOCATION)))
                .map(x -> {
                    MapLocation location = x.location;
                    int conviction = x.lastKnownConviction;
                    int cost = conviction + Constants.POLITICIAN_EMPOWER_PENALTY;
                    Util.println("Spawning Politician for Neutral EC: " + cost);
                    if (influence >= cost) {
                        buildPolitician(cost);
                    } else {
                        buildCheapMuckraker();
                    }
                    return true;
                }).orElse(false)) {
            return true;
        }*/
        return false;
    }

    public static boolean buildCheapMuckraker() {
        int influence = Math.max(1, (int) (0.1 * rc.getInfluence() / (1500 - rc.getRoundNum())));
        if (turnsSinceSelfEmpowerer <= 11) return Util.tryBuildCardinal(RobotType.MUCKRAKER, influence);
        if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), influence)) {
            muckrakerCount++;
            return true;
        }
        return false;
    }

    public static boolean buildMuckraker(int influence) {
        int cap = Math.max(1000, (int) (0.5 * rc.getInfluence() / (1500 - rc.getRoundNum())));
        influence = Math.min(influence, cap);
        if (turnsSinceSelfEmpowerer <= 11) return Util.tryBuildCardinal(RobotType.MUCKRAKER, influence);
        if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), influence)) {
            muckrakerCount++;
            return true;
        }
        return false;
    }

    public static boolean buildSlanderer(int influence) {
        // check if slanderer will provide negligible eco
        if (rc.getConviction() >= 50000 || rc.getConviction() >= 300 * (1500 - rc.getRoundNum())) return false;
        influence = Math.min(influence, 463);
        if (turnsSinceSelfEmpowerer <= 11) {
            if (Util.tryBuildCardinal(RobotType.SLANDERER, influence)) {
                slandererCount++;
                return true;
            }
        }
        Direction awayFromEnemies = enemyDirection.directionTo(Cache.MY_LOCATION);
        if (awayFromEnemies == Direction.CENTER) {
            awayFromEnemies = Util.randomAdjacentDirection();
        }
        int cost = SlandererBuild.getBuildInfluence(influence);
        if (cost > 0 && Util.tryBuildRobotTowards(RobotType.SLANDERER, awayFromEnemies, cost)) {
            slandererCount++;
            return true;
        } else {
            return false;
        }
    }

    public static boolean buildCheapPolitician() {
        int influence = Math.max(Util.randBetween(14, 20), (int) (0.1 * rc.getInfluence() / (1500 - rc.getRoundNum())));
        if (influence % 10 == 6) influence++;
        if (turnsSinceSelfEmpowerer <= 11) return Util.tryBuildCardinal(RobotType.POLITICIAN, influence);
        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), influence)) {
            politicianCount++;
            return true;
        }
        return false;
    }

    public static boolean buildPolitician(int influence) {
        int cap = Math.max(1000, (int) (0.5 * (rc.getInfluence() / (1500 - rc.getRoundNum()))));
        influence = Math.min(influence, cap);
        if (influence % 10 == 6) influence++;
        if (turnsSinceSelfEmpowerer <= 11) return Util.tryBuildCardinal(RobotType.POLITICIAN, influence);
        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), influence)) {
            politicianCount++;
            return true;
        }
        return false;
    }

    public static boolean buildSelfEmpowerer(int influence) {
        influence = influence + 6 - (influence % 10);
        if (influence <= 20) return false;
        if (Util.tryBuildCardinal(RobotType.POLITICIAN, influence)) {
            politicianCount++;
            turnsSinceSelfEmpowerer = 0;
            return true;
        }
        return false;
    }
}