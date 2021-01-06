package pathingtest;

import battlecode.common.*;
import pathingtest.util.Cache;
import pathingtest.util.Util;
import static pathingtest.util.Constants.*;

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
        if(Cache.TURN_COUNT == 1) {
            if (rc.getInfluence() >= 100) {
                produceUnitAnywhere(RobotType.SLANDERER, rc.getInfluence()/2);
            }
        }
    }

    private int produceUnitAnywhere(RobotType t, int influence) throws GameActionException {
        for (int i = 0; i < ORDINAL_DIRECTIONS.length; i++) {
            MapLocation next_loc = Cache.MY_LOCATION.add(ORDINAL_DIRECTIONS[i]);
            if (rc.canBuildRobot(t, ORDINAL_DIRECTIONS[i], influence)) {
                rc.buildRobot(t, ORDINAL_DIRECTIONS[i], influence);
                System.out.println("DO BUILD");
                System.out.println("ID is: " + rc.senseRobotAtLocation(next_loc).getID());
                return rc.senseRobotAtLocation(next_loc).getID();
            }
        }
        return -1;
    }
}
