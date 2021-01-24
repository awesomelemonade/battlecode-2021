package combobot3_old;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import combobot3_old.util.Cache;
import combobot3_old.util.Constants;
import combobot3_old.util.Util;

public strictfp class RobotPlayer {
    public static int currentTurn;
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
                bot = new Muckraker(rc);
                break;
        }
        try {
            Util.init(rc);
            bot.init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        boolean errored = false;
        boolean overBytecodes = false;
        while (true) {
            try {
                while (true) {
                    currentTurn = rc.getRoundNum();
                    if (Constants.DEBUG_RESIGN && (currentTurn >= 500 || currentTurn >= 150 && rc.getRobotCount() < 10)) {
                        rc.resign();
                    }
                    Util.loop();
                    bot.turn();
                    Util.postLoop();
                    if (rc.getRoundNum() != currentTurn) {
                        overBytecodes = true;
                        // We ran out of bytecodes! - MAGENTA
                        Util.setIndicatorDot(rc.getLocation(), 255, 0, 255);
                        int over = Clock.getBytecodeNum() + (rc.getRoundNum() - currentTurn - 1) * rc.getType().bytecodeLimit;
                        Util.println(rc.getLocation() + " out of bytecodes: " + Cache.TURN_COUNT + " (over by " + over + ")");
                    }
                    if (errored) {
                        Util.setIndicatorDot(rc.getLocation(), 255, 0, 0); // red
                    }
                    if (overBytecodes) {
                        Util.setIndicatorDot(rc.getLocation(), 128, 0, 255); // purple
                    }
                    Clock.yield();
                }
            } catch (Exception ex) {
                Util.println(rc.getLocation() + " errored: " + Cache.TURN_COUNT);
                ex.printStackTrace();
                errored = true;
                Clock.yield();
            }
        }
    }
}
