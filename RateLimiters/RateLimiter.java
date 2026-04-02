public interface RateLimiter {
    /**
     * Determines if a request from the given user is allowed.
     *
     * @param userId The unique identifier for the user.
     * @return true if the request is allowed, false if it is rate-limited (HTTP 429).
     */
    boolean allowRequest(String userId);
}
