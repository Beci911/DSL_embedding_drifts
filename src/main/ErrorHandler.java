package main;

import java.util.Arrays;
import java.util.List;

public class ErrorHandler {

    // Known keywords, time units, method and channel names used to produce typo suggestions.
    // Keep this list aligned with the grammar tokens to improve suggestion quality.
    private static final List<String> KNOWN_KEYWORDS = Arrays.asList(
        // Keywords
        "monitor", "source", "baseline", "drift_check", "every", "method", 
        "threshold", "alert", "severity", "feature_drift", "on", "significance",
        "metadata", "owner", "version", "description",
        
        // Time Units
        "minutes", "hours", "daily", "weekly", "monthly",
        
        // Drift Methods
        "wasserstein_distance", "kl_divergence", "psi", "mmd", 
        "cosine_similarity", "js_divergence", "hellinger_distance",
        
        // Statistical Tests
        "ks_test", "chi_square", "mann_whitney", "t_test",
        
        // Channels & Severity
        "slack", "email", "pagerduty", "webhook", "sms",
        "low", "medium", "high", "critical"
    );

    // Return a suggestion string if a close match is found, otherwise empty.
    public static String getSuggestion(String typo) {
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String keyword : KNOWN_KEYWORDS) {
            // Skip exact matches (case-insensitive) to avoid suggesting identical tokens
            if (keyword.equalsIgnoreCase(typo)) continue;

            int distance = levenshteinDistance(typo, keyword);

            // Heuristic: If distance is small (<= 2 edits) and it's the best so far
            if (distance <= 2 && distance < minDistance) {
                minDistance = distance;
                bestMatch = keyword;
            }
        }

        if (bestMatch != null) {
            return "Did you mean '" + bestMatch + "'?";
        }
        return "";
    }

    // Standard Levenshtein Distance Algorithm (Dynamic Programming)
    private static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(
                        dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1),
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1
                    );
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }
}