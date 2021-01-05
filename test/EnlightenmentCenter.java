package test;
import battlecode.common.*;

import static test.RobotPlayer.rc;
import static test.Util.directions;
import static test.RobotPlayer.turnCount;
import static test.Util.packSigned128;
import static test.Util.unpackSigned128;

public class EnlightenmentCenter {

	static MapLocation cur_loc;
	static int prev_flag = 0;
	static int flag = 0;

	static int muckrakerCount = 0;
	static int[] muckrakerIDs = new int[4];

	static MapLocation enemyEC = null;

	public static void run() throws GameActionException {
		cur_loc = rc.getLocation();
		prev_flag = flag;
		flag = 0;

		receiveMuckrakerComms();

		if (turnCount == 1) {
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

	static void receiveMuckrakerComms() throws GameActionException {
		for (int i = 0; i < muckrakerCount; i++) {
			if (rc.canGetFlag(muckrakerIDs[i])) {
				int temp_flag = rc.getFlag(muckrakerIDs[i]);
				if ((temp_flag & 1) == 1 && enemyEC == null) {
					enemyEC = new MapLocation(cur_loc.x + unpackSigned128(temp_flag >> 1), cur_loc.y + unpackSigned128(temp_flag >> 9));
					System.out.println("Received Enemy Loc: " + enemyEC.toString());
				}
			}
		}
	}

	static void setFlagEnemyEC() {
		flag = 1 | (packSigned128(enemyEC.x - cur_loc.x) << 1) | (packSigned128(enemyEC.y - cur_loc.y) << 9);
	}

	static void setFlagDirectionCommand(int direction) {
		// lsb 3 bits
		flag = flag - (flag & 7) + direction;
	}

	static int produceUnitAnywhere(RobotType t, int influence) throws GameActionException {
		for (int i = 0; i < directions.length; i++) {
			MapLocation next_loc = cur_loc.add(directions[i]);
			if (rc.canBuildRobot(t, directions[i], influence)) {
				rc.buildRobot(t, directions[i], influence);
				System.out.println("DO BUILD");
				System.out.println("ID is: " + rc.senseRobotAtLocation(next_loc).getID());
				return rc.senseRobotAtLocation(next_loc).getID();
			}
		}
		return -1;
	}
}
