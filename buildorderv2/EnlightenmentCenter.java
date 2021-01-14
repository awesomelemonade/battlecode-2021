package buildorderv2;

import battlecode.common.*;
import buildorderv2.util.Cache;
import buildorderv2.util.CentralCommunication;
import buildorderv2.util.Constants;
import buildorderv2.util.SlandererBuild;
import buildorderv2.util.Util;

import java.util.Optional;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private RobotController rc;

    private static int slandererCount = 0;
    private static int muckrakerCount = 0;
    private static int politicianCount = 0;
    private static boolean danger;
    private static int safetyTimer = 0; // currently rounds since an enemy was near our base; maybe rounds since last slanderer died is better

    private static MapLocation enemyDirection;

    public EnlightenmentCenter(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        enemyDirection = rc.getLocation();
    }

    public void preTurn() {
        danger = CentralCommunication.nearestEnemy != null
                && CentralCommunication.nearestEnemyDistanceSquared <= 25;
        if (rc.getRoundNum() <= 50 || (CentralCommunication.nearestEnemy != null && CentralCommunication.nearestEnemyDistanceSquared <= 100)) {
            safetyTimer = 0;
        } else {
            safetyTimer++;
        }
    }

    @Override
    public void turn() throws GameActionException {
        preTurn();
        System.out.println("EC Turn: buff=" + rc.getEmpowerFactor(Constants.ALLY_TEAM, 0));
        if (CentralCommunication.nearestEnemy != null) {
            Direction directionToNearestEnemy = Cache.MY_LOCATION.directionTo(CentralCommunication.nearestEnemy);
            enemyDirection = enemyDirection.add(directionToNearestEnemy);
        }
        bid();
        if (!rc.isReady()) {
            return;
        }
        if (buildDefensivePolitician()) {
            return;
        }
        if (shouldSave()) {
            if (buildMuckraker()) return;
        }
        if (rc.getRoundNum() == 1 && !danger) {
            if(buildSlanderer()) return;
        }
        if (rc.getInfluence() >= 300) {
            if(safetyTimer <= 80) {
                if(Math.random() <= 0.2) {
                    if(buildMuckraker()) return;
                }
                if(buildBigP()) return;
            } else {
                if(rc.getInfluence() >= 1000) {
                    double r = Math.random();
                    if(r <= 0.5) {
                        if(buildBigP()) return;
                    } else if (r <= 0.95) {
                        if(buildSlanderer()) return;
                    } else {
                        if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(),
                                Math.min(1000, rc.getInfluence() - 500))) {
                            muckrakerCount++;
                            return;
                        }
                    }
                }
            }
        }
        if (rc.getRoundNum() <= 30) {
            double r = Math.random();
            if (r <= 0.7) {
                if(buildMuckraker()) {
                    return;
                }
            } else {
                if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(),
                        Util.randBetween(18, 21))) {
                    politicianCount++;
                    return;
                }
            }
        }
        if (rc.getInfluence() <= 100) {
            if(buildMuckraker()) return;
        } else {
            double r = Math.random();
            if (!danger && r <= 0.5) {
                if(buildSlanderer()) return;
            } else if (r <= 0.65) {
                if(buildMuckraker()) return;
            } else {
                if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(),
                        Util.randBetween(18, 21))) {
                    politicianCount++;
                    return;
                }
            }
        }
    }

    public void bid() throws GameActionException {
        if (rc.getInfluence() <= 100)
            return;
        if (rc.getTeamVotes() == 751) {
            return;
        }
        double r = Math.random();
        int amount;
        if (r < 0.2) {
            amount = Util.randBetween(1, 3);
        } else if (r < 0.4) {
            amount = (int) (0.01 * rc.getInfluence());
        } else {
            amount = (int) (0.02 * rc.getInfluence());
        }
        rc.bid(amount);
    }

    public boolean shouldSave() {
        if (CentralCommunication.nearestEnemy == null)
            return false;
        if (CentralCommunication.nearestEnemyType == RobotType.POLITICIAN
                && CentralCommunication.nearestEnemyDistanceSquared <= 100
                && CentralCommunication.nearestEnemyInfluence >= 50) {
            return true;
        }
        return false;
    }

    public boolean buildMuckraker() throws GameActionException {
        if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1)) {
            muckrakerCount++;
            return true;
        }
        return false;
    }

    public boolean buildSlanderer() throws GameActionException {
        Direction awayFromEnemies = enemyDirection.directionTo(Cache.MY_LOCATION);
        if (awayFromEnemies == Direction.CENTER) {
            awayFromEnemies = Util.randomAdjacentDirection();
        }
        if (Util.tryBuildRobotTowards(RobotType.SLANDERER, awayFromEnemies,
                SlandererBuild.getBuildInfluence(rc.getInfluence() - 5))) {
            slandererCount++;
            return true;
        } else {
            return false;
        }
    }

    public boolean buildDefensivePolitician() throws GameActionException {
        if (CentralCommunication.nearestEnemy == null) {
            return false;
        }
        if (CentralCommunication.nearestEnemyType == RobotType.MUCKRAKER
                && CentralCommunication.nearestEnemyDistanceSquared > 64) {
            return false;
        }
        if (CentralCommunication.nearestEnemyType == RobotType.POLITICIAN
                && CentralCommunication.nearestEnemyDistanceSquared > 16) {
            return false;
        }
        if (CentralCommunication.nearestEnemyType == RobotType.ENLIGHTENMENT_CENTER) {
            return false;
        }
        int influence = 5 * CentralCommunication.nearestEnemyInfluence + Constants.POLITICIAN_EMPOWER_PENALTY;
        influence = Math.max(influence, 12);
        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN,
                Cache.MY_LOCATION.directionTo(CentralCommunication.nearestEnemy), influence)) {
            politicianCount++;
            return true;
        }
        return false;
    }

    public boolean buildBigP() {
        if (rc.getConviction() <= 100)
            return false;
        int influence = rc.getConviction() - 50;
        if(rc.getInfluence() >= 1000) {
            influence = Math.min(influence, (int)(0.3*rc.getInfluence()));
        }
        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), influence)) {
            politicianCount++;
            return true;
        }
        return false;
    }
}
