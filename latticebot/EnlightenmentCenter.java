package latticebot;

import battlecode.common.*;
import latticebot.util.Cache;
import latticebot.util.CentralCommunication;
import latticebot.util.Constants;
import latticebot.util.SlandererBuild;
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
        System.out.println("EC Turn: buff=" + rc.getEmpowerFactor(Constants.ALLY_TEAM, 0));
        bid();
        if (!rc.isReady()) {
            return;
        }
        if (CentralCommunication.nearestEnemy == null || CentralCommunication.nearestEnemyDistanceSquared > 225) {
            // if we don't see any enemy units
            if (rc.getInfluence() > 20 && Math.random() >= 0.7) {
                Util.tryBuildRobotTowards(RobotType.SLANDERER, Util.randomAdjacentDirection(), SlandererBuild.getBuildInfluence(rc.getInfluence()));
            } else {
                Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1);
            }
        } else {
            // if we see muckrakers, build politicians for defense
            if (CentralCommunication.nearestEnemyType == RobotType.MUCKRAKER) {
                Util.tryBuildRobotTowards(RobotType.POLITICIAN, Cache.MY_LOCATION.directionTo(CentralCommunication.nearestEnemy),
                        CentralCommunication.nearestEnemyInfluence + Constants.POLITICIAN_EMPOWER_PENALTY);
            } else {
                Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1);
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
