package com.example.java24.classfileapi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;

/**
 * Java 24: Class-File API (JEP 484)
 *
 * <p>Java 23: the JDK had no final, standard API for parsing, generating, or
 * transforming Java class files. Code that needed to inspect bytecode typically
 * used third-party libraries (such as ASM or Byte Buddy) or preview/incubator
 * APIs that were not usable in this repository's {@code --release 23} setup.
 *
 * <p>Java 24: {@code java.lang.classfile} is final. It provides an immutable,
 * tree-oriented model for class files. {@link java.lang.classfile.ClassFile}
 * can parse bytes into a {@link java.lang.classfile.ClassModel}, letting code
 * inspect class metadata, fields, methods, and instructions with standard JDK
 * types.
 */
public class ClassFileApiExample {

    public static void main(String[] args) throws IOException {
        // Java 23 style: without a standard API, bytecode inspection relied on
        // third-party libraries or reflection for limited metadata.
        System.out.println("Java 23 style: inspect classes with reflection/library tooling");
        System.out.println("Java 23 reflection method count for sample: "
                + Sample.class.getDeclaredMethods().length);

        // Java 24: parse the class file bytes with the standard Class-File API.
        byte[] classBytes = readClassBytes(Sample.class);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        System.out.println("Java 24 ClassFile parsed class: " + classModel.thisClass().asInternalName());

        long declaredMethods = classModel.methods().stream()
                .map(MethodModel::methodName)
                .map(name -> name.stringValue())
                .filter(name -> !name.equals("<init>"))
                .count();
        System.out.println("Java 24 declared method names (excluding <init>): " + declaredMethods);

        System.out.println("Java 24 method names: " + classModel.methods().stream()
                .map(method -> method.methodName().stringValue())
                .toList());
    }

    private static byte[] readClassBytes(Class<?> type) throws IOException {
        String resource = type.getName().replace('.', '/') + ".class";
        ClassLoader loader = type.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        try (InputStream input = loader.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Class resource not found: " + resource);
            }
            return input.readAllBytes();
        }
    }

    static final class Sample {
        String greet(String name) {
            return "Hello, " + name;
        }

        int add(int left, int right) {
            return left + right;
        }
    }
}
