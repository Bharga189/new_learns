package com.example.java25.scopedvalues;



/**
 * Java 25: Scoped Values (JEP 506)
 *
 * <p>Java 24: sharing immutable, request-scoped context across methods and
 * threads commonly used {@link ThreadLocal}. {@code ThreadLocal} values are
 * mutable, can leak if not removed, and are expensive to inherit correctly in
 * some structured-concurrency styles. Scoped Values existed only as a preview
 * API, so this repository's {@code --release 24} (no {@code --enable-preview})
 * code could not rely on them as a final feature.
 *
 * <p>Java 25: {@link ScopedValue} is final. A scoped value is bound for the
 * dynamic scope of a call with {@code where(...).run(...)} or
 * {@code where(...).call(...)}. The binding is immutable while in scope and is
 * automatically unavailable after the scope exits.
 */
public class ScopedValuesExample {

    private static final ThreadLocal<String> JAVA24_REQUEST_ID = new ThreadLocal<>();
    private static final ScopedValue<String> JAVA25_REQUEST_ID = ScopedValue.newInstance();

    public static void main(String[] args) throws Exception {
        // Java 24 style: ThreadLocal must be set and removed carefully.
        JAVA24_REQUEST_ID.set("req-24");
        try {
            System.out.println("Java 24 ThreadLocal: " + handleWithThreadLocal());
        } finally {
            JAVA24_REQUEST_ID.remove();
        }

        // Java 25: scoped binding is visible only inside the dynamic scope.
        ScopedValue.where(JAVA25_REQUEST_ID, "req-25").run(() -> {
            System.out.println("Java 25 ScopedValue: " + handleWithScopedValue());
        });

        System.out.println("Java 25 ScopedValue bound outside scope: " + JAVA25_REQUEST_ID.isBound());

        String nested = ScopedValue.where(JAVA25_REQUEST_ID, "outer").call(() ->
                ScopedValue.where(JAVA25_REQUEST_ID, "inner").call(() -> JAVA25_REQUEST_ID.get()));
        System.out.println("Java 25 nested rebinding reads inner: " + nested);
    }

    private static String handleWithThreadLocal() {
        return "handled " + JAVA24_REQUEST_ID.get();
    }

    private static String handleWithScopedValue() {
        return "handled " + JAVA25_REQUEST_ID.get();
    }
}
