package empower;

import battlecode.common.*;
import empower.util.Cache;
import empower.util.Communication;
import empower.util.Constants;
import empower.util.Util;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private RobotController rc;

    private static int muckrakerCount = 0;
    private static int politicianCount = 0;
    private static int extraInfluence = 0;

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
        if (Cache.ENEMY_ROBOTS.length == 0) {
            // if we don't see any enemy units
            double r = Math.random();
            if (r <= 0.5) {
                Util.tryBuildRobotTowards(RobotType.SLANDERER, Util.randomAdjacentDirection(), Math.min(1000, rc.getInfluence() / 2));
            } else if (r <= 0.95) {
                Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1);
            } else {
                if (rc.getInfluence() > 2000) {
                    extraInfluence += (rc.getInfluence() - 2000) / 3;
                }
                Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), 100 + extraInfluence);
            }
        } else {
            // if we see enemies, build politicians for defense
            RobotInfo enemy = Util.getClosestEnemyRobot();
            if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, rc.getLocation().directionTo(enemy.getLocation()), 5*enemy.influence + Constants.POLITICIAN_EMPOWER_PENALTY)) {
                politicianCount++;
            }
        }
    }

    public void bid() throws GameActionException {
        if(rc.getInfluence() <= 100) return;
        double r = Math.random();
        int amount;
        if(r < 0.2) {
            amount = Util.randBetween(1, 3);
        } else if(r < 0.4) {
            amount = (int)(0.01 * rc.getInfluence());
        } else {
            amount = (int)(0.02 * rc.getInfluence());
        }
        rc.bid(amount);
    }
}
