package bigps;

import battlecode.common.*;
import bigps.util.Cache;
import bigps.util.Communication;
import bigps.util.Constants;
import bigps.util.Util;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private RobotController rc;

    private static int slandererCount = 0;
    private static int muckrakerCount = 0;
    private static int politicianCount = 0;

    public EnlightenmentCenter(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        bid();
        if (!rc.isReady()) {
            return;
        }
        if (buildDefensivePolitician()) {
            return;
        }
        if (rc.getRoundNum() == 1) {
            if (Util.tryBuildRobotTowards(RobotType.SLANDERER, Util.randomAdjacentDirection(),
                    rc.getInfluence() - 10)) {
                slandererCount++;
            }
        } else if (rc.getRoundNum() <= 200) {
            if (rc.getInfluence() >= 200) {
                int influence = Math.min(300, rc.getInfluence() - 50);
                if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), influence)) {
                    politicianCount++;
                    return;
                }
            }
            if (politicianCount <= 3) {
                if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(),
                        Util.randBetween(18, 21))) {
                    politicianCount++;
                    return;
                }
            }
            if (Math.random() <= 0.8 || rc.getInfluence() <= 100) {
                if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1)) {
                    muckrakerCount++;
                    return;
                }
            } else {
                if (Util.tryBuildRobotTowards(RobotType.SLANDERER, Util.randomAdjacentDirection(),
                        Math.min(949, rc.getInfluence() / 2))) {
                    slandererCount++;
                    return;
                }
            }
        } else {
            double r = Math.random();
            if (r <= 0.3) {
                if (Util.tryBuildRobotTowards(RobotType.SLANDERER, Util.randomAdjacentDirection(),
                        Math.min(949, rc.getInfluence() / 2))) {
                    slandererCount++;
                    return;
                }
            } else if (r <= 0.6) {
                if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1)) {
                    muckrakerCount++;
                    return;
                }
            } else {
                if (politicianCount % 5 == 0 && rc.getInfluence() >= 1000) {
                    int influence = Math.min(500, rc.getInfluence() - 50);
                    if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), influence)) {
                        politicianCount++;
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
        }
    }

    public void bid() throws GameActionException {
        if (rc.getInfluence() <= 100)
            return;
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

    public boolean buildDefensivePolitician() throws GameActionException {
        if (Cache.ENEMY_ROBOTS.length == 0)
            return false;
        RobotInfo enemy = Util.getClosestEnemyRobot();
        if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, rc.getLocation().directionTo(enemy.getLocation()),
                5 * enemy.influence + Constants.POLITICIAN_EMPOWER_PENALTY)) {
            politicianCount++;
            return true;
        }
        return false;
    }
}
