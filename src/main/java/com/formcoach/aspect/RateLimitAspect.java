package com.formcoach.aspect;

import com.formcoach.annotation.RateLimit;
import com.formcoach.common.BusinessException;
import com.formcoach.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-based rate limiting with graceful degradation to in-memory counters.
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, Long> localCounters = new ConcurrentHashMap<>();

    public RateLimitAspect(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit);

        if (!exceedsLimit(key, rateLimit)) {
            return pjp.proceed();
        }

        log.warn("Rate limit exceeded: key={}, max={}", key, rateLimit.maxRequests());
        throw new BusinessException(ErrorCode.RATE_LIMIT);
    }

    private boolean exceedsLimit(String key, RateLimit rl) {
        // Try Redis first (distributed, accurate)
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1) {
                    redisTemplate.expire(key, rl.duration(), rl.timeUnit());
                }
                return count != null && count > rl.maxRequests();
            }
        } catch (Exception e) {
            log.debug("Redis unavailable for rate limit, falling back to local: {}", e.getMessage());
        }

        // Fallback: local in-memory counter (per-instance, best-effort)
        long now = System.currentTimeMillis();
        long windowMs = rl.timeUnit().toMillis(rl.duration());
        String localKey = key + ":" + (now / windowMs);
        long count = localCounters.merge(localKey, 1L, Long::sum);
        if (count == 1) {
            // Simple cleanup: remove old windows
            localCounters.entrySet().removeIf(e -> !e.getKey().contains(":" + (now / windowMs)));
        }
        return count > rl.maxRequests();
    }

    private String buildKey(RateLimit rl) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
            String userId = "anonymous";
            var auth = request.getUserPrincipal();
            if (auth != null) userId = auth.getName();
            return String.format("rate_limit:%s:%s:%s", rl.key(), userId, request.getRemoteAddr());
        } catch (Exception e) {
            return "rate_limit:" + rl.key() + ":unknown";
        }
    }
}
