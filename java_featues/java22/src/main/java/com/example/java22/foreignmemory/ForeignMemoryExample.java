package com.example.java22.foreignmemory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Java 22: Foreign Function & Memory API (JEP 454)
 *
 * Java 21: java.lang.foreign was a preview API. Using it required
 * --enable-preview, and the API could still change between releases. For safe
 * off-heap memory the practical options were ByteBuffer.allocateDirect() or
 * sun.misc.Unsafe.
 *
 * Java 22: the API is final. Arena ties off-heap memory to a lexical scope and
 * frees it deterministically on close, while MemorySegment gives bounds-checked
 * access with explicit layouts.
 *
 * (Native function calls use the same API's Linker; this example stays with
 * memory segments, which need no native access flags.)
 */
public class ForeignMemoryExample {

    public static void main(String[] args) {
        // Java 21 style: direct ByteBuffer, released only by the garbage collector
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(3 * Integer.BYTES);
        buffer.putInt(0, 10);
        buffer.putInt(4, 20);
        buffer.putInt(8, 30);
        System.out.println("Java 21 ByteBuffer sum: "
                + (buffer.getInt(0) + buffer.getInt(4) + buffer.getInt(8)));

        // Java 22: a confined Arena frees the segment when the block exits
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(3L * Integer.BYTES);
            segment.set(ValueLayout.JAVA_INT, 0, 10);
            segment.set(ValueLayout.JAVA_INT, 4, 20);
            segment.set(ValueLayout.JAVA_INT, 8, 30);

            int sum = segment.get(ValueLayout.JAVA_INT, 0)
                    + segment.get(ValueLayout.JAVA_INT, 4)
                    + segment.get(ValueLayout.JAVA_INT, 8);
            System.out.println("Java 22 MemorySegment sum: " + sum);
            System.out.println("Java 22 MemorySegment bytes: " + segment.byteSize());
        }
    }
}
