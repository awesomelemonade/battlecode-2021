package greedybot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import greedybot.util.Cache;
import greedybot.util.CentralUnitTracker;
import greedybot.util.Constants;
import greedybot.util.LambdaUtil;
import greedybot.util.MapInfo;
import greedybot.util.SlandererBuild;
import greedybot.util.Util;

import java.util.Comparator;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private static RobotController rc;

    private static int slandererCount = 0;
    private static int muckrakerCount = 0;
    private static int politicianCount = 0;
    private static int lastInfluence = 0;
    private static int perTurnProfit = 0;
    private static boolean initialEC;

    private static MapLocation enemyDirection;
    private static MapLocation nearestEnemy;
    private static RobotType nearestEnemyType;
    private static int nearestEnemyDistanceSquared;
    private static int nearestEnemyConviction;

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
        perTurnProfit = rc.getInfluence() - lastInfluence;
        Util.println("Profit = " + perTurnProfit);
        nearestEnemy = null;
        nearestEnemyType = null;
        nearestEnemyDistanceSquared = Integer.MAX_VALUE;
        nearestEnemyConviction = -1;
        LambdaUtil.arraysStreamMin(Cache.ENEMY_ROBOTS,
                Comparator.comparingInt(r -> Cache.MY_LOCATION.distanceSquaredTo(r.getLocation()))).ifPresent(r -> {
            nearestEnemy = r.getLocation();
            nearestEnemyType = r.getType();
            nearestEnemyDistanceSquared = Cache.MY_LOCATION.distanceSquaredTo(nearestEnemy);
            nearestEnemyConviction = r.getConviction();
        });
    }

    public void postTurn() {
        lastInfluence = rc.getInfluence();
    }

    @Override
    public void turn() throws GameActionException {
        preTurn();
        if (nearestEnemy != null) {
            Direction directionToNearestEnemy = Cache.MY_LOCATION.directionTo(nearestEnemy);
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
        if (rc.isReady()) {
            if (reactBuild(influence)) return;
            boolean seesEnemyMuckrakerOrEC = LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS,
                    r -> r.getType() == RobotType.MUCKRAKER || r.getType() == RobotType.ENLIGHTENMENT_CENTER);
            int unitsBuilt = slandererCount + politicianCount + muckrakerCount;
            if (initialEC && unitsBuilt < 30) {
                switch (unitsBuilt % 4) {
                    case 0:
                        if (seesEnemyMuckrakerOrEC) {
                            buildCheapMuckraker();
                        } else {
                            buildSlanderer(influence);
                        }
                        break;
                    case 1:
                        buildCheapMuckraker();
                        break;
                    case 2:
                        buildCheapMuckraker();
                        break;
                    case 3:
                        if (influence >= 300) {
                            buildPolitician(influence);
                        } else {
                            buildCheapPolitician(influence);
                        }
                        break;
                }
                return;
            }
            // no danger? build slanderers / big p
            if (influence >= 150 && Math.random() < 0.2) {
                // build politician w/ minimum 150
                if (rc.getRoundNum() > 250 && Math.random() < 0.5) {
                    if (buildMuckraker(Math.max(influence - 50, 150))) {
                        return;
                    }
                }
                if (buildPolitician(Math.max(influence - 50, 150))) {
                    return;
                }
            }
            boolean foundEnemyEC = !MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).isEmpty();
            double random = Math.random();
            if ((rc.getRoundNum() <= 2 || rc.getRoundNum() >= 30) && (slandererCount == 0 || random < 0.6) && (!seesEnemyMuckrakerOrEC)) {
                if (buildSlanderer(influence)) {
                    return;
                }
            }
            random = Math.random();
            if (random < (foundEnemyEC ? 0.3 : 0.35)) {
                if (buildCheapPolitician(influence)) {
                    return;
                }
            }
            // otherwise, build cheap muckrakers
            buildCheapMuckraker();
        }
    }

    public static void bid(int influence) throws GameActionException {
        if (rc.getTeamVotes() == 751) {
            return;
        }
        if (influence <= 100) {
            return;
        }
        double r = Math.random();
        int amount;
        if (r < 0.2) {
            amount = Math.max(Util.randBetween(1, 3), (int) (0.1 * rc.getInfluence() / (1500 - rc.getRoundNum())));
        } else if (r < 0.4) {
            amount = (int) (0.01 * influence);
        } else {
            amount = (int) (0.02 * influence);
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

    public static boolean reactBuild(int influence) {
        // If there are enemy slanderers in sensing radius, build muckrakers
        if (LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> r.getType() == RobotType.SLANDERER)) {
            if (buildCheapMuckraker()) return true;
        }
        boolean needToDefendAgainstMuckraker =
                CentralUnitTracker.numNearbySmallEnemyMuckrakers > CentralUnitTracker.numNearbySmallDefenders ||
                        (CentralUnitTracker.numNearbyAllySlanderers <= 8 &&
                                CentralUnitTracker.numNearbySmallDefenders < CentralUnitTracker.numNearbyAllySlanderers / 2);
        // ignore small politicians's unless they're really close
        boolean needToDefendAgainstPolitician = nearestEnemyType == RobotType.POLITICIAN &&
                nearestEnemyDistanceSquared <= 25;
        // do we have slanderers? is there danger (muckrakers)? build defender politicians
        if ((slandererCount > 0 && needToDefendAgainstMuckraker) || needToDefendAgainstPolitician) {
            if (nearestEnemy == null) {
                if (buildCheapPolitician(influence)) {
                    return true;
                }
            } else {
                int cost = 5 * nearestEnemyConviction + Constants.POLITICIAN_EMPOWER_PENALTY;
                cost = Math.min(cost, 3 * nearestEnemyConviction + 20);
                cost = Math.min(cost, 2 * nearestEnemyConviction + 30);
                cost = Math.min(cost, nearestEnemyConviction + 50);
                if (influence >= cost) {
                    if (buildPolitician(cost)) {
                        return true;
                    }
                }
            }
            // save for politician - build cheap muckraker
            buildCheapMuckraker();
            return true;
        }
        return false;
    }

    public static boolean buildCheapMuckraker() {
        int influence = Math.max(1, (int) (0.1 * rc.getInfluence() / (1500 - rc.getRoundNum())));
        Direction directionToEnemies = Cache.MY_LOCATION.directionTo(enemyDirection);
        if (directionToEnemies == Direction.CENTER) {
            directionToEnemies = Util.randomAdjacentDirection();
        }
        if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, directionToEnemies, influence)) {
            muckrakerCount++;
            return true;
        }
        return false;
    }

    public static boolean buildMuckraker(int influence) {
        int cap = Math.max(1000, (int) (0.5 * rc.getInfluence() / (1500 - rc.getRoundNum())));
        influence = Math.min(influence, cap);
        Direction directionToEnemies = Cache.MY_LOCATION.directionTo(enemyDirection);
        if (directionToEnemies == Direction.CENTER) {
            directionToEnemies = Util.randomAdjacentDirection();
        }
        if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, directionToEnemies, influence)) {
            muckrakerCount++;
            return true;
        }
        return false;
    }

    public static boolean buildSlanderer(int influence) {
        // check if slanderer will provide negligible eco
        if (rc.getConviction() >= 50000 || rc.getConviction() >= 300 * (1500 - rc.getRoundNum())) return false;
        influence = Math.min(influence, 463);
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

    public static boolean buildCheapPolitician(int influence) {
        int cap = Math.max(Util.randBetween(14, 20), (int) (0.1 * rc.getInfluence() / (1500 - rc.getRoundNum())));
        return buildPolitician(Math.min(influence, cap));
    }

    public static boolean buildPolitician(int influence) {
        if (influence < 12) {
            // 12 is the cheapest
            return false;
        }
        int cap = Math.max(1000, (int) (0.5 * (rc.getInfluence() / (1500 - rc.getRoundNum()))));
        influence = Math.min(influence, cap);
        Direction directionToEnemies = Cache.MY_LOCATION.directionTo(enemyDirection);
        if (directionToEnemies == Direction.CENTER) {
            directionToEnemies = Util.randomAdjacentDirection();
        }
        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, directionToEnemies, influence)) {
            politicianCount++;
            return true;
        }
        return false;
    }
}
