package ppbot;
import battlecode.common.*;
import static ppbot.Constants.*;

public strictfp class RobotPlayer {
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotType robot_type = rc.getType();
        switch(robot_type) {
            case ENLIGHTENMENT_CENTER:
                EnlightenmentCenter enlightenment_center = new EnlightenmentCenter(rc);
                while (true) {
                    try {
                        System.out.println("I'm a " + robot_type + "! Location " + rc.getLocation());
                        enlightenment_center.run();
                        Clock.yield();
                    } catch (Exception e) {
                        System.out.println(rc.getType() + " Exception");
                        e.printStackTrace();
                    }
                }
            case POLITICIAN:
                Politician politician = new Politician(rc);
                while (true) {
                    try {
                        System.out.println("I'm a " + robot_type + "! Location " + rc.getLocation());
                        politician.run();
                        Clock.yield();
        
                    } catch (Exception e) {
                        System.out.println(rc.getType() + " Exception");
                        e.printStackTrace();
                    }
                }
            case SLANDERER:
                Slanderer slanderer = new Slanderer(rc);
                while(true) {
                    try {
                        System.out.println("I'm a " + robot_type + "! Location " + rc.getLocation());
                        slanderer.run();
                        Clock.yield();
    
                    } catch (Exception e) {
                        System.out.println(rc.getType() + " Exception");
                        e.printStackTrace();
                    }
                }
            case MUCKRAKER:
                Muckracker muckracker = new Muckracker(rc);
                while(true) {
                    try {
                        System.out.println("I'm a " + robot_type + "! Location " + rc.getLocation());
                        muckracker.run();
                        Clock.yield();
    
                    } catch (Exception e) {
                        System.out.println(rc.getType() + " Exception");
                        e.printStackTrace();
                    }
                }
        }
    }
}
