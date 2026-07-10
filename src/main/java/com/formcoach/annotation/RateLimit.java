package com.formcoach.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiting annotation.
 * Apply on Controller methods to limit request frequency.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** Max requests allowed in the time window */
    int maxRequests() default 10;

    /** Time window duration */
    int duration() default 60;

    /** Time unit of the window */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /** Rate limit key prefix */
    String key() default "rate_limit";
}
