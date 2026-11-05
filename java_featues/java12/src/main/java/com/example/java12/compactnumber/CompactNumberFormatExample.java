package com.example.java12.compactnumber;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Java 12 Compact Number Formatting
 *
 * NumberFormat.getCompactNumberInstance() formats numbers in a short or long
 * compact form ("1.5K", "2M", "1.5 thousand") instead of the full digits.
 *
 * Java 8-11: custom formatting logic or a third-party library
 * Java 12:   NumberFormat.getCompactNumberInstance(locale, style)
 */
public class CompactNumberFormatExample {

    public static void main(String[] args) {
        NumberFormat shortFormat =
                NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        NumberFormat longFormat =
                NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.LONG);

        System.out.println("SHORT 1,500: " + shortFormat.format(1500));         // 1.5K
        System.out.println("SHORT 2,000,000: " + shortFormat.format(2_000_000)); // 2M
        System.out.println("LONG 1,500: " + longFormat.format(1500));           // 1.5 thousand
    }
}
