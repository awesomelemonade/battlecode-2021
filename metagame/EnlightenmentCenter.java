package metagame;

import battlecode.common.*;
import metagame.util.Cache;
import metagame.util.Communication;
import metagame.util.Constants;
import metagame.util.Util;

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
            if (rc.getInfluence() >= 110 && Math.random() >= 0.3) {
                Util.tryBuildRobotTowards(RobotType.SLANDERER, Util.randomAdjacentDirection(), Math.min(1000, rc.getInfluence() / 2));
            } else if (muckrakerCount < 30 || muckrakerCount < politicianCount) { // shitty heuristics for now
                if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1)) {
                    muckrakerCount++;
                }
            } else {
                if (rc.getInfluence() > 2000) {
                    extraInfluence += rc.getInfluence() / 3;
                }
                Util.tryBuildRobotTowards(RobotType.POLITICIAN, Util.randomAdjacentDirection(), (int)(Math.random() * 10 + 21 + extraInfluence)); // [21, 31]
            }
        } else {
            // if we see muckrakers, build politicians for defense
            RobotInfo muckraker = Util.getClosestEnemyRobot(x -> x.getType() == RobotType.MUCKRAKER);
            // defense
            if (muckraker == null) {
                if (Util.tryBuildRobotTowards(RobotType.MUCKRAKER, Util.randomAdjacentDirection(), 1)) {
                    muckrakerCount++;
                }
            } else {
                if (Util.tryBuildRobotTowards(RobotType.POLITICIAN, rc.getLocation().directionTo(muckraker.getLocation()), muckraker.influence + Constants.POLITICIAN_EMPOWER_PENALTY)) {
                    politicianCount++;
                }
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
