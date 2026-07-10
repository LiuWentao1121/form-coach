package com.formcoach.aspect;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * Logs every Controller method call with method, args, IP, and elapsed time.
 */
@Slf4j
@Aspect
@Component
public class ControllerLogAspect {

    @Pointcut("execution(* com.formcoach.controller..*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String args = Arrays.stream(pjp.getArgs())
                .filter(a -> !(a instanceof HttpServletRequest))
                .map(a -> {
                    try { return JSON.toJSONString(a); }
                    catch (Exception e) { return "[unserializable]"; }
                })
                .findFirst().orElse("[]");

        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long elapsed = System.currentTimeMillis() - start;

        log.info("[{}] {} | args={} | {}ms | {}", method, uri,
                args.length() > 200 ? args.substring(0, 200) + "..." : args,
                elapsed,
                request.getRemoteAddr());

        return result;
    }
}
