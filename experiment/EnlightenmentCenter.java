package experiment;

import battlecode.common.*;
import experiment.util.Cache;
import experiment.util.CentralCommunication;
import experiment.util.Constants;
import experiment.util.LambdaUtil;
import experiment.util.MapInfo;
import experiment.util.SlandererBuild;
import experiment.util.Util;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private static RobotController rc;

    private static int slandererCount = 0;
    private static int muckrakerCount = 0;
    private static int politicianCount = 0;
    private static int eM = 0, eS = 0, eP = 0;
    private static int lastInfluence = 0;
    private static int perTurnProfit = 0;
    private static int turnsSincePolitician = 0;

    private static MapLocation enemyDirection;

    public EnlightenmentCenter(RobotController rc) {
        EnlightenmentCenter.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        enemyDirection = rc.getLocation();
        lastInfluence = rc.getInfluence();
    }

    public void preTurn() {
        perTurnProfit = rc.getInfluence() - lastInfluence;
        Util.println("Profit = " + perTurnProfit);
        turnsSincePolitician++;
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
            /*int unitsBuilt = slandererCount + politicianCount + muckrakerCount;
            if(unitsBuilt < 12 && rc.getRoundNum() <= 50) {
                switch(unitsBuilt%4) {
                    case 0:
                        buildSlanderer(influence);
                        break;
                    case 1:
                        buildCheapMuckraker();
                        break;
                    case 2:
                        buildCheapMuckraker();
                        break;
                    case 3:
                        buildCheapPolitician();
                        break;
                }
            }*/
            // If there are enemy slanderers in sensing radius, build muckrakers
            if (LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> r.getType() == RobotType.SLANDERER)) {
                buildCheapMuckraker();
                return;
            }
            // if too long with no politician built, build one
            if(turnsSincePolitician >= 10) {
                buildPolitician(Math.max(latticebot.util.Util.randBetween(16, 20), (int)(0.1*influence)));
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
                if (influence >= cost) {
                    buildPolitician(cost);
                } else {
                    // save for politician - build 1 cost muckraker
                    buildCheapMuckraker();
                }
                return;
            }
            // find smallest neutral/enemy EC (in vision)
            if (LambdaUtil.arraysStreamMin(Cache.ALL_ROBOTS,
                    r -> r.getTeam() != Constants.ALLY_TEAM && r.getType() == RobotType.ENLIGHTENMENT_CENTER,
                    r -> r.getConviction() + Constants.POLITICIAN_EMPOWER_PENALTY).map(cost -> {
                if (influence >= cost) {
                    buildPolitician(influence);
                    return true;
                } else {
                    return false;
                }
            }).orElse(false)) {
                return;
            }
            // TODO: neutral ec (communicated)
            // TODO: enemy ec (communicated)
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
            eS = 0;
            eM = 0;
            eP = 0;
            for (RobotInfo r : Cache.ENEMY_ROBOTS) {
                switch (r.getType()) {
                    case SLANDERER:
                        eS++;
                        break;
                    case MUCKRAKER:
                        eM++;
                        break;
                    case POLITICIAN:
                        eP++;
                        break;
                }
            }

            double random = Math.random();
            if ((rc.getRoundNum() <= 2 || rc.getRoundNum() >= 30) && (slandererCount == 0 || random < 0.4) && eM == 0) {
                if (buildSlanderer(influence)) {
                    return;
                }
            }
            random = Math.random();
            if (random < (foundEnemyEC ? 0.4 : 0.45)) {
                if(buildCheapPolitician()) {
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
            amount = Math.max(Util.randBetween(1,3), (int) (0.1 * rc.getInfluence() / (1500 - rc.getRoundNum())));
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

    public static boolean buildCheapMuckraker() {
        int influence = Math.max(1, (int)(0.1*rc.getInfluence()/(1500-rc.getRoundNum())));
        if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), influence)) {
            muckrakerCount++;
            return true;
        }
        return false;
    }

    public static boolean buildMuckraker(int influence) {
        int cap = Math.max(1000, (int)(0.5*rc.getInfluence()/(1500-rc.getRoundNum())));
        influence = Math.min(influence, cap);
        if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), influence)) {
            muckrakerCount++;
            return true;
        }
        return false;
    }

    public static boolean buildSlanderer(int influence) {
        // check if slanderer will provide negligible eco
        if(rc.getConviction() >= 50000 || rc.getConviction() >= 300*(1500-rc.getRoundNum())) return false;
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
        int influence = Math.max(Util.randBetween(20, 30), (int)(0.1*rc.getInfluence()/(1500-rc.getRoundNum())));
        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), influence)) {
            politicianCount++;
            turnsSincePolitician = 0;
            return true;
        }
        return false;
    }

    public static boolean buildPolitician(int influence) {
        int cap = Math.max(1000, (int)(0.5*(rc.getInfluence()/(1500-rc.getRoundNum()))));
        influence = Math.min(influence, cap);
        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), influence)) {
            politicianCount++;
            turnsSincePolitician = 0;
            return true;
        }
        return false;
    }
}
