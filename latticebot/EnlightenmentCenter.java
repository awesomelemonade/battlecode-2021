package latticebot;

import battlecode.common.*;
import latticebot.util.Cache;
import latticebot.util.Communication;
import latticebot.util.Constants;
import latticebot.util.Util;

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
        if(rc.getInfluence() <= 500) return;
        double r = Math.random();
        int amount;
        if(r < 0.2) {
            return;
        } else if(r < 0.4) {
            amount = (int)(0.01 * rc.getInfluence());
        } else {
            amount = (int)(0.02 * rc.getInfluence());
        }
        System.out.println(amount);
        rc.bid(amount);
    }
}
