package mrush.util;

import battlecode.common.MapLocation;

import java.util.Optional;
import java.util.function.Consumer;

public class SlandererQueue {
    private MapLocation[] locations;
    private boolean[] bad;
    private int[] turnNumbers;
    private int index;
    private int size;
    private int capacity;
    private int nextLoc = 0;
    public SlandererQueue(int maxCapacity) {
        this.locations = new MapLocation[maxCapacity];
        this.turnNumbers = new int[maxCapacity];
        this.bad = new boolean[maxCapacity];
        this.index = 0;
        this.size = 0;
        this.capacity = maxCapacity;
    }
    public void removeExpiredLocations(int turnNumber) {
        while (size > 0 && turnNumbers[index % capacity] < turnNumber) {
            index++;
            size--;
        }
    }
    public boolean contains(MapLocation location) {
        for (int i = size; --i >= 0;) {
            if (location.equals(locations[(index + i) % capacity])) {
                return true;
            }
        }
        return false;
    }
    public void add(MapLocation location, int turnNumber) {
        if (size == capacity) {
            int index = (this.index++) % capacity;
            locations[index] = location;
            turnNumbers[index] = turnNumber;
            // bad[index] = false;
        } else {
            int index = (this.index + (size++)) % capacity;
            locations[index] = location;
            turnNumbers[index] = turnNumber;
            // bad[index] = false;
        }
    }
    public Optional<MapLocation> getRandomLocation() {
        if (size == 0) {
            return Optional.empty();
        }
        int randomIndex = (int) (Math.random() * size);
        if (bad[(index + randomIndex) % capacity]) {
            return Optional.empty();
        }
        // nextLoc = (nextLoc + 1) % size;
        return Optional.of(locations[(index + randomIndex) % capacity]);
    }
    public Optional<MapLocation> getClosestLocation() {
        MapLocation closestLocation = null;
        int closestDistanceSquared = Integer.MAX_VALUE;
        for (int i = size; --i >= 0;) {
            MapLocation location = locations[(index + i) % capacity];
            int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(location);
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closestLocation = location;
            }
        }
        if (closestLocation == null) {
            return Optional.empty();
        }
        return Optional.of(closestLocation);
    }
    public Optional<MapLocation> getClosestLocationCut() {
        MapLocation closestLocation = null;
        int closestDistanceSquared = Integer.MAX_VALUE;
        for (int i = size; --i >= 0;) {
            if (bad[(index + i) % capacity]) {
                continue;
            }
            MapLocation location = locations[(index + i) % capacity];
            MapLocation cut = Util.borderCut(location);
            int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(cut);
            if (distanceSquared <= 13) {
                bad[(index + i) % capacity] = true;
            } else {
                if (distanceSquared < closestDistanceSquared) {
                    closestDistanceSquared = distanceSquared;
                    closestLocation = cut;
                }
            }
        }
        if (closestLocation == null) {
            return Optional.empty();
        }
        return Optional.of(closestLocation);
    }
    public void forEach(Consumer<MapLocation> consumer) {
        for (int i = size; --i >= 0;) {
            if (bad[(index + i) % capacity]) {
                continue;
            }
            consumer.accept(locations[(index + i) % capacity]);
        }
    }
}
