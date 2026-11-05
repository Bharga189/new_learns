package com.example.java25.moduleimport;

import module java.base;

import java.nio.file.Path;

/**
 * Java 25: Module Import Declarations (JEP 511)
 *
 * <p>Java 24: source files imported types individually (or with package
 * {@code *} imports). Using APIs from several packages in the same module meant
 * listing each package or type import explicitly.
 *
 * <p>Java 25: {@code import module M;} imports the public top-level types
 * exported by module {@code M} to the current module, reducing import
 * boilerplate while keeping dependencies module-oriented. This example also
 * keeps an explicit {@link Path} import to show both forms can coexist.
 *
 * <p>Because this repository compiles the module with {@code --release 25}, the
 * compiler accepts the module import declaration directly.
 */
public class ModuleImportExample {

    public static void main(String[] args) {
        // Java 24 style (conceptual): explicit imports for each package/type.
        System.out.println("Java 24 style: import java.util.*; import java.util.stream.*; ...");

        // Java 25: types such as List, Map, Stream, and Collectors are available
        // through `import module java.base;` above.
        List<String> names = List.of("Alice", "Bob", "Carol");
        Map<Integer, List<String>> grouped = names.stream()
                .collect(Collectors.groupingBy(String::length));

        Path sample = Path.of("java25", "example.txt");
        System.out.println("Java 25 module import result: " + grouped);
        System.out.println("Java 25 explicit + module imports can coexist: " + sample.getFileName());
    }
}
