package latticebot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import latticebot.util.Cache;
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
        if (rc.isReady()) {
            if (Cache.ENEMY_ROBOTS.length == 0) {
                // if we don't see any enemy units
                int floored = (rc.getInfluence() / 20) * 20;
                if (floored > 0) {
                    Util.tryBuildRobotTowards(RobotType.SLANDERER, Util.randomAdjacentDirection(), floored);
                } else {
                    Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1);
                }
            } else {
                // defense
                Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1);
            }
        }
    }
}
