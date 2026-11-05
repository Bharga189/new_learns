package com.example.java14.switchdemo;

/**
 * Java 14 Switch Expressions Example
 * 
 * Java 14 made switch expressions standard (preview in Java 12 and Java 13),
 * adding yield and arrow syntax.
 * 
 * Java 11 and earlier:
 * - Switch fell through cases automatically
 * - Required break statements
 * - Could not be used as an expression (no return value)
 * - switch statement with enum: switch (day) { case MONDAY: ... }
 * 
 * Java 14 switch expression features:
 * - Arrow syntax: case x -> ... (no break needed)
 * - yield keyword: return a value from switch
 * - Expression form: int result = switch (...) { ... }
 * - No fall-through by default
 * - instanceof pattern matching enhancement
 * 
 * Example:
 *   int result = switch (day) {
 *       case MONDAY, FRIDAY, SUNDAY -> 6;
 *       case TUESDAY -> 7;
 *       default -> 8;
 *   };
 * 
 * Realtime usage: Cleaner switch statements, expression assignments,
 * pattern matching, reduced boilerplate with break statements
 */
public class SwitchExpressionExample {

    // Day of week enum
    enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
    
    public static void main(String[] args) {
        // Java 11 style: switch statement with break
        Day today = Day.MONDAY;
        int java11Result;
        switch (today) {
            case MONDAY:
            case FRIDAY:
            case SUNDAY:
                java11Result = 6;
                break;
            case TUESDAY:
                java11Result = 7;
                break;
            default:
                java11Result = 8;
                break;
        }
        System.out.println("Java 11 switch result: " + java11Result);
        
        // Java 12 switch expression with yield/arrow
        int java12Result = switch (today) {
            case MONDAY, FRIDAY, SUNDAY -> 6;
            case TUESDAY -> 7;
            default -> 8;
        };
        System.out.println("Java 14 switch expression: " + java12Result);
        
        // Switch with yield (earlier versions)
        int yieldResult = switch (today) {
            case MONDAY -> {
                yield 6;
            }
            case TUESDAY -> 7;
            default -> 8;
        };
        System.out.println("Java 12 with yield: " + yieldResult);
        
        // Realtime usage: Cleaner switch statements, expression assignments,
        // pattern matching, reduced boilerplate with break statements
    }
}