package ppbot;
import battlecode.common.*;
import static ppbot.Constants.*;

public strictfp class EnlightenmentCenter {
    public RobotController rc;
    public Constants C;

    public EnlightenmentCenter(RobotController rc) {
        this.rc = rc;
        this.C = new Constants(this.rc);
    }

    public void run() throws GameActionException {
        RobotType toBuild = C.randomSpawnableRobotType();
        int influence = 50;
        for (Direction dir : directions) {
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
