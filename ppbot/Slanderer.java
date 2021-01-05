package ppbot;
import battlecode.common.*;
import ppbot.util.Constants;
import ppbot.util.Util;

public strictfp class Slanderer implements RunnableBot {
    public RobotController rc;

    public Slanderer(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        Util.tryRandomMove();
    }
}
