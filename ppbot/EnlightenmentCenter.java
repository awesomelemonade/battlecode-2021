package ppbot;
import battlecode.common.*;
import ppbot.util.Constants;
import ppbot.util.Util;

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
        RobotType toBuild = Util.randomSpawnableRobotType();
        int influence = 50;
        for (Direction dir : Constants.ORDINAL_DIRECTIONS) {
            System.out.println("can build?");
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
                System.out.println("building?");
            } else {
                break;
            }
        }
    }
}
