package latticebot;

import battlecode.common.*;
import latticebot.util.Cache;
import latticebot.util.Constants;
import latticebot.util.Pathfinder;
import latticebot.util.Util;

public strictfp class Politician implements RunnableBot {
    private RobotController rc;

    public Politician(RobotController rc) {
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
        if (Cache.ENEMY_ROBOTS.length == 0) {
            Util.randomExplore();
        } else {
            int actionRadiusSquared = rc.getType().actionRadiusSquared;
            // we want to find square that can target only 1 muckraker
            for (int i = 0; i < Constants.FLOOD_OFFSET_X_20.length; i++) {
                MapLocation location = rc.getLocation().translate(Constants.FLOOD_OFFSET_X_20[i], Constants.FLOOD_OFFSET_Y_20[i]);
                RobotInfo closestRobot = Util.getClosestRobot(location, actionRadiusSquared);
                if (closestRobot == null || closestRobot.getTeam() == Constants.ALLY_TEAM) {
                    continue;
                }
                int distanceSquared = location.distanceSquaredTo(closestRobot.getLocation());
                boolean isolated = true;
                for (RobotInfo robot : Cache.ALL_ROBOTS) {
                    if (robot == closestRobot) {
                        continue;
                    }
                    if (robot.getLocation().isWithinDistanceSquared(location, distanceSquared)) {
                        isolated = false;
                        break;
                    }
                }
                if (isolated) {
                    if (i == 0) { // we're on the target square
                        if (rc.canEmpower(distanceSquared)) {
                            rc.empower(distanceSquared);
                        }
                    } else {
                        Pathfinder.execute(location);
                    }
                    break;
                }
            }
        }
    }
}
