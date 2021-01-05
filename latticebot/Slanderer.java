package latticebot;

import battlecode.common.*;
import latticebot.util.*;

public strictfp class Slanderer implements RunnableBot {
    private RobotController rc;
    private Politician politician;

    public Slanderer(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        RobotInfo closestEnemy = Util.getClosestEnemyRobot();
        if (rc.getType() == RobotType.SLANDERER) {
            if (closestEnemy == null) {
                // lattice
                if (LatticeUtil.isLatticeLocation(rc.getLocation())) {
                    // do nothing
                } else {
                    MapLocation target = LatticeUtil.getClosestLatticeLocation(rc.getLocation());
                    if (target == null) {
                        Util.randomExplore();
                    } else {
                        Pathfinder.execute(target);
                    }
                }
            } else {
                tryKiteFrom(closestEnemy.getLocation());
            }
        } else {
            // Camouflage
            if (politician == null) {
                politician = new Politician(rc);
                politician.init();
            }
            politician.turn();
        }
    }

    public boolean tryKiteFrom(MapLocation location) throws GameActionException {
        // TODO: Use passability
        return Util.tryMoveTowards(location.directionTo(rc.getLocation()));
    }
}
