package com.example.redislock;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedisLockManager {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)\\}");

    private final StringRedisTemplate redis;
    private final RedisLockProperties properties;
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ExpressionParser parser = new SpelExpressionParser();

    public RedisLockManager(StringRedisTemplate redis, RedisLockProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public RedisLockProperties getProperties() {
        return properties;
    }

    public RedisPriorityLock createLock(String lockKey, boolean highPriority, long waitTime, long leaseTime) {
        long w = waitTime > 0 ? waitTime : properties.getWaitTime();
        long l = leaseTime > 0 ? leaseTime : properties.getLeaseTime();
        return new RedisPriorityLock(redis, lockKey, highPriority, w, l, properties.getRetryInterval());
    }

    public String buildKey(String overridePrefix, String[] keys, Method method, Object[] args) {
        String prefix = (overridePrefix != null && !overridePrefix.isBlank())
                ? overridePrefix : properties.getPrefix();
        String keyPart;
        if (keys == null || keys.length == 0) {
            keyPart = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        } else {
            List<String> evaluated = new ArrayList<>();
            for (String k : keys) {
                evaluated.add(evaluate(k, method, args));
            }
            keyPart = String.join(":", evaluated);
        }
        return prefix + ":" + keyPart;
    }

    private String evaluate(String key, Method method, Object[] args) {
        if (key == null) return "";
        if (key.contains("{")) {
            Matcher m = PLACEHOLDER.matcher(key);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String expr = m.group(1);
                String val = evalSpel(expr, method, args);
                m.appendReplacement(sb, Matcher.quoteReplacement(val));
            }
            m.appendTail(sb);
            return sb.toString();
        }
        try {
            return evalSpel(key, method, args);
        } catch (Exception e) {
            return key;
        }
    }

    private String evalSpel(String expr, Method method, Object[] args) {
        org.springframework.context.expression.MethodBasedEvaluationContext ctx =
                new org.springframework.context.expression.MethodBasedEvaluationContext(
                        null, method, args, nameDiscoverer);
        String expression = expr.trim();
        if (!expression.startsWith("#") && !expression.startsWith("'") && !expression.matches(".*[()\\s+\\-*/].*")) {
            // treat bare identifier as a variable reference
            expression = "#" + expression;
        }
        Object val = parser.parseExpression(expression).getValue(ctx);
        return val == null ? "" : String.valueOf(val);
    }
}
