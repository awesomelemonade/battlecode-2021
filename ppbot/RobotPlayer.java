package ppbot;
import battlecode.common.*;

public strictfp class RobotPlayer {
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotType robot_type = rc.getType();
        RunnableBot bot = null;
        switch(robot_type) {
            case ENLIGHTENMENT_CENTER:
                bot = new EnlightenmentCenter(rc);
                break;
            case POLITICIAN:
                bot = new Politician(rc);
            case SLANDERER:
                bot = new Slanderer(rc);
            case MUCKRAKER:
                bot = new Muckracker(rc);
        }
        try {
            bot.init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        while (true) {
            try {
                bot.turn();
                Clock.yield();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
