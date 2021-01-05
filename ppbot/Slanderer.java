package ppbot;
import battlecode.common.*;
import static ppbot.Constants.*;

public strictfp class Slanderer {
    public RobotController rc;
    public Constants C;

    public Slanderer(RobotController rc) {
        this.rc = rc;
        this.C = new Constants(this.rc);
    }

    public void run() throws GameActionException {
        if (C.tryMove(C.randomDirection()))
            System.out.println("I moved!");
    }
}
