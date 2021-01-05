package ppbot;
import battlecode.common.*;
import ppbot.util.Util;

public strictfp class Muckracker implements RunnableBot {
    private RobotController rc;

    public Muckracker(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
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
        if (Util.tryMove(Util.randomAdjacentDirection()))
            System.out.println("I moved!");
    }
}
