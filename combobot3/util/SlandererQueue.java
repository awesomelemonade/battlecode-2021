package combobot3.util;

import battlecode.common.MapLocation;

import java.util.Optional;
import java.util.function.Consumer;

public class SlandererQueue {
    private MapLocation[] locations;
    private int[] turnNumbers;
    private int index;
    private int size;
    private int capacity;
    // Enemy Prediction
    public MapLocation firstLocation;
    private boolean first = true;
    public boolean ignoreFirst = false;

    public SlandererQueue(int maxCapacity) {
        this.locations = new MapLocation[maxCapacity];
        this.turnNumbers = new int[maxCapacity];
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
        if (location.equals(firstLocation)) {
            return true;
        }
        for (int i = size; --i >= 0;) {
            if (locations[(index + i) % capacity].equals(location)) {
                return true;
            }
        }
        return false;
    }
    public void add(MapLocation location, int turnNumber) {
        if (firstLocation == null) {
            firstLocation = location;
            return;
        }
        first = false;
        if (size == capacity) {
            int index = (this.index++) % capacity;
            locations[index] = location;
            turnNumbers[index] = turnNumber;
        } else {
            int index = (this.index + (size++)) % capacity;
            locations[index] = location;
            turnNumbers[index] = turnNumber;
        }
    }
    public Optional<MapLocation> getRandomLocation() {
        if (size == 0) {
            if (firstLocation != null && !ignoreFirst) {
                return Optional.of(firstLocation);
            }
            return Optional.empty();
        }
        int randomIndex = (int) (Math.random() * size);
        return Optional.of(locations[(index + randomIndex) % capacity]);
    }
    public Optional<MapLocation> getClosestLocation() {
        if (first && firstLocation != null && !ignoreFirst) {
            return Optional.of(firstLocation);
        }
        MapLocation closestLocation = null;
        int closestDistanceSquared = Integer.MAX_VALUE;
        for (int i = size; --i >= 0;) {
            MapLocation location = locations[(index + i) % capacity];
            if (firstLocation != null && location.equals(firstLocation)) {
                continue;
            }
            if (!MapInfo.potentiallyInBounds(location)) {
                continue;
            }
            int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(location);
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closestLocation = location;
            }
        }
        return Optional.ofNullable(closestLocation);
    }
    public void forEach(Consumer<MapLocation> consumer) {
        for (int i = size; --i >= 0;) {
            consumer.accept(locations[(index + i) % capacity]);
        }
    }
}
