package ppbot;
import battlecode.common.*;
import static ppbot.Constants.*;

public strictfp class Muckracker {
    public RobotController rc;
    public Constants C;

    public Muckracker(RobotController rc) {
        this.rc = rc;
        this.C = new Constants(this.rc);
    }

    public void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (C.tryMove(C.randomDirection()))
            System.out.println("I moved!");
    }
}
