package com.example.java9.processapi;

import java.util.Arrays;

/**
 * Java 9 Process API Example
 * 
 * Java 9 introduced a new Process API under java.lang.process package,
 * providing significantly improved support for process management.
 * 
 * Java 8 and earlier:
 * - Limited process management via Runtime.getRuntime().exec()
 * - ProcessBuilder for creating operating system processes
 * - Basic PID awareness, but no easy way to get process info
 * - Hard to manage/orphan processes, destroyProcessDesc
 * 
 * Java 9 New Process API (java.lang.ProcessHandle):
 * - ProcessHandle.current() - get handle to current process
 * - ProcessHandle.allProcesses() - view all running processes
 * - ProcessHandle.of(pid) - get handle by PID (platform-dependent)
 * - ProcessInfo: command(), pid(), parent(), children(), info()
 * - Process.destroy() - destroy the process
 * - Process.destroyForcibly() - force destroy the process
 * - Subscription to process lifecycle events
 * 
 * Example usage:
 * ProcessHandle current = ProcessHandle.current();
 * current.info().command().ifPresent(cmd -> 
 *     System.out.println("Command: " + cmd));
 * 
 * Realtime usage: Monitoring processes, finding PIDs, 
 * managing system processes, building DevOps tools,
 * detecting zombie processes, resource management
 */
public class ProcessApiDemo {

    public static void main(String[] args) {
        // Java 9: Process API - get current process info
        ProcessHandle current = ProcessHandle.current();
        System.out.println("Current PID: " + current.pid());
        
        // Get process command line
        current.info().command().ifPresent(cmd -> 
            System.out.println("Command: " + cmd));
        
        // Check if process is alive
        if (current.isAlive()) {
            System.out.println("Process is still running");
        }
        
        // Realtime usage: Monitoring processes, finding PIDs, 
        // managing system processes, building DevOps tools,
        // detecting zombie processes, resource management
    }
}