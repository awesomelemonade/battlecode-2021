package experiment.util;

import battlecode.common.MapLocation;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MapLocationList {
    private MapLocationListNode head = null;
    private int size;
    public void add(MapLocation location) {
        head = new MapLocationListNode(location, head);
        size++;
    }
    public Optional<MapLocation> getRandomLocation() {
        if (size == 0) {
            return Optional.empty();
        }
        MapLocationListNode current = head;
        for (int i = (int) (size * Math.random()); --i >= 0;) {
            current = current.next;
        }
        return Optional.of(current.location);
    }
    public boolean contains(MapLocation location) {
        MapLocationListNode current = head;
        while (current != null) {
            if (current.location.equals(location)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }
    public void removeIf(Predicate<MapLocation> predicate) {
        MapLocationListNode prev = null;
        MapLocationListNode current = head;
        while (current != null) {
            if (predicate.test(current.location)) {
                if (prev == null) {
                    head = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
            } else {
                prev = current;
            }
            current = current.next;
        }
    }
    public void forEach(Consumer<MapLocation> consumer) {
        MapLocationListNode current = head;
        while (current != null) {
            consumer.accept(current.location);
            current = current.next;
        }
    }
    public int getClosestLocationDistance(MapLocation location, int defaultDistance) {
        return getClosestLocation(location).map(x -> x.distanceSquaredTo(location)).orElse(defaultDistance);
    }
    public int getClosestLocationDistance(int defaultDistance) {
        return getClosestLocation().map(x -> x.distanceSquaredTo(Cache.MY_LOCATION)).orElse(defaultDistance);
    }
    public Optional<MapLocation> getClosestLocation() {
        return getClosestLocation(Cache.MY_LOCATION);
    }
    public Optional<MapLocation> getClosestLocation(MapLocation location) {
        return min(Comparator.comparingInt(x -> x.distanceSquaredTo(location)));
    }
    public Optional<MapLocation> min(Comparator<MapLocation> comparator) {
        if (size == 0) {
            return Optional.empty();
        }
        MapLocation minLocation = head.location;
        MapLocationListNode current = head.next;
        while (current != null) {
            MapLocation location = current.location;
            if (comparator.compare(location, minLocation) < 0) {
                minLocation = location;
            }
            current = current.next;
        }
        return Optional.of(minLocation);
    }
    public boolean isEmpty() {
        return size == 0;
    }
    static class MapLocationListNode {
        MapLocation location;
        MapLocationListNode next;
        public MapLocationListNode(MapLocation location, MapLocationListNode next) {
            this.location = location;
            this.next = next;
        }
    }
}
