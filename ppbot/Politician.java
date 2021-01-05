package ppbot;
import battlecode.common.*;
import static ppbot.Constants.*;

public strictfp class Politician {
    public RobotController rc;
    public Constants C;

    public Politician(RobotController rc) {
        this.rc = rc;
        this.C = new Constants(this.rc);
    }

    public void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (C.tryMove(C.randomDirection()))
            System.out.println("I moved!");
    }
}
