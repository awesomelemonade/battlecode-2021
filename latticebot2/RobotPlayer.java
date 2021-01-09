package latticebot2;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import latticebot2.util.Cache;
import latticebot2.util.Util;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        RobotType robot_type = rc.getType();
        RunnableBot bot = null;
        switch(robot_type) {
            case ENLIGHTENMENT_CENTER:
                bot = new EnlightenmentCenter(rc);
                break;
            case POLITICIAN:
                bot = new Politician(rc);
                break;
            case SLANDERER:
                bot = new Slanderer(rc);
                break;
            case MUCKRAKER:
                bot = new Muckracker(rc);
                break;
        }
        try {
            Util.init(rc);
            bot.init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        boolean errored = false;
        while (true) {
            try {
                while (true) {
                    if (errored) {
                        // Errored - RED
                        rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
                    }
                    int currentTurn = rc.getRoundNum();
                    Util.loop();
                    bot.turn();
                    if (rc.getRoundNum() != currentTurn) {
                        // We ran out of bytecodes! - MAGENTA
                        rc.setIndicatorDot(rc.getLocation(), 255, 0, 255);
                        int over = Clock.getBytecodeNum() + (rc.getRoundNum() - currentTurn - 1) * rc.getType().bytecodeLimit;
                        System.out.println(rc.getID() + " out of bytecodes: " + Cache.TURN_COUNT + " (over by " + over + ")");
                    }
                    Clock.yield();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                errored = true;
                Clock.yield();
            }
        }
    }
}
