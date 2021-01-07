package latticebot.util;

import battlecode.common.*;

public class Communication {
    private static RobotController rc;
    private static boolean flag_set = false; // to avoid setting flag multiple times each turn

    public static void init(RobotController rc) throws GameActionException {
        Communication.rc = rc;
    }

    public static void loop() throws GameActionException {
        processComms();
        if (!flag_set) {
            // process_comms didnt set flag, see if we can comm anything else useful
            commAnyData();
        }
    }

    // for ECs only, track all produced unit IDs
    // use linked list since we'll always iterate over the entire thing anyways lol
    // maybe this is a bad idea idk im dumb
    static class unitNode {
        int robot_id;
        unitNode next;
    }
    private static unitNode known_units = null;

    // update list whenever we produce a new unit
    // todo: also add if we see a new id? but that seems hard to do efficiently since java sets are expensive
    public static void updateKnownUnits(int id) {
        unitNode tmp = new unitNode();
        tmp.robot_id = id;
        tmp.next = known_units;
        known_units = tmp;
    }

    // this uh, gets really expensive when we have a lot of units... how 2 fix?
    public static void processComms() throws GameActionException {
        // todo: also iterate over nearby units?
        flag_set = false;
        unitNode prev = null;
        unitNode current = known_units;
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
                processSingleCmd(flag);
            }
            prev = current;
            current = current.next;
        }
    }

    public static void processSingleCmd(int flag) throws GameActionException {
        // TODO: implement this
        if (flag != 0) {
            //System.out.println("found flag " + flag);
            int cmd = flag & 31;
            switch (cmd) {
                case 0:
                    System.out.println("Error, found 0 cmd but not all 0 flag");
                    break;
                case 1:
                    int enc_X = (flag >> 5) & 127;
                    int enc_Y = (flag >> 12) & 127;
                    MapLocation enemyEC = new MapLocation(Constants.SPAWNEC.x + unpackSigned64(enc_X),
                            Constants.SPAWNEC.y + unpackSigned64(enc_Y));
                    //System.out.println("Found enemy EC at " + enemyEC);
                    // todo: rebroadcast to everyone else
                    break;
                default:
                    System.out.println("Error, unrecognized command " + cmd);
                    break;
            }
        }
    }

    // basically, we want to comm any significant info if we haven't commed anything already
    // todo: check bytecodes left before this?
    public static void commAnyData() throws GameActionException {
        // first, check for any enemy ECs
        // todo: check for dups so we dont keep broadcasting the same one
        for (RobotInfo enemy : Cache.ENEMY_ROBOTS) {
            if (enemy.type == RobotType.ENLIGHTENMENT_CENTER) {
                // broadcast enemy EC
                //System.out.println("Found enemy EC at " + enemy.location);
                commEnemyEC(enemy.location);
                return;
            }
        }

        // todo: add more stuff

        // nothing, set flag to 0
        setFlag(0);
    }

    public static void setFlag(int val) throws GameActionException {
        rc.setFlag(val);
        flag_set = true;
    }

    public static void commEnemyEC(MapLocation loc) throws GameActionException {
        // cmd #1
        int flag = 1;
        flag |= (packSigned64(loc.x - Constants.SPAWNEC.x) << 5);
        flag |= (packSigned64(loc.y - Constants.SPAWNEC.y) << 12);
        setFlag(flag);
    }

    public static int packSigned64(int value) { // 7 bit
        if (value < 0) {
            return 64 - value;
        }
        return value;
    }

    public static int unpackSigned64(int value) { // 7 bit
        if (value >= 64) {
            return 64 - value;
        }
        return value;
    }
}
