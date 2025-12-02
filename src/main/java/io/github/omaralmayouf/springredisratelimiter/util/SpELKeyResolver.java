package io.github.omaralmayouf.springredisratelimiter.util;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * Utility class for resolving SpEL expressions in rate limit keys.
 */
public class SpELKeyResolver {

    private static final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Resolves a SpEL expression using method parameters.
     *
     * @param keyExpression SpEL expression (e.g., "#phone", "#user.id", "#p0")
     * @param method        The method being invoked
     * @param args          The method arguments
     * @return Resolved key value as a string
     */
    public static String resolveKey(String keyExpression, Method method, Object[] args) {
        if (keyExpression == null || keyExpression.isEmpty()) {
            return "";
        }

        StandardEvaluationContext context = new StandardEvaluationContext();

        // Add method parameters by name
        String[] parameterNames = getParameterNames(method);
        for (int i = 0; i < parameterNames.length && i < args.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
            // Also support #p0, #p1, etc.
            context.setVariable("p" + i, args[i]);
        }

        // Add all args as an array
        context.setVariable("args", args);

        try {
            Object value = parser.parseExpression(keyExpression).getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to resolve SpEL expression: " + keyExpression, e);
        }
    }

    /**
     * Gets parameter names from method.
     * Note: In production, you might want to use Spring's ParameterNameDiscoverer
     * or compile with -parameters flag to get actual parameter names.
     */
    private static String[] getParameterNames(Method method) {
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        String[] names = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            names[i] = parameters[i].getName();
        }
        return names;
    }
}

