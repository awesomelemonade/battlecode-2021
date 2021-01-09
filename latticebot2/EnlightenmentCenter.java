package latticebot2;

import battlecode.common.*;
import latticebot2.util.Cache;
import latticebot2.util.Communication;
import latticebot2.util.Constants;
import latticebot2.util.Util;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private RobotController rc;

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
            if (rc.getInfluence() >= 110 && Math.random() >= 0.3) {
                Util.tryBuildRobotTowards(RobotType.SLANDERER, Util.randomAdjacentDirection(), 100);
            } else {
                Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1);
            }
        } else {
            // if we see muckrakers, build politicians for defense
            RobotInfo muckraker = Util.getClosestEnemyRobot(x -> x.getType() == RobotType.MUCKRAKER);
            // defense
            if (muckraker == null) {
                Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1);
            } else {
                Util.tryBuildRobotTowards(RobotType.POLITICIAN, rc.getLocation().directionTo(muckraker.getLocation()),
                        muckraker.influence + Constants.POLITICIAN_EMPOWER_PENALTY);
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
