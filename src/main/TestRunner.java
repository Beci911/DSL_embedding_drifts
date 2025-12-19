package main;

import java.io.File;

public class TestRunner {

    public static void runAll(boolean generateOutput) {
        System.out.println("==================================================");
        System.out.println("? DRIFT DSL TEST SUITE");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        TestResult posResult = runDirectory("test/positive", true, generateOutput);
        passed += posResult.passed;
        failed += posResult.failed;

        System.out.println("\n--------------------------------------------------");

        TestResult negResult = runDirectory("test/negative", false, generateOutput);
        passed += negResult.passed;
        failed += negResult.failed;

        System.out.println("\n==================================================");
        System.out.println("SUMMARY");
        System.out.println("Total: " + (passed + failed) + " | Passed: " + passed + " | Failed: " + failed);
        
        if (failed == 0) System.out.println("\n? ALL TESTS PASSED!");
        else System.out.println("\n? SOME TESTS FAILED.");
    }

    private static TestResult runDirectory(String dirPath, boolean expectSuccess, boolean generateOutput) {
        File dir = new File(dirPath);
        int p = 0;
        int f = 0;

        System.out.println("Scanning: " + dirPath);
        
        if (!dir.exists() || !dir.isDirectory()) return new TestResult(0, 0);

        File[] files = dir.listFiles((d, name) -> name.endsWith(".drift"));
        if (files == null) return new TestResult(0, 0);

        for (File file : files) {
            System.out.println(); 
            // Only generate output for positive tests; negative tests won't produce generated code.
            boolean doGen = expectSuccess && generateOutput;
            
            boolean processingSucceeded = Main.processFile(file.getPath(), doGen);
            
            if (processingSucceeded == expectSuccess) {
                System.out.println("   >>> ? PASS: " + file.getName());
                p++;
            } else {
                System.out.println("   >>> ? FAIL: " + file.getName());
                f++;
            }
        }
        return new TestResult(p, f);
    }

    private static class TestResult {
        int passed, failed;
        TestResult(int p, int f) { this.passed = p; this.failed = f; }
    }
}