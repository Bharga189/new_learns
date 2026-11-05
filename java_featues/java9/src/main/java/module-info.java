/**
 * Java 9 Module Information File
 * 
 * This module-info.java defines the Java 9 module for this example project.
 * It demonstrates the module system (JPMS) introduced in Java 9.
 */
module com.example.java9 {
    // Java 9 module requires base module (implicit, but good to be explicit)
    requires java.base;
    
    // Exports packages that should be accessible to other modules
    exports com.example.java9.moduleinfo;
    exports com.example.java9.optionalenhancements;
    exports com.example.java9.privateinterface;
    exports com.example.java9.processapi;
    exports com.example.java9.streamenhancements;
}