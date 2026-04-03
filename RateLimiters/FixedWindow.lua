-- KEYS[1] = The unique key for the rate limiter (e.g., "rate_limit:User_A")
-- ARGV[1] = The maximum number of allowed requests (e.g., "5")
-- ARGV[2] = The window size in milliseconds (e.g., "2000")

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])

local current_count = redis.call("GET", key)

if current_count and tonumber(current_count) >= limit then 
    return 0
end
    
redis.call("INCR", key)

if current_count == false then 
    redis.call("PEXPIRE", key, window_ms)
end

return 1
