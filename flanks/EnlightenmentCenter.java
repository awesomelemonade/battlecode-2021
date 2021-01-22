package flanks;

import battlecode.common.*;
import flanks.util.Cache;
import flanks.util.CentralCommunication;
import flanks.util.Constants;
import flanks.util.LambdaUtil;
import flanks.util.MapInfo;
import flanks.util.SlandererBuild;
import flanks.util.Util;

import java.util.Comparator;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private static RobotController rc;

    private static int slandererCount = 0;
    private static int muckrakerCount = 0;
    private static int politicianCount = 0;
    private static int lastInfluence = 0;
    private static int perTurnProfit = 0;
    private static int turnsSinceSelfEmpowerer = 0;
    private static int earlyCheapPMCounter = 0;
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
                            buildSlanderer(influence - 10);
                        }
                        break;
                    case 2:
                        // buildCheapMuckraker();
                        buildMuckraker(1);
                        break;
                    case 3:
                        buildMuckraker(1);
                        break;
                    case 1:
                        if (influence >= 300) {
                            buildPolitician(influence - 100);
                        } else {
                            if(earlyCheapPMCounter % 4 == 1 || (politicianCount < 3)) {
                                buildCheapPolitician();
                            } else {
                                buildMuckraker(16);
                            }
                            earlyCheapPMCounter++;
                        }
                        break;
                }
                return;
            }
            if (rc.getEmpowerFactor(Constants.ALLY_TEAM, 15) >= 2 && turnsSinceSelfEmpowerer >= 11) {
                int cost = influence / 2;
                if (buildSelfEmpowerer(cost)) {
                    return;
                } else {
                    if (buildCheapMuckraker()) return;
                }
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
            if ((rc.getRoundNum() <= 2 || rc.getRoundNum() >= 30) && (slandererCount == 0 || random < 0.4) && (!seesEnemyMuckrakerOrEC)) {
                if (buildSlanderer(influence)) {
                    return;
                }
            }
            random = Math.random();
            if (random < (foundEnemyEC ? 0.4 : 0.45)) {
                if (buildCheapPolitician()) {
                    return;
                }
            }
            // otherwise, build 1 cost muckrakers
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
        if (Util.tryBuildRobotTowardsRandomDirection(RobotType.MUCKRAKER, influence)) {
            muckrakerCount++;
            return true;
        }
        return false;
    }

    public static boolean buildMuckraker(int influence) {
        int cap = Math.max(1000, (int) (0.5 * rc.getInfluence() / (1500 - rc.getRoundNum())));
        influence = Math.min(influence, cap);
        if (Util.tryBuildRobotTowardsRandomDirection(RobotType.MUCKRAKER, influence)) {
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

    public static boolean buildCheapPolitician() {
        int influence = Math.max(Util.randBetween(14, 20), (int) (0.1 * rc.getInfluence() / (1500 - rc.getRoundNum())));
        if (influence % 10 == 6) influence++;
        if (Util.tryBuildRobotTowardsRandomDirection(RobotType.POLITICIAN, influence)) {
            politicianCount++;
            return true;
        }
        return false;
    }

    public static boolean buildPolitician(int influence) {
        int cap = Math.max(1000, (int) (0.5 * (rc.getInfluence() / (1500 - rc.getRoundNum()))));
        influence = Math.min(influence, cap);
        if (influence % 10 == 6) influence++;
        if (Util.tryBuildRobotTowardsRandomDirection(RobotType.POLITICIAN, influence)) {
            politicianCount++;
            return true;
        }
        return false;
    }

    public static boolean buildSelfEmpowerer(int influence) {
        influence = influence + 6 - (influence % 10);
        if (influence <= 50) return false;
        if (Util.tryBuildRobotTowardsRandomDirection(RobotType.POLITICIAN, influence)) {
            politicianCount++;
            turnsSinceSelfEmpowerer = 0;
            return true;
        }
        return false;
    }
}
