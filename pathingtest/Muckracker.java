package pathingtest;

import battlecode.common.*;
import pathingtest.util.Cache;
import pathingtest.util.Util;
import static pathingtest.util.Constants.*;

public strictfp class Muckracker implements RunnableBot {
    private RobotController rc;

    private MapLocation cur_loc;
    private MapLocation spawnEC;

    private int initialExploreDirection;

    private int minX = -1, minY = -1, maxX = -1, maxY = -1;
    private int flag = 0;

    private MapLocation enemyEC;

    public Muckracker(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {

    }
}
