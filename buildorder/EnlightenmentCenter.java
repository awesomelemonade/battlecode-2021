package buildorder;

import battlecode.common.*;
import buildorder.util.Cache;
import buildorder.util.CentralCommunication;
import buildorder.util.Constants;
import buildorder.util.SlandererBuild;
import buildorder.util.Util;

import java.util.Optional;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private RobotController rc;

    private static int slandererCount = 0;
    private static int muckrakerCount = 0;
    private static int politicianCount = 0;

    private static MapLocation enemyDirection;

    public EnlightenmentCenter(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        enemyDirection = rc.getLocation();
    }

    @Override
    public void turn() throws GameActionException {
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
            buildMuckraker();
            return;
        }
        boolean danger = CentralCommunication.nearestEnemy != null
                && CentralCommunication.nearestEnemyDistanceSquared <= 25;
        if (rc.getRoundNum() == 1 && !danger) {
            buildSlanderer();
        }
        if (rc.getConviction() >= 300) {
            buildBigP();
        }
        if (rc.getRoundNum() <= 30) {
            double r = Math.random();
            if (r <= 0.7) {
                buildMuckraker();
            } else {
                if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(),
                        Util.randBetween(18, 21))) {
                    politicianCount++;
                }
            }
        }
        if (rc.getInfluence() <= 100) {
            buildMuckraker();
        } else {
            if (!danger && Math.random() <= 0.5) {
                buildSlanderer();
            } else {
                if (Math.random() <= 0.3) {
                    buildMuckraker();
                } else {
                    if (rc.getInfluence() >= 300) {
                        if (buildBigP())
                            return;
                    } else {
                        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(),
                                Util.randBetween(18, 21))) {
                            politicianCount++;
                            return;
                        }
                    }
                }
            }
        }
    }

    public void bid() throws GameActionException {
        if (rc.getInfluence() <= 100)
            return;
        if (rc.getTeamVotes() == 1500) {
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
        int influence = Math.min(800, rc.getConviction() - 50);
        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), influence)) {
            politicianCount++;
            return true;
        }
        return false;
    }
}
