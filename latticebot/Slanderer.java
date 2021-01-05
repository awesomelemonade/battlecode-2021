package latticebot;

import battlecode.common.*;
import latticebot.util.*;

public strictfp class Slanderer implements RunnableBot {
    private RobotController rc;

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
            if (closestEnemy == null) {
                Util.randomExplore();
            } else {
                int enemyDistanceSquared = rc.getLocation().distanceSquaredTo(closestEnemy.getLocation());
                int actionRadiusSquared = rc.getType().actionRadiusSquared;
                if (enemyDistanceSquared <= actionRadiusSquared && rc.canEmpower(actionRadiusSquared)) {
                    rc.empower(actionRadiusSquared);
                } else {
                    Pathfinder.execute(closestEnemy.getLocation());
                }
            }
        }
    }

    public boolean tryKiteFrom(MapLocation location) throws GameActionException {
        // TODO: Use passability
        return Util.tryMoveTowards(location.directionTo(rc.getLocation()));
    }
}
