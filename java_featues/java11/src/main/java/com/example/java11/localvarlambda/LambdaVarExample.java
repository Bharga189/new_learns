package com.example.java11.localvarlambda;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Java 11 Local-Variable Syntax for Lambda Parameters (JEP 323)
 *
 * Java 11 allows 'var' as the type of lambda parameters. The main benefit is
 * that annotations can be applied to lambda parameters, because an annotated
 * parameter must have its type written explicitly.
 *
 * Java 8-10:
 *   Function<String, String> upper = s -> s.toUpperCase();
 *   // no way to annotate the parameter
 *
 * Java 11:
 *   Function<String, String> upper = (var s) -> s.toUpperCase();
 *   BiFunction<Integer, Integer, Integer> add =
 *       (@NonNull var a, @NonNull var b) -> a + b;
 *
 * Rules:
 * - If one parameter uses var, every parameter must use var.
 * - var is not a keyword, so a variable named var still works.
 */
public class LambdaVarExample {

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NonNull {
    }

    public static void main(String[] args) {
        // Java 8-10 style: implicitly typed lambda parameters
        Function<String, String> implicit = s -> s.toUpperCase();
        System.out.println("Implicit parameter: " + implicit.apply("java 11"));

        // Java 11: var in lambda parameters
        Function<String, String> withVar = (var s) -> s.toUpperCase();
        System.out.println("var parameter: " + withVar.apply("java 11"));

        // The point of var: annotations on lambda parameters
        BiFunction<Integer, Integer, Integer> add = (@NonNull var a, @NonNull var b) -> a + b;
        System.out.println("Annotated var parameters: " + add.apply(20, 22));
    }
}
