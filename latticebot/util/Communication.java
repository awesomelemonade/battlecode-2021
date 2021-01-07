package latticebot.util;

import battlecode.common.*;

public class Communication {
    private static RobotController rc;
    private static boolean flag_set = false; // to avoid setting flag multiple times each turn
    private static int cycle_ctr = 0;
    private static int enemyEC_ctr = 0; // which enemyEC to comm
    private static int edge_ctr = 0; // which edge to comm
    private static final int MAX_CYCLE = 2; // update whenever we add a new class

    public static void init(RobotController rc) throws GameActionException {
        Communication.rc = rc;
    }

    public static void loop() throws GameActionException {
        processComms();
        if (!flag_set) {
            if (rc.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                // see if we can comm anything else useful
                commAnyData();
            } else {
                // cycle through important info
                commCycle();
            }
        }
    }

    public static void commCycle() throws GameActionException {
        switch (cycle_ctr) {
            case 0: // broadcast enemy ec
                // inc until next nonempty entry
                int orig_i = enemyEC_ctr;
                do {
                    enemyEC_ctr = (enemyEC_ctr + 1) % Cache.enemyECs.length;
                } while (Cache.enemyECs[enemyEC_ctr] == null && enemyEC_ctr != orig_i);
                if (Cache.enemyECs[enemyEC_ctr] != null) {
                    commEnemyEC(Cache.enemyECs[enemyEC_ctr]);
                }
                break;
            case 1: // broadcast edge
                orig_i = edge_ctr;
                int val;
                do {
                    edge_ctr = (edge_ctr + 1) % 4;
                    val = Util.indexToEdge(edge_ctr);
                } while (val == -1 && edge_ctr != orig_i);

                if (val != -1) {
                    commEdge(edge_ctr, val);
                }
                break;
            default:
                System.out.println("Error, invalid cycle counter");
                break;
        }
        cycle_ctr = (cycle_ctr + 1) % MAX_CYCLE;
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
                case 1: // enemy EC
                    int enc_X = (flag >> 5) & 127;
                    int enc_Y = (flag >> 12) & 127;
                    MapLocation enemyEC = new MapLocation(Constants.SPAWNEC.x + unpackSigned64(enc_X),
                            Constants.SPAWNEC.y + unpackSigned64(enc_Y));
                    Util.addToEnemyECs(enemyEC);
                    break;
                case 2: // edge
                    int which = flag >> 20;
                    int val = (flag >> 5) & 32767;
                    Util.setIndexEdge(which, val);
                    break;
                default:
                    System.out.println("Error, unrecognized command " + cmd);
                    break;
            }
        }
    }

    // basically, we want to comm any significant info if we haven't commed anything already
    // todo: check bytecodes left before this?
    // todo: split into multiple funcs?
    public static void commAnyData() throws GameActionException {
        // first, check for any enemy ECs
        // todo: check for dups so we dont keep broadcasting the same one
        for (RobotInfo enemy : Cache.ENEMY_ROBOTS) {
            if (enemy.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (!Util.inEnemyECs(enemy.location)) {
                    // broadcast enemy EC
                    //System.out.println("Found enemy EC at " + enemy.location);
                    commEnemyEC(enemy.location);
                }
                return;
            }
        }

        // comm edges
        for (int i = 3; i >= 0; i--) {
            if (!Util.indexToEdgeBool(i) && Util.indexToEdge(i) != -1) {
                int val = Util.indexToEdge(i);
                commEdge(i, val);
                Util.setIndexEdge(i, val);
                return;
            }
        }

        // todo: add more stuff

        // nothing, set flag to 0
        if (rc.getFlag(rc.getID()) != 0) {
            setFlag(0);
        }
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

    public static void commEdge(int which, int val) throws GameActionException {
        // cmd #2, map edge
        int flag = 2;
        flag |= val << 5;
        flag |= which << 20;
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
