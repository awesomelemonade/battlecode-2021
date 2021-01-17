package latticebot;

import battlecode.common.*;
import latticebot.util.Cache;
import latticebot.util.Constants;
import latticebot.util.LatticeUtil;
import latticebot.util.MapInfo;
import latticebot.util.Pathfinder;
import latticebot.util.UnitCommunication;
import latticebot.util.Util;


public strictfp class Politician implements RunnableBot {
    private static RobotController rc;
    private static int power;
    private static final int[] DEFENSE_SQUARES_X = {2, 0, -2, 0, 2, 1, -1, -2, -2, -1, 1, 2, 2, -2, -2, 2, 3, 0, -3,
            0};
    private static final int[] DEFENSE_SQUARES_Y = {0, 2, 0, -2, 1, 2, 2, 1, -1, -2, -2, -1, 2, 2, -2, -2, 0, 3, 0,
            -3};
    private static boolean defender;
    private static MapLocation nearestAllyEC;
    private static MapLocation nearestNeutralEC;
    private static MapLocation nearestEnemyEC;
    private static MapLocation nearestS;
    private static boolean selfempowerer;
    private static boolean attacking = false;

    private static int currentConviction;
    private static int currentConviction_10; // = currentConviction - 10
    private static double currentEmpowerFactor;

    public Politician(RobotController rc) {
        Politician.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        if (Math.random() < 0.4) {
            defender = true;
        } else {
            defender = false;
        }
        if (rc.getInfluence() % 10 == 6) {
            selfempowerer = true;
            System.out.println("SELF EMPOWERER! " + rc.getLocation().x + " " + rc.getLocation().y);
            System.out.println(rc.getEmpowerFactor(Constants.ALLY_TEAM, 10));
        } else {
            selfempowerer = false;
        }
    }

    private void preTurn() throws GameActionException {
        currentConviction = rc.getConviction();
        currentConviction_10 = currentConviction - 10;
        currentEmpowerFactor = rc.getEmpowerFactor(Constants.ALLY_TEAM, 0);
        power = (int) (currentConviction_10 * currentEmpowerFactor);

        nearestAllyEC = MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getClosestLocation().orElse(null);
        nearestNeutralEC = MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).getClosestLocation().orElse(null);
        nearestEnemyEC = MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getClosestLocation().orElse(null);
        int minDist = Integer.MAX_VALUE;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0; ) {
            RobotInfo robot = Cache.ALLY_ROBOTS[i];
            if (UnitCommunication.isPotentialSlanderer(robot)) {
                int dist = robot.location.distanceSquaredTo(Cache.MY_LOCATION);
                if (dist < minDist) {
                    minDist = dist;
                    nearestS = robot.location;
                }
            }
        }
        if (rc.getRoundNum() % 25 == 0) attacking = true;
    }

    @Override
    public void turn() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        preTurn();
        if (currentConviction_10 <= 0) { // useless; best thing to do is try to block an enemy ec
            if (campEnemyEC()) {
                return;
            }
            Util.smartExplore();
            return;
        }
        if (power >= 50 && tryClaimEC()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 0, 255); // blue
            return;
        }
        if (currentConviction >= 50 && tryHealEC()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 102, 51, 0); // brown
            return;
        }
        if (tryEmpower()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 255); // cyan
            return;
        }
        if (chaseWorthwhileEnemy()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 0); // green
            return;
        }
        if (selfempowerer && currentConviction >= 50 && trySelfBuff()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 102, 102, 153); // bluish purple
            return;
        }
        if (power >= 50 && goToNearestEC()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 255, 255, 0); // yellow
            return;
        }
        if (((defender && currentConviction < 50/* && rc.getRoundNum() >= 100*/) || !shouldAttack()) && tryDefend()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 255, 0, 255); // pink
            return;
        }
        if (Util.smartExplore()) {
            return;
        }
    }

    public boolean shouldAttack() {
        if (rc.getRoundNum() <= 300) return true;
        return attacking;
    }

    public boolean tryEmpowerAtEC(MapLocation loc, Team team) throws GameActionException {
        if (rc.canSenseLocation(loc)) {
            RobotInfo ec = rc.senseRobotAtLocation(loc);
            if (ec == null) {
                Util.println("WARNING: No EC detected at " + loc);
                return false;
            }
            int ecConviction = ec.getConviction();
            // Check if empowering will take the ec
            int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(loc);
            if (team == Team.NEUTRAL) {
                if (distanceSquared <= 9) {
                    // empower range - see if we can take it ourselves
                    if (rc.getConviction() - 10 > ecConviction && rc.senseNearbyRobots(distanceSquared).length == 1) {
                        rc.empower(distanceSquared);
                    }
                }
                // pathfind to nearest cardinal-adjacent square
                if (Cache.MY_LOCATION.isWithinDistanceSquared(loc, 1)) {
                    int neighborsOnTheMap = 0;
                    for (Direction direction : Constants.CARDINAL_DIRECTIONS) {
                        if (rc.onTheMap(loc.add(direction))) {
                            neighborsOnTheMap++;
                        }
                    }
                    // add up all conviction we have
                    int sumConviction = 0;
                    RobotInfo[] allyRobots = rc.senseNearbyRobots(loc, 1, Constants.ALLY_TEAM);
                    for (RobotInfo robot : allyRobots) {
                        if (robot.getType() == RobotType.POLITICIAN) {
                            sumConviction += robot.getConviction() - 10;
                        }
                    }
                    // TODO: Perhaps we should check for nearby robots so we don't split damage?
                    if ((allyRobots.length + 1) == neighborsOnTheMap || sumConviction > ecConviction) {
                        rc.empower(1);
                    }
                    return true;
                }
            } else {
                if (distanceSquared <= 1 || distanceSquared <= 9 && rc.senseNearbyRobots(distanceSquared).length == 1) {
                    rc.empower(distanceSquared);
                }
            }
        }
        Pathfinder.execute(loc);
        return true;
    }

    public boolean shouldHeal(RobotInfo ec) throws GameActionException {
        if (Constants.SPAWN.isWithinDistanceSquared(ec.getLocation(), 16)) {
            return false;
        }
        // heal if ec is low and surrounded by enemy ps
        int enemyConviction = 0;
        RobotInfo[] enemies = rc.senseNearbyRobots(ec.location, 64, Constants.ENEMY_TEAM);
        for (RobotInfo robot : enemies) {
            if (robot.getType() == RobotType.POLITICIAN) {
                enemyConviction += robot.getConviction() - 10;
            }
        }
        if (2 * enemyConviction > ec.getConviction()) {
            return true;
        }
        return false;
    }

    public boolean trySelfBuff() throws GameActionException {
        if (currentEmpowerFactor < 1.5) return false;
        int bestDist = Integer.MAX_VALUE;
        MapLocation bestLoc = null;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0; ) {
            RobotInfo robot = Cache.ALLY_ROBOTS[i];
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation loc = robot.location;
                int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        if (bestLoc == null) return false;
        return tryEmpowerAtEC(bestLoc, Constants.ALLY_TEAM);
    }

    public boolean tryHealEC() throws GameActionException {
        int bestDist = Integer.MAX_VALUE;
        MapLocation bestLoc = null;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0; ) {
            RobotInfo robot = Cache.ALLY_ROBOTS[i];
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && shouldHeal(robot)) {
                MapLocation loc = robot.location;
                int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        if (bestLoc == null) return false;
        return tryEmpowerAtEC(bestLoc, Constants.ALLY_TEAM);
    }

    // if sees empty square next to ec, go to it
    public boolean campEnemyEC() throws GameActionException {
        if (nearestEnemyEC == null) return false;
        if (!Cache.MY_LOCATION.isWithinDistanceSquared(nearestEnemyEC, 25)) {
            return Util.tryMove(nearestEnemyEC);
        }
        if (Cache.MY_LOCATION.isWithinDistanceSquared(nearestEnemyEC, 2)) {
            return true;
        }
        // TODO: Loop unrolling?
        for (Direction d : Constants.ORDINAL_DIRECTIONS) {
            MapLocation loc = nearestEnemyEC.add(d);
            if (rc.canSenseLocation(loc) && !rc.isLocationOccupied(loc)) {
                Util.tryMove(loc);
                return true;
            }
        }
        return false;
    }

    // Lower defense score better
    public int getDefenseScore(MapLocation loc) {
        if (!LatticeUtil.isLatticeLocation(loc)) {
            return Integer.MAX_VALUE;
        }
        if (nearestAllyEC == null) {
            return Integer.MAX_VALUE;
        }
        int dist = nearestAllyEC.distanceSquaredTo(loc);
        if (dist <= 16)
            return Integer.MAX_VALUE;
        // TODO: Unroll loop?
        for (Direction d : Constants.ORDINAL_DIRECTIONS) {
            MapLocation adj = loc.add(d);
            if (rc.canSenseLocation(adj)) {
                try {
                    RobotInfo robot = rc.senseRobotAtLocation(adj);
                    if (robot != null && UnitCommunication.isPotentialSlanderer(robot)) {
                        return Integer.MAX_VALUE;
                    }
                } catch (GameActionException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        return nearestEnemyEC == null ? 5000 * dist : 5000 * dist + nearestEnemyEC.distanceSquaredTo(loc);
    }

    public boolean tryDefend() throws GameActionException {
        // find best defense score
        int bestDefenseScore = Integer.MAX_VALUE;
        Direction bestDir = null;
        // TODO: Potentially unroll loop
        for (Direction d : Constants.ORDINAL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(d);
            if (rc.canMove(d)) {
                int score = getDefenseScore(loc);
                if (score < bestDefenseScore) {
                    bestDefenseScore = score;
                    bestDir = d;
                }
            }
        }
        if (bestDir == null) {
            return false;
        }
        if (getDefenseScore(Cache.MY_LOCATION) <= bestDefenseScore) {
            return true;
        }
        return Util.tryMove(bestDir);
    }

    public boolean goToNearestEC() {
        if (nearestNeutralEC != null) {
            Pathfinder.execute(nearestNeutralEC);
            return true;
        }
        if (!shouldAttack()) return false;
        if (nearestEnemyEC != null) {
            Pathfinder.execute(nearestEnemyEC);
            return true;
        }
        return false;
    }

    private int getScore(int radiusSquared) throws GameActionException {
        int numUnits = rc.senseNearbyRobots(radiusSquared).length;
        if (numUnits == 0) {
            return 0;
        }
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(radiusSquared, Constants.ENEMY_TEAM);
        if (enemyRobots.length + Cache.NEUTRAL_ROBOTS.length == 0) {
            return 0;
        }
        int damage = power / numUnits;
        int mKills = 0;
        int pKills = 0;
        int ecKills = 0;
        int mecConviction = 0;
        int pConviction = 0;
        int distMtoS = 9999;
        int distPtoEC = 9999;
        for (int i = enemyRobots.length; --i >= 0; ) {
            RobotInfo enemy = enemyRobots[i];
            if (enemy.getType() == RobotType.POLITICIAN) {
                pConviction += Math.min(damage,
                        Math.max(0, enemy.getConviction() - 10) + Math.max(0, enemy.getInfluence() - 10));
                if (damage > enemy.getConviction()) {
                    pKills++;
                }
                if (nearestAllyEC != null)
                    distPtoEC = Math.min(distPtoEC, enemy.location.distanceSquaredTo(nearestAllyEC));
            } else if (enemy.getType() == RobotType.MUCKRAKER) {
                mecConviction += Math.min(damage, enemy.getConviction());
                if (damage > enemy.getConviction()) {
                    mKills++;
                }
                if (nearestS != null) distMtoS = Math.min(distMtoS, enemy.location.distanceSquaredTo(nearestS));
            } else if (enemy.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                mecConviction += Math.min(damage, enemy.getConviction());
                if (damage > enemy.getConviction()) {
                    ecKills++;
                }
            }
        }
        for (RobotInfo neutralEC : Cache.NEUTRAL_ROBOTS) {
            if (Cache.MY_LOCATION.isWithinDistanceSquared(neutralEC.getLocation(), radiusSquared)) {
                mecConviction += Math.min(damage, neutralEC.getConviction());
                if (damage > neutralEC.getConviction()) {
                    ecKills++;
                }
            }
        }
        int numKills = ecKills + mKills + pKills;
        int score = ecKills * 100000 * 100000 + numKills * 100000 + mecConviction;
        if (shouldEmpower(ecKills, mKills, pKills, mecConviction, pConviction, distPtoEC, distMtoS)) {
            return score;
        } else {
            return 0;
        }
    }

    public boolean tryClaimEC() throws GameActionException {
        // if we see enemy/neutral ec, try to move closer to it
        // if can't move any closer, explode
        MapLocation bestLoc = null;
        int bestDist = Integer.MAX_VALUE;
        Team bestTeam = null;

        for (int i = Cache.ENEMY_ROBOTS.length; --i >= 0; ) {
            RobotInfo robot = Cache.ENEMY_ROBOTS[i];
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation location = robot.getLocation();
                int distance = location.distanceSquaredTo(Cache.MY_LOCATION);
                if (distance < bestDist) {
                    bestDist = distance;
                    bestLoc = location;
                    bestTeam = Constants.ENEMY_TEAM;
                }
            }
        }
        for (int i = Cache.NEUTRAL_ROBOTS.length; --i >= 0; ) {
            RobotInfo robot = Cache.NEUTRAL_ROBOTS[i];
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation location = robot.getLocation();
                int distance = location.distanceSquaredTo(Cache.MY_LOCATION);
                if (distance < bestDist) {
                    bestDist = distance;
                    bestLoc = location;
                    bestTeam = Team.NEUTRAL;
                }
            }
        }
        if (bestLoc == null)
            return false;
        return tryEmpowerAtEC(bestLoc, bestTeam);
    }

    public boolean shouldEmpower(int ecKills, int mKills, int pKills, int mecConviction, int pConviction, int distPtoEC, int distMtoS) throws GameActionException {
        /*Util.println("ecKills = " + ecKills);
        Util.println("mKills = " + mKills);
        Util.println("pKills = " + pKills);
        Util.println("distPtoEC = " + distPtoEC);
        Util.println("distMtoS = " + distMtoS);*/
        if (mecConviction + pConviction > currentConviction) return true;
        if (ecKills + mKills + pKills >= 8) return true;
        if (ecKills >= 1) {
            return true;
        }
        int numKills = mKills + pKills;
        if (currentConviction < 50) {
            if (numKills == 0)
                return false;
            if (distMtoS <= 100 && mKills >= 1)
                return true;
            if (distPtoEC <= 8 && pKills >= 1)
                return true;
            if (mKills >= 2) {
                return true;
            }
            if (nearestAllyEC != null && nearestAllyEC.isWithinDistanceSquared(Cache.MY_LOCATION, 100) && mKills >= 1)
                return true;
            return false;
        } else {
            if (numKills * 15 >= currentConviction_10) return true;
            int distFromEC = nearestAllyEC == null ? 1024 : nearestAllyEC.distanceSquaredTo(Cache.MY_LOCATION);
            double euclidDist = Math.sqrt(distFromEC);
            double requiredRatio = 1.0 / (0.03 * (euclidDist + 5.0)) + 1;
            if (mecConviction * requiredRatio >= currentConviction_10) {
                return true;
            }
            if (distPtoEC <= 2 && pConviction * 8 >= currentConviction_10) {
                return true;
            }
            return false;
        }
    }

    public boolean tryEmpower() throws GameActionException {
        if (power <= 0) {
            return false;
        }
        if (Cache.ENEMY_ROBOTS.length + Cache.NEUTRAL_ROBOTS.length == 0) {
            return false;
        }
        // if can kill something, maximize the number
        int bestRadius = -1;
        int bestScore = 0;
        for (int r = 1; r <= Constants.POLITICIAN_ACTION_RADIUS_SQUARED; r++) {
            int score = getScore(r);
            if (score > bestScore) {
                bestScore = score;
                bestRadius = r;
            }
        }
        if (bestRadius == -1) {
            return false;
        }
        rc.empower(bestRadius);
        return true;
    }

    private boolean shouldChase(RobotInfo robot) {
        if (robot.getType() == RobotType.POLITICIAN)
            return false;
        int robotConviction = robot.getConviction();
        if (currentConviction * 2 + 20 >= robotConviction
                && robotConviction * 2 + 20 >= currentConviction)
            return true;
        MapLocation robotLocation = robot.getLocation();
        if (robot.getType() == RobotType.MUCKRAKER
                && ((nearestS != null && robotLocation.isWithinDistanceSquared(nearestS, 81))
                || (nearestAllyEC != null && robotLocation.isWithinDistanceSquared(nearestAllyEC, 81)))
                && robotConviction * 10 + 30 >= currentConviction)
            return true;
        return false;
    }

    private boolean chaseWorthwhileEnemy() throws GameActionException {
        int totx = 0;
        int toty = 0;
        int tot = 0;
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (shouldChase(robot)) {
                int numNearbyPoliticians = 0;
                MapLocation loc = robot.location;
                RobotInfo[] nearbyAllies = rc.senseNearbyRobots(loc, 5, Constants.ALLY_TEAM);
                for (RobotInfo robot2 : nearbyAllies) {
                    if (robot2.getType() == RobotType.POLITICIAN)
                        numNearbyPoliticians++;
                }
                if (numNearbyPoliticians >= 2)
                    continue;
                totx += loc.x;
                toty += loc.y;
                tot++;
            }
        }
        if (tot == 0) {
            return false;
        }
        MapLocation desired = new MapLocation(totx / tot, toty / tot);
        return Util.tryMove(desired);
    }
}
