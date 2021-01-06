package pathingtest;
import battlecode.common.*;
import pathingtest.util.Cache;
import pathingtest.util.Util;
import static pathingtest.util.Constants.*;

public strictfp class Slanderer implements RunnableBot {
    private RobotController rc;

    private MapLocation dest_loc;

    public Slanderer(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        //int x = ((int)Math.random()*40) - 10;
        //int y = ((int)Math.random()*40) - 10;
        int x = 40;
        int y = 40;
        dest_loc = rc.getLocation().translate(x, y);
        System.out.println("My destination is (" + Integer.toString(dest_loc.x) + ", " + Integer.toString(dest_loc.y) + ")");
    }

    @Override
    public void turn() throws GameActionException {
        Util.tryMoveSmart(dest_loc, 1);
    }
}
