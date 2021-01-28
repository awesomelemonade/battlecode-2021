package combobot3_8a5c445;

import battlecode.common.GameActionException;

public interface RunnableBot {
    public void init() throws GameActionException;
    public void turn() throws GameActionException;
}
