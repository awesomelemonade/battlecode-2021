package ppbot;

import battlecode.common.*;
import ppbot.util.Cache;
import ppbot.util.Util;
import static ppbot.util.Constants.*;

public strictfp class EnlightenmentCenter implements RunnableBot {
    private RobotController rc;
    static MapLocation cur_loc;
    static int prev_flag = 0;
    static int flag = 0;

    static int muckrakerCount = 0;
    static int[] muckrakerIDs = new int[4];

    static MapLocation enemyEC = null;

    public EnlightenmentCenter(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        cur_loc = rc.getLocation();
        prev_flag = flag;
        flag = 0;

        receiveMuckrakerComms();

        if (Cache.TURN_COUNT == 1) {
            System.out.println("Try produce Slanderer");
            produceUnitAnywhere(RobotType.SLANDERER, 140);
        }

        if (muckrakerCount < 4) {
            System.out.println("Try produce Muckraker");
            int r_id;
            if ((r_id = produceUnitAnywhere(RobotType.MUCKRAKER, 1)) != -1) {
                // 0, 2, 4, 6
                setFlagDirectionCommand(muckrakerCount * 2);
                muckrakerIDs[muckrakerCount] = r_id;
                muckrakerCount++;
            }
        }

        if (enemyEC != null && muckrakerCount >= 4) {
            // Build attack politicians
            if (rc.getInfluence() >= 50) {
                int r_id;
                if ((r_id = produceUnitAnywhere(RobotType.POLITICIAN, 50)) != -1) {
                    setFlagEnemyEC();
                }
            }
        }

        rc.setFlag(prev_flag);
    }

    private void receiveMuckrakerComms() throws GameActionException {
        for (int i = 0; i < muckrakerCount; i++) {
            if (rc.canGetFlag(muckrakerIDs[i])) {
                int temp_flag = rc.getFlag(muckrakerIDs[i]);
                if ((temp_flag & 1) == 1 && enemyEC == null) {
                    enemyEC = new MapLocation(cur_loc.x + Util.unpackSigned128(temp_flag >> 1),
                            cur_loc.y + Util.unpackSigned128(temp_flag >> 9));
                    System.out.println("Received Enemy Loc: " + enemyEC.toString());
                }
            }
        }
    }

    private void setFlagEnemyEC() {
        flag = 1 | (Util.packSigned128(enemyEC.x - cur_loc.x) << 1) | (Util.packSigned128(enemyEC.y - cur_loc.y) << 9);
    }

    private void setFlagDirectionCommand(int direction) {
        // lsb 3 bits
        flag = flag - (flag & 7) + direction;
    }

    private int produceUnitAnywhere(RobotType t, int influence) throws GameActionException {
        for (int i = 0; i < ORDINAL_DIRECTIONS.length; i++) {
            MapLocation next_loc = cur_loc.add(ORDINAL_DIRECTIONS[i]);
            if (rc.canBuildRobot(t, ORDINAL_DIRECTIONS[i], influence)) {
                rc.buildRobot(t, ORDINAL_DIRECTIONS[i], influence);
                System.out.println("DO BUILD");
                System.out.println("ID is: " + rc.senseRobotAtLocation(next_loc).getID());
                return rc.senseRobotAtLocation(next_loc).getID();
            }
        }
        return -1;
    }
}
