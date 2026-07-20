package com.qagenie.testbe.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-cutting entry/exit logging for every REST controller method.
 * Logs: timestamp, correlation id, HTTP method + URI, input arguments
 * (redacting binary/multipart payloads and password-like fields), the
 * outcome and elapsed time.
 *
 * Applying this via AOP keeps controllers free of boilerplate logging code.
 */
@Aspect
@Component
public class ControllerLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger("CONTROLLER_AUDIT");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CORRELATION_ID = "correlationId";

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(CORRELATION_ID, correlationId);

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Instant start = Instant.now();

        String httpMethod = "N/A";
        String uri = "N/A";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            httpMethod = request.getMethod();
            uri = request.getRequestURI();
        }

        String inputFields = safeSerializeArgs(joinPoint);

        log.info("--> ENTRY [{}] {}.{}() | {} {} | timestamp={} | input={}",
                correlationId, className, methodName, httpMethod, uri, start, inputFields);

        try {
            Object result = joinPoint.proceed();
            long elapsedMs = java.time.Duration.between(start, Instant.now()).toMillis();
            log.info("<-- EXIT  [{}] {}.{}() | status=SUCCESS | durationMs={} | timestamp={}",
                    correlationId, className, methodName, elapsedMs, Instant.now());
            return result;
        } catch (Throwable ex) {
            long elapsedMs = java.time.Duration.between(start, Instant.now()).toMillis();
            log.warn("<-- EXIT  [{}] {}.{}() | status=ERROR({}) | durationMs={} | timestamp={}",
                    correlationId, className, methodName, ex.getClass().getSimpleName(), elapsedMs, Instant.now());
            throw ex;
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }

    private String safeSerializeArgs(ProceedingJoinPoint joinPoint) {
        try {
            String[] paramNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
            Object[] args = joinPoint.getArgs();
            Map<String, Object> fields = new HashMap<>();

            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                String name = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;

                if (arg == null) {
                    continue;
                }
                if (arg instanceof MultipartFile mf) {
                    fields.put(name, "MultipartFile[name=" + mf.getOriginalFilename() + ", size=" + mf.getSize() + "]");
                } else if (arg instanceof HttpServletRequest) {
                    continue; // skip raw servlet request/response objects
                } else if (isLikelyBindingResultOrResponse(arg)) {
                    continue;
                } else {
                    fields.put(name, redactSensitive(name, arg));
                }
            }
            return MAPPER.writeValueAsString(fields);
        } catch (Exception e) {
            return "<unserializable>";
        }
    }

    private boolean isLikelyBindingResultOrResponse(Object arg) {
        String typeName = arg.getClass().getSimpleName().toLowerCase();
        return typeName.contains("bindingresult") || typeName.contains("httpservletresponse");
    }

    private Object redactSensitive(String fieldName, Object value) {
        String lower = fieldName.toLowerCase();
        if (lower.contains("password") || lower.contains("secret") || lower.contains("token")) {
            return "***REDACTED***";
        }
        return value;
    }
}
