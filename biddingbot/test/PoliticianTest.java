package biddingbot.test;

public class PoliticianTest {
    public static void main(String[] args) {
        // already damaged case
        assertTrue(test(20, 10, 20) == 0);
        assertTrue(test(20, 5, 20) == 5);
        assertTrue(test(20, 5, 5) == 0);
        assertTrue(test(20, 10, 20) == 0);
        assertTrue(test(15, 5, 5) == 0);
        assertTrue(test(15, 1, 10) == 0);
        assertTrue(test(15, 1, 20) == 4);
        // simple damage case
        assertTrue(test(5, 20, 20) == 5);
        assertTrue(test(10, 30, 90) == 10);
        assertTrue(test(10, 20, 20) == 10);
        // leaves enemy between 0-10
        assertTrue(test(15, 20, 20) == 10);
        assertTrue(test(20, 20, 20) == 10);
        assertTrue(test(123, 132, 999) == 122);
        // converts into ally of 1-10
        assertTrue(test(20, 10, 20) == 0);
        assertTrue(test(15, 10, 20) == 0);
        assertTrue(test(1000, 5, 5) == 0);
        assertTrue(test(1000, 10, 10) == 0);
        assertTrue(test(15, 3, 30) == 2);
        assertTrue(test(15, 3, 8) == 0);
        // convert into ally of 11+
        assertTrue(test(12, 1, 10) == 0);
        assertTrue(test(12, 1, 11) == 1);
        assertTrue(test(12, 1, 50) == 1);
        assertTrue(test(50, 15, 15) == 10);
        assertTrue(test(50, 15, 100) == 30);
        assertTrue(test(200, 100, 100) == 180);
    }
    public static void assertTrue(boolean value) {
        if (!value) {
            throw new IllegalStateException();
        }
    }
    // Formula to derive pConviction in Politician.getScore()
    public static int test(int damage, int conviction, int influence) {
        return Math.max(0, Math.min(damage, conviction - 10)) +
                Math.max(0, Math.min(damage - conviction, influence) - 10);
    }
}
