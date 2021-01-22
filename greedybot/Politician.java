package greedybot;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import greedybot.util.Cache;
import greedybot.util.Constants;
import greedybot.util.LambdaUtil;
import greedybot.util.MapInfo;
import greedybot.util.Pathfinder;
import greedybot.util.UnitCommunication;
import greedybot.util.Util;

import java.util.Comparator;

public strictfp class Politician implements RunnableBot {
    private static RobotController rc;

    private static boolean earlyGame = true;
    private static boolean hasFortified = false; // whether the politician has fortified before
    private static boolean doNotFortify = false; // whether the politician should be forced to not fortify
    private static boolean isSmallPolitician;
    private static int currentConviction;
    private static int currentDamage;
    private static boolean attacking = false; // used for waves

    public Politician(RobotController rc) {
        Politician.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        earlyGame = rc.getRoundNum() < 100;
        currentConviction = rc.getConviction();
        currentDamage = Math.max(0, currentConviction - 10);
        isSmallPolitician = currentDamage <= 10;
        attacking = attacking || rc.getRoundNum() % 25 == 0;
        if (!rc.isReady()) {
            return;
        }
        if (currentDamage <= 0) {
            if (campEnemyEC()) {
                return;
            }
            Util.smartExplore();
            return;
        }
        if (tryEmpowerEnemy()) {
            return;
        }
        if (currentDamage >= 50) {
            if (tryClaimEC()) {
                Util.setIndicatorDot(Cache.MY_LOCATION, 0, 0, 255); // blue
                return;
            }
            if (goToEC()) {
                Util.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 255); // cyan
                return;
            }
        }
        if (Cache.ALLY_ROBOTS.length > 12) {
            // TODO: go towards enemy (or at least somewhere open)
            Util.smartExplore();
            return;
        }

        // Some initial analysis

        if (isSmallPolitician) {
            // this is a small politician
            if (!doNotFortify) {
                if (tryFortifyNearSlanderers()) {
                    return;
                }
            }
        }

        // Is this a big politician? Small politician?
        // Are there slanderers to protect?
        // Are there a lot of small muckrakers? (what is the maximum number of muckrakers this politician can kill?)
        // do we do a self buff? do we empower neutral ec? do we empower enemy ec?
        // do we go towards neutral ec? do we go towards enemy ec?


        // small politicians: objective is to defend against multiple small muckrakers
        //                      kill small muckrakers/politicians/slanderers hanging around neutral/enemy ec
        //                          -> removes the split of damage from our large politicians
        //                      move away from large muckraker if large politician nearby
        // large politicians: defend against large muckrakers, go for neutral ec, go for enemy ec
        //                      move away from small muckrakers if small politician nearby (if defending)

        if (Util.smartExplore()) {
            return;
        }
    }

    public static boolean campEnemyEC() {
        return MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getClosestLocation().map(loc -> {
            if (!Cache.MY_LOCATION.isAdjacentTo(loc)) {
                Pathfinder.execute(loc);
            }
            return true;
        }).orElse(false);
    }

    public static boolean tryClaimEC() throws GameActionException {
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

    public static boolean tryEmpowerAtEC(MapLocation loc, Team team) throws GameActionException {
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
                if (distanceSquared <= 16) {
                    int numNeighborsOpen = 0;
                    MapLocation closestCardinalAdjacentSquare = null;
                    int closestDistanceSquared = Integer.MAX_VALUE;
                    for (Direction direction : Constants.CARDINAL_DIRECTIONS) {
                        MapLocation neighbor = loc.add(direction);
                        if (rc.onTheMap(neighbor)) {
                            RobotInfo neighborRobot = rc.senseRobotAtLocation(neighbor);
                            if (neighborRobot == null) {
                                numNeighborsOpen++;
                                int dist = Cache.MY_LOCATION.distanceSquaredTo(neighbor);
                                if (dist < closestDistanceSquared) {
                                    closestCardinalAdjacentSquare = neighbor;
                                    closestDistanceSquared = dist;
                                }
                            } else {
                                if (neighborRobot.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                                    numNeighborsOpen++;
                                }
                            }
                        }
                    }
                    if (distanceSquared <= 1 || distanceSquared <= 9 && rc.senseNearbyRobots(distanceSquared).length == 1) {
                        // Add up our influence & enemy influence
                        RobotInfo[] allNearbyRobots = rc.senseNearbyRobots(loc, 16, null);
                        int convictionBalance = currentDamage; // current robot's conviction
                        boolean hasEnemy = false;
                        for (int i = allNearbyRobots.length; --i >= 0;) {
                            RobotInfo robot = allNearbyRobots[i];
                            if (robot.getType() == RobotType.POLITICIAN) {
                                if (robot.getTeam() == Constants.ALLY_TEAM) {
                                    convictionBalance += Math.max(0, robot.getConviction() - 10);
                                }
                                if (robot.getTeam() == Constants.ENEMY_TEAM) {
                                    hasEnemy = true;
                                    convictionBalance -= Math.max(0, robot.getConviction() - 10);
                                }
                            }
                        }
                        if ((!hasEnemy) && convictionBalance > ecConviction) {
                            rc.empower(distanceSquared);
                            return true;
                        }
                        if (convictionBalance > ecConviction + 20) {
                            rc.empower(distanceSquared);
                            return true;
                        }
                        if (distanceSquared <= 1 && convictionBalance > 0) {
                            RobotInfo[] allyRobots = rc.senseNearbyRobots(loc, 1, Constants.ALLY_TEAM);
                            if ((allyRobots.length + 1) == numNeighborsOpen) {
                                rc.empower(distanceSquared);
                                return true;
                            }
                        }
                        // Stay
                        return true;
                    } else {
                        // Pathfind to nearest cardinal-adjacent square
                        if (closestCardinalAdjacentSquare == null) {
                            Pathfinder.execute(loc);
                        } else {
                            Pathfinder.execute(closestCardinalAdjacentSquare);
                        }
                        return true;
                    }
                }
            } else {
                if (distanceSquared <= 16) {
                    if (distanceSquared <= 1 || distanceSquared <= 9 && rc.senseNearbyRobots(distanceSquared).length == 1) {
                        rc.empower(distanceSquared);
                        return true;
                    }
                    MapLocation closestCardinalAdjacentSquare = null;
                    int closestDistanceSquared = Integer.MAX_VALUE;
                    for (Direction direction : Constants.CARDINAL_DIRECTIONS) {
                        MapLocation neighbor = loc.add(direction);
                        if (rc.onTheMap(neighbor)) {
                            RobotInfo neighborRobot = rc.senseRobotAtLocation(neighbor);
                            if (neighborRobot == null) {
                                int dist = Cache.MY_LOCATION.distanceSquaredTo(neighbor);
                                if (dist < closestDistanceSquared) {
                                    closestCardinalAdjacentSquare = neighbor;
                                    closestDistanceSquared = dist;
                                }
                            }
                        }
                    }
                    if (closestCardinalAdjacentSquare == null) {
                        if (distanceSquared <= 2) {
                            rc.empower(distanceSquared);
                        } else {
                            Pathfinder.execute(loc);
                        }
                    } else {
                        Pathfinder.execute(closestCardinalAdjacentSquare);
                    }
                    return true;
                }
            }
        }
        Pathfinder.execute(loc);
        return true;
    }

    private static Comparator<MapLocation> compareECs = Comparator.comparingInt(loc -> Cache.MY_LOCATION.distanceSquaredTo(loc));

    public static boolean goToEC() {
        MapLocation bestNeutralEC = MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).minLocation(compareECs).orElse(null);
        MapLocation bestEnemyEC = MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).minLocation(compareECs).orElse(null);
        if (bestNeutralEC == null && bestEnemyEC == null) {
            return false;
        }
        if (bestEnemyEC == null || (bestNeutralEC != null && compareECs.compare(bestNeutralEC, bestEnemyEC) < 0)) {
            // go for neutral EC
            Pathfinder.execute(bestNeutralEC);
        } else {
            // go for enemy EC
            if (!shouldAttack(bestEnemyEC)) {
                return false;
            }
            Pathfinder.execute(bestEnemyEC);
        }
        return true;
    }

    public static boolean shouldAttack(MapLocation loc) {
        if (rc.getRoundNum() <= 300) return true;
        if (attacking) return true;
        return loc.distanceSquaredTo(Cache.MY_LOCATION) >= 121;
    }

    public static boolean tryEmpowerEnemy() throws GameActionException {
        if (Cache.ENEMY_ROBOTS.length + Cache.NEUTRAL_ROBOTS.length == 0) {
            return false;
        }
        if (tryEmpowerSplash()) {
            return true;
        }
        if (tryEmpower1v1Enemy()) {
            return true;
        }
        return false;
    }

    public static boolean tryEmpowerSplash() throws GameActionException {
        if (Cache.ENEMY_ROBOTS.length <= 1) {
            return false;
        }
        Direction bestDirection = Direction.CENTER;
        long bestScore = Long.MIN_VALUE;
        int bestEmpowerDistanceSquared = -1;
        // Scoring for Direction.CENTER
        {
            // unrolled loop
            long score1 = getMultiEmpowerScore(Cache.MY_LOCATION, 1);
            if (score1 > bestScore) {
                bestScore = score1;
                bestEmpowerDistanceSquared = 1;
            }
            long score2 = getMultiEmpowerScore(Cache.MY_LOCATION, 2);
            if (score2 > bestScore) {
                bestScore = score2;
                bestEmpowerDistanceSquared = 2;
            }
            long score4 = getMultiEmpowerScore(Cache.MY_LOCATION, 4);
            if (score4 > bestScore) {
                bestScore = score4;
                bestEmpowerDistanceSquared = 4;
            }
            long score5 = getMultiEmpowerScore(Cache.MY_LOCATION, 5);
            if (score5 > bestScore) {
                bestScore = score5;
                bestEmpowerDistanceSquared = 5;
            }
            long score8 = getMultiEmpowerScore(Cache.MY_LOCATION, 8);
            if (score8 > bestScore) {
                bestScore = score8;
                bestEmpowerDistanceSquared = 8;
            }
            long score9 = getMultiEmpowerScore(Cache.MY_LOCATION, 9);
            if (score9 > bestScore) {
                bestScore = score9;
                bestEmpowerDistanceSquared = 9;
            }
        }
        // Scoring for direction towards centroid
        if (bestScore >= 100_000L) {
            MapLocation enemyCentroid = getSensedEnemiesCentroid();
            Direction directionToCentroid = Cache.MY_LOCATION.directionTo(enemyCentroid);
            if (directionToCentroid != Direction.CENTER) {
                for (Direction direction : Constants.getAttemptOrder(directionToCentroid)) {
                    if (rc.canMove(direction)) {
                        MapLocation location = Cache.MY_LOCATION.add(direction);
                        // is empowering here the best?
                        // we can empower [1, 2, 4, 5, 8, 9]
                        // unrolled loop
                        long score1 = getMultiEmpowerScore(location, 1);
                        if (score1 > bestScore) {
                            bestScore = score1;
                            bestDirection = direction;
                            bestEmpowerDistanceSquared = 1;
                        }
                        long score2 = getMultiEmpowerScore(location, 2);
                        if (score2 > bestScore) {
                            bestScore = score2;
                            bestDirection = direction;
                            bestEmpowerDistanceSquared = 2;
                        }
                        long score4 = getMultiEmpowerScore(location, 4);
                        if (score4 > bestScore) {
                            bestScore = score4;
                            bestDirection = direction;
                            bestEmpowerDistanceSquared = 4;
                        }
                        long score5 = getMultiEmpowerScore(location, 5);
                        if (score5 > bestScore) {
                            bestScore = score5;
                            bestDirection = direction;
                            bestEmpowerDistanceSquared = 5;
                        }
                        long score8 = getMultiEmpowerScore(location, 8);
                        if (score8 > bestScore) {
                            bestScore = score8;
                            bestDirection = direction;
                            bestEmpowerDistanceSquared = 8;
                        }
                        long score9 = getMultiEmpowerScore(location, 9);
                        if (score9 > bestScore) {
                            bestScore = score9;
                            bestDirection = direction;
                            bestEmpowerDistanceSquared = 9;
                        }
                        break;
                    }
                }
            }
        }
        if (bestEmpowerDistanceSquared == -1) {
            return false;
        } else {
            if (bestDirection == Direction.CENTER) {
                rc.empower(bestEmpowerDistanceSquared);
            } else {
                if (Constants.DEBUG_DRAW) {
                    Util.setIndicatorLine(Cache.MY_LOCATION, Cache.MY_LOCATION.add(bestDirection), 0, 255, 0);
                }
                Util.move(bestDirection);
            }
            return true;
        }
    }

    public static MapLocation getSensedEnemiesCentroid() {
        int x = 0;
        int y = 0;
        int numEnemies = Cache.ENEMY_ROBOTS.length;
        for (int i = numEnemies; --i >= 0;) {
            RobotInfo enemy = Cache.ENEMY_ROBOTS[i];
            x += enemy.location.x;
            y += enemy.location.y;
        }
        return new MapLocation(x / numEnemies, y / numEnemies);
    }

    public static long getMultiEmpowerScore(MapLocation location, int radiusSquared) {
        // score by kills, then damage
        RobotInfo[] robots = rc.senseNearbyRobots(location, radiusSquared, null);
        if (robots.length == 0) {
            return Long.MIN_VALUE;
        }
        int damagePerUnit = currentDamage / robots.length; // conservative estimate
        if (damagePerUnit == 0) {
            return Long.MIN_VALUE;
        }

        long pKills = 0;
        long mKills = 0;
        long ecKills = 0;
        int totalDamageValue = 0;
        int maxDistance = 0;
        int numEnemies = 0;

        for (int i = robots.length; --i >= 0;) {
            RobotInfo robot = robots[i];
            if (robot.getTeam() == Constants.ENEMY_TEAM) {
                numEnemies++;
                maxDistance = Math.max(maxDistance, location.distanceSquaredTo(robot.getLocation()));
                totalDamageValue += getDamageValue(damagePerUnit, robot);
                switch (robot.getType()) {
                    case POLITICIAN:
                        int enemyConviction = robot.getConviction();
                        if (enemyConviction > 10 && damagePerUnit >= enemyConviction - 10) {
                            // putting a politician at 10 influence or under renders them useless
                            pKills++;
                        }
                        break;
                    case ENLIGHTENMENT_CENTER:
                        if (damagePerUnit > robot.getConviction()) {
                            ecKills++;
                        }
                        break;
                    case MUCKRAKER:
                        if (damagePerUnit > robot.getConviction()) {
                            mKills++;
                        }
                        break;
                }
            }
        }
        if (ecKills == 0 && (mKills + pKills) <= 1 && totalDamageValue <= 30) {
            return Long.MIN_VALUE;
        }
        // totalDamageValue / currentDamage <= 0.3
        if (totalDamageValue * 10 <= 3 * currentDamage) {
            return Long.MIN_VALUE;
        }
        return ecKills * 1_000_000_000L + (mKills + pKills) * 50_000L + totalDamageValue * 1_000L - maxDistance;
    }

    public static boolean tryEmpower1v1Enemy() throws GameActionException {
        // Find the closest target "worth" to 1v1 trade
        //  - find the closest square that isolates
        //  - pathfind/empower
        return LambdaUtil.arraysStreamMin(Cache.ENEMY_ROBOTS,
                r -> worthToKill(r),
                Comparator.comparingInt(r -> Cache.MY_LOCATION.distanceSquaredTo(r.getLocation())))
                .map(r -> {
                    MapLocation rLocation = r.getLocation();
                    Util.setIndicatorLine(Cache.MY_LOCATION, rLocation, 255, 0, 0); // red
                    // TODO: would empowering here kill the singular target?
                    int closestDistanceSquared = Integer.MAX_VALUE;
                    MapLocation closestLocation = null;
                    for (int i = Constants.FLOOD_OFFSET_X_9.length; --i >= 0;) {
                        MapLocation location =
                                rLocation.translate(Constants.FLOOD_OFFSET_X_9[i], Constants.FLOOD_OFFSET_Y_9[i]);
                        int empowerDistanceSquared = rLocation.distanceSquaredTo(location);
                        if (rc.senseNearbyRobots(location, empowerDistanceSquared, null).length == 1) {
                            Util.setIndicatorDot(location, 0, 255, 0); // green
                            // isolated location
                            int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(location);
                            if (distanceSquared < closestDistanceSquared) {
                                closestLocation = location;
                                closestDistanceSquared = distanceSquared;
                            }
                        }
                    }
                    if (closestLocation == null) {
                        // hopefully this never happens
                        Pathfinder.execute(rLocation);
                        Util.setIndicatorLine(Cache.MY_LOCATION, rLocation, 255, 128, 0); // orange
                    } else {
                        if (closestDistanceSquared == 0) {
                            try {
                                rc.empower(Cache.MY_LOCATION.distanceSquaredTo(rLocation));
                            } catch (Exception ex) {
                                throw new IllegalStateException(ex);
                            }
                        } else {
                            Pathfinder.execute(closestLocation);
                        }
                    }
                    return true;
                }).orElse(false);
    }

    public static boolean worthToKill(RobotInfo enemy) {
        if (MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM)
                .getClosestLocationDistance(Cache.MY_LOCATION, Integer.MAX_VALUE) > 100) {
            return false;
        }
        return 10 * getDamageValue(currentDamage, enemy) >=
                (enemy.getType() == RobotType.MUCKRAKER ? (isSmallPolitician ? 2 : 5) : 9) * currentDamage;
    }

    // Does not handle empowering or chasing after muckrakers/other enemies
    public static boolean tryFortifyNearSlanderers() throws GameActionException {
        // let's try to find some slanderers to protect
        // Chooses a fortify direction based on:
        //      1. furthest from the nearest politician (<= 5 dist ^ 2)
        //      2. within 10 or so distance squared from the nearest slanderer
        double bestScore = Double.MAX_VALUE;
        Direction bestDirection = null;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.CENTER || rc.canMove(direction)) {
                MapLocation location = Cache.MY_LOCATION.add(direction);
                int sDistSquared = getDistanceSquaredToClosestSensedSlanderer(location);
                if (sDistSquared != Integer.MAX_VALUE) {
                    double sDistFactor = (Math.sqrt(sDistSquared) - (earlyGame ? 3 : 4));
                    int pDistSquared = getDistanceSquaredToClosestSensedPolitician(location, earlyGame ? 5 : 8);
                    // goal: minimize score
                    double score = sDistFactor * sDistFactor - pDistSquared;
                    if (score < bestScore) {
                        bestDirection = direction;
                        bestScore = score;
                    }
                }
            }
        }

        if (bestDirection == null) {
            // we can't find any slanderer
            if (hasFortified) {
                doNotFortify = true;
            }
            return false;
        } else {
            hasFortified = true;
            Util.setIndicatorDot(Cache.MY_LOCATION, 102, 51, 0); // brown
            Util.move(bestDirection);
            return true;
        }
    }
    public static int getDistanceSquaredToClosestSensedPolitician(MapLocation location, int upperBound) {
        return LambdaUtil.arraysStreamMin(Cache.ALLY_ROBOTS,
                r -> r.getType() == RobotType.POLITICIAN,
                r -> r.getLocation().distanceSquaredTo(location), upperBound);
    }
    public static int getDistanceSquaredToClosestSensedSlanderer(MapLocation location) {
        return LambdaUtil.arraysStreamMin(Cache.ALLY_ROBOTS,
                r -> UnitCommunication.isPotentialSlanderer(r),
                r -> r.getLocation().distanceSquaredTo(location), Integer.MAX_VALUE);
    }
    // value of damaging a politician w/ a specified conviction and influence
    public static int getDamageValue(int damage, RobotInfo enemy) {
        switch (enemy.getType()) {
            case POLITICIAN:
                // reminder: this is potentially a slanderer
                // (there is value because it gets converted into politician after 50 turns)
                int enemyConviction = enemy.getConviction();
                int enemyInfluence = enemy.getInfluence();
                return Math.max(0, Math.min(damage, enemyConviction - 10)) +
                        Math.max(0, Math.min(damage - enemyConviction, enemyInfluence) - 10);
            case ENLIGHTENMENT_CENTER:
                return currentDamage;
            case MUCKRAKER:
                // there is value in having the enemy get negative conviction
                return Math.min(currentDamage, enemy.getConviction() + 1);
            default:
                // should never happen
                throw new IllegalStateException();
        }
    }
}
