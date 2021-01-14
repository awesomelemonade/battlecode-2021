package buildorderv3.util;

import battlecode.common.MapLocation;

import java.util.Optional;
import java.util.function.Consumer;

public class SlandererQueue {
    private MapLocation[] locations;
    private int[] turnNumbers;
    private int index;
    private int size;
    private int capacity;
    public SlandererQueue(int maxCapacity) {
        this.locations = new MapLocation[maxCapacity];
        this.turnNumbers = new int[maxCapacity];
        this.index = 0;
        this.size = 0;
        this.capacity = maxCapacity;
    }
    public void removeExpiredLocations(int turnNumber) {
        while (size > 0) {
            if (turnNumbers[(index + size) % capacity] < turnNumber) {
                this.index++;
                size--;
            } else {
                break;
            }
        }
    }
    public boolean contains(MapLocation location) {
        for (int i = size; --i >= 0;) {
            if (locations[(index + i) % size].equals(location)) {
                return true;
            }
        }
        return false;
    }
    public void add(MapLocation location, int turnNumber) {
        int index = (this.index + (size++)) % capacity;
        locations[index] = location;
        turnNumbers[index] = turnNumber;
    }
    public Optional<MapLocation> getRandomLocation() {
        if (size == 0) {
            return Optional.empty();
        }
        int randomIndex = (int) (Math.random() * size);
        return Optional.of(locations[(index + randomIndex) % capacity]);
    }
    public Optional<MapLocation> getClosestLocation() {
        if (size == 0) {
            return Optional.empty();
        }
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
        return Optional.of(closestLocation);
    }
    public void forEach(Consumer<MapLocation> consumer) {
        for (int i = size; --i >= 0;) {
            consumer.accept(locations[(index + i) % capacity]);
        }
    }
}
