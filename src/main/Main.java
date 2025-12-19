package main;

import parser.DriftConfigParser;
import parser.ParseException;
import parser.Token;
import ast.MonitorConfig;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        // --- Argument parsing (flags and input file) ---
        boolean generateOutput = false;
        String filePath = null;
        boolean runTests = false;

        for (String arg : args) {
            if (arg.equals("--run-all-tests")) {
                runTests = true;
            } else if (arg.equals("-g") || arg.equals("--generate")) {
                generateOutput = true;
            } else if (!arg.startsWith("-")) {
                filePath = arg;
            }
        }

        // --- Execution mode and dispatch ---
        if (runTests) {
            // We pass 'generateOutput' to tests too, in case you want to see test outputs
            TestRunner.runAll(generateOutput); 
            return;
        }

        if (filePath == null) {
            System.err.println("X Error: No input file specified.");
            printUsage();
            return;
        }

        processFile(filePath, generateOutput);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  Validation only:   java main.Main <file.drift>");
        System.out.println("  Generate Python:   java main.Main <file.drift> -g");
        System.out.println("  Run Test Suite:    java main.Main --run-all-tests");
    }

    // Core processing logic. Returns true if successful.
    // filePath: Path to the .drift file
    // generateCode: If true, generates Python code in 'generated/' folder
    public static boolean processFile(String filePath, boolean generateCode) {
        System.out.println("Processing: " + filePath);

        try {
            FileInputStream fis = new FileInputStream(filePath);
            DriftConfigParser parser = new DriftConfigParser(fis);
            
            // 1. PARSE
            List<MonitorConfig> configs = parser.Root();
            
            // 2. VALIDATE
            Validator validator = new Validator();
            List<String> semanticErrors = validator.validate(configs);

            if (!semanticErrors.isEmpty()) {
                System.err.println("X Semantic Errors Found:");
                for (String err : semanticErrors) {
                    System.err.println("  - " + err);
                }
                return false;
            }

            System.out.println("? Validation Successful.");

            // 3. GENERATE (Optional)
            if (generateCode) {
                // Ensure 'generated' directory exists
                File outputDir = new File("generated");
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                // Extract filename (e.g., "test1.drift" -> "test1.py")
                File sourceFile = new File(filePath);
                String filename = sourceFile.getName().replace(".drift", ".py");
                String fullOutputPath = "generated/" + filename;
                
                PythonGenerator generator = new PythonGenerator();
                generator.generate(configs, fullOutputPath);
            }
            
            return true;

        } catch (ParseException e) {
            System.err.println("X Parse Error:");
            Token currentToken = e.currentToken;
            if (currentToken != null && currentToken.next != null) {
                Token badToken = currentToken.next;
                System.err.println("  Line " + badToken.beginLine + ", Column " + badToken.beginColumn);
                System.err.println("  Encountered: \"" + badToken.image + "\"");
                // Print expected tokens from the ParseException (clean up quoted literals)
                try {
                    int[][] expectedSeqs = e.expectedTokenSequences;
                    String[] tokenImage = e.tokenImage;
                    if (expectedSeqs != null && expectedSeqs.length > 0) {
                        java.util.Set<String> expectedSet = new java.util.LinkedHashSet<>();
                        for (int[] seq : expectedSeqs) {
                            if (seq == null) continue;
                            for (int tok : seq) {
                                if (tok <= 0 || tokenImage == null || tok >= tokenImage.length) continue;
                                String img = tokenImage[tok];
                                if (img == null) continue;
                                // If it's a quoted literal like "_" or ":" strip surrounding quotes
                                if (img.startsWith("\"") && img.endsWith("\"") && img.length() >= 2) {
                                    img = img.substring(1, img.length() - 1);
                                }
                                expectedSet.add(img);
                            }
                        }
                        if (!expectedSet.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (String s : expectedSet) {
                                if (sb.length() > 0) sb.append(", ");
                                sb.append(s);
                            }
                            System.err.println("  Expected: " + sb.toString());
                        }
                    }
                } catch (Exception ex) {
                    // Non-fatal: fall back to suggestion if anything goes wrong
                }

                String suggestion = ErrorHandler.getSuggestion(badToken.image);
                if (!suggestion.isEmpty()) {
                    System.err.println("  " + suggestion);
                }
            } else {
                System.err.println("  " + e.getMessage());
            }
            return false;
        } catch (FileNotFoundException e) {
            System.err.println("X File not found: " + filePath);
            return false;
        } catch (Exception e) {
            System.err.println("X Unexpected Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}