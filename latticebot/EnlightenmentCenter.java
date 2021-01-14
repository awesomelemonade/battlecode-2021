package latticebot;

import battlecode.common.*;
import latticebot.util.Cache;
import latticebot.util.CentralCommunication;
import latticebot.util.Constants;
import latticebot.util.LambdaUtil;
import latticebot.util.MapInfo;
import latticebot.util.SlandererBuild;
import latticebot.util.Util;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private static RobotController rc;

    private static int slandererCount = 0;
    private static int muckrakerCount = 0;
    private static int politicianCount = 0;
    private static int turnsSincePolitician = 0;

    private static MapLocation enemyDirection;

    public EnlightenmentCenter(RobotController rc) {
        EnlightenmentCenter.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        enemyDirection = rc.getLocation();
    }

    @Override
    public void turn() throws GameActionException {
        if (CentralCommunication.nearestEnemy != null) {
            Direction directionToNearestEnemy = Cache.MY_LOCATION.directionTo(CentralCommunication.nearestEnemy);
            enemyDirection = enemyDirection.add(directionToNearestEnemy);
        }
        turnsSincePolitician++;
        int saveAmount = getSaveAmount();
        Util.println("EC Turn: save=" + saveAmount + ", buff=" + rc.getEmpowerFactor(Constants.ALLY_TEAM, 0));
        int influenceLeft = rc.getInfluence() - saveAmount - 1;
        buildUnit(influenceLeft);
        influenceLeft = rc.getInfluence() - saveAmount - 1;
        bid(influenceLeft);
    }

    public static void buildUnit(int influence) {
        if (rc.isReady()) {
            // If there are enemy slanderers in sensing radius, build muckrakers
            if (LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> r.getType() == RobotType.SLANDERER)) {
                buildMuckraker();
                return;
            }
            // if too long with no politician built, build one
            if(turnsSincePolitician >= 10) {
                buildPolitician(Math.max(Util.randBetween(16, 20), (int)(0.1*influence)));
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
                    buildMuckraker();
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
            double random = Math.random();
            if ((rc.getRoundNum() <= 2 || rc.getRoundNum() >= 30) && (slandererCount == 0 || random < 0.4)) {
                if (buildSlanderer(influence)) {
                    return;
                }
            }
            random = Math.random();
            if (random < (foundEnemyEC ? 0.4 : 0.45)) {
                int cost = Util.randBetween(10, 20) + Constants.POLITICIAN_EMPOWER_PENALTY;
                if (influence >= cost) {
                    if (buildPolitician(cost)) {
                        return;
                    }
                }
            }
            // otherwise, build 1 cost muckrakers
            buildMuckraker();
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
            amount = Util.randBetween(1, 3);
        } else if (r < 0.4) {
            amount = (int) (0.01 * influence);
        } else {
            amount = (int) (0.02 * influence);
        }
        rc.bid(amount);
    }

    public static int getSaveAmount() {
        // Save if we see enemy EC or enemy politician
        return LambdaUtil.arraysStreamSum(Cache.ALL_ROBOTS,
                r -> r.getTeam() != Constants.ALLY_TEAM &&
                        (r.getType() == RobotType.ENLIGHTENMENT_CENTER || r.getType() == RobotType.POLITICIAN),
                r -> Math.max(r.getConviction() - Constants.POLITICIAN_EMPOWER_PENALTY + 1, 0));
    }

    public static boolean buildMuckraker() {
        if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1)) {
            muckrakerCount++;
            return true;
        }
        return false;
    }

    public static boolean buildMuckraker(int influence) {
        if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), influence)) {
            muckrakerCount++;
            return true;
        }
        return false;
    }

    public static boolean buildSlanderer(int influence) {
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

    public static boolean buildPolitician(int influence) {
        turnsSincePolitician = 0;
        int cap = Math.max(1000, (int)(0.1 * rc.getInfluence()));
        influence = Math.min(influence, cap);
        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), influence)) {
            politicianCount++;
            return true;
        }
        return false;
    }
}
