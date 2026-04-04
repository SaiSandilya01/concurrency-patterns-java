-- KEYS[1] = The unique key for the lock (e.g., "CRON_JOB_LOCK")
-- ARGV[1] = The unique node identifier trying to release it (e.g., "UUID_Server_A")

local current_owner = redis.call("GET", KEYS[1])

if current_owner == ARGV[1] then 
    return redis.call("DEL", KEYS[1])
end

return 0
