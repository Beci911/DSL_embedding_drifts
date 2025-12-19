package main;

import ast.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Validator {
    private List<String> errors = new ArrayList<>();
    
    // Valid drift detection methods. Keep this list in sync with the grammar and ErrorHandler.
    private static final List<String> VALID_DRIFT_METHODS = Arrays.asList(
        "wasserstein_distance", "kl_divergence", "psi", "mmd", 
        "cosine_similarity", "js_divergence", "hellinger_distance"
    );

    // Valid statistical tests for feature-level checks.
    private static final List<String> VALID_TEST_METHODS = Arrays.asList(
        "ks_test", "chi_square", "mann_whitney", "t_test"
    );

    public List<String> validate(List<MonitorConfig> configs) {
        errors.clear();
        for (MonitorConfig config : configs) {
            validateMonitor(config);
        }
        return errors;
    }

    private void validateMonitor(MonitorConfig config) {
        // Reject monitors where source == baseline — comparing the same dataset is meaningless.
        if (config.source.equals(config.baseline)) {
            errors.add("Logic Error: Source and Baseline cannot be the same ('" + config.source + "') in monitor: " + config.monitorName);
        }

        if (config.driftCheck != null) {
            validateDriftCheck(config.driftCheck, config.monitorName);
        }

        if (config.featureDrift != null) {
            validateFeatureDrift(config.featureDrift, config.monitorName);
        }
    }

    private void validateDriftCheck(DriftCheckConfig dcc, String monitorName) {
        // Threshold must be a valid probability in [0.0, 1.0].
        if (dcc.threshold < 0.0 || dcc.threshold > 1.0) {
            errors.add("Logic Error in '" + monitorName + "': Threshold must be between 0.0 and 1.0. Found: " + dcc.threshold);
        }

        // Interval number must be positive.
        if (dcc.intervalNumber <= 0) {
            errors.add("Logic Error in '" + monitorName + "': Interval must be a positive integer. Found: " + dcc.intervalNumber);
        }

        // Check that the configured drift method exists. If not, try to offer a typo suggestion.
        if (!VALID_DRIFT_METHODS.contains(dcc.method)) {
             String suggestion = ErrorHandler.getSuggestion(dcc.method);
             errors.add("Logic Error in '" + monitorName + "': Unknown drift method '" + dcc.method + "'." + suggestion);
        }
    }

    private void validateFeatureDrift(FeatureDriftConfig fdc, String monitorName) {
        // Significance (p-value) is typically small (e.g. 0.05). Warn on suspicious values.
        if (fdc.significance <= 0.0 || fdc.significance >= 0.5) {
             errors.add("Logic Warning in '" + monitorName + "': Significance is usually small (e.g. 0.05). Found: " + fdc.significance);
        }

        // Feature list must contain at least one feature when the block is present.
        if (fdc.features.isEmpty()) {
            errors.add("Logic Error in '" + monitorName + "': Feature drift block declared but no features listed.");
        }

        // Validate the configured statistical test; offer a suggestion on typos.
        if (!VALID_TEST_METHODS.contains(fdc.method)) {
            String suggestion = ErrorHandler.getSuggestion(fdc.method);
            errors.add("Logic Error in '" + monitorName + "': Unknown test method '" + fdc.method + "'." + suggestion);
       }
    }
}