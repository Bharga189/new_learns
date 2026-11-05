package com.example.java15.textblocks;

/** Java 15 Text Blocks Example */
public class TextBlocksExample {

    public static void main(String[] args) {
        // Java 11 style with escape sequences
        String java11Html = "<html>\n    <body>\n        <p>Hello</p>\n    </body>\n</html>";
        System.out.println("Java 11: " + java11Html);
        
        // Java 15 text block
        String java12Html = """
            <html>
                <body>
                    <p>Hello</p>
                </body>
            </html>
            """;
        System.out.println("Java 15: " + java12Html);
    }
}