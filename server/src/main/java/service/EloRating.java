package service;

public final class EloRating {
    private EloRating() { }
    public static int after(int rating, int opponent, double score) {
        double expected = 1.0 / (1.0 + Math.pow(10.0, (opponent - rating) / 400.0));
        return Math.max(100, (int) Math.round(rating + 32 * (score - expected)));
    }
}
