package com.impetus.user_service.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* com.impetus.user_service.service..*(..)) || execution(* com.impetus.user_service.controller..*(..))")
    public void applicationPackagePointcut() {
    }

    @Before("applicationPackagePointcut()")
    public void beforeMethod(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        logger.info("[AOP] >>> {}() START - Args: {}", methodName, joinPoint.getArgs());
    }

    @After("applicationPackagePointcut()")
    public void afterMethod(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        logger.info("[AOP] <<< {}() COMPLETED", methodName);
    }

    @AfterReturning(pointcut = "applicationPackagePointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().toShortString();
        logger.info("[AOP] <<< {}() RETURNED - Result: {}", methodName, result);
    }

    @AfterThrowing(pointcut = "applicationPackagePointcut()", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Throwable ex) {
        String methodName = joinPoint.getSignature().toShortString();
        logger.error("[AOP] !!! {}() THREW EXCEPTION - {}", methodName, ex.getMessage(), ex);
    }

    @Around("applicationPackagePointcut()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        String methodName = joinPoint.getSignature().toShortString();
        logger.info("[AOP] >>> {}() EXECUTED IN {} ms", methodName, duration);

        return result;
    }
}