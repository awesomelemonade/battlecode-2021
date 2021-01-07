package latticebot.util;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Communication {
    private static RobotController rc;

    public static void init(RobotController rc) {
        // Find enlightenment center that spawned this slanderer
        Communication.rc = rc;
    }

    // for ECs only, track all produced unit IDs
    // use linked list since we'll always iterate over the entire thing anyways lol
    // maybe this is a bad idea idk im dumb
    static class unit_node {
        int robot_id;
        unit_node next;
    }
    private static unit_node known_units = null;

    // update list whenever we produce a new unit
    // todo: also add if we see a new id? but that seems hard to do efficiently since java sets are expensive
    public static void update_known_units(int id) {
        unit_node tmp = new unit_node();
        tmp.robot_id = id;
        tmp.next = known_units;
        known_units = tmp;
    }

    public static void process_comms() throws GameActionException {
        unit_node prev = null;
        unit_node current = known_units;
        while (current != null) {
            // first, see if can sense flag
            if (!rc.canGetFlag(current.robot_id)) {
                // cannot sense flag, unit mustve died
                // remove from LL
                if (prev == null) {
                    // edge case, havent iterated yet
                    known_units = null;
                } else {
                    // normal case, delete normally
                    prev.next = current.next;
                }
            } else {
                // can sense flag, get flag
                int flag = rc.getFlag(current.robot_id);
                process_single_cmd(flag);
            }
            prev = current;
            current = current.next;
        }
    }

    public static void process_single_cmd(int flag) {
        // TODO: implement this
        if (flag != 0) {
            System.out.println("found flag " + flag);
        }
    }
}
