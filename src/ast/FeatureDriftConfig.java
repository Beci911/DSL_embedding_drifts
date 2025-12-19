package ast;
import java.util.ArrayList;
import java.util.List;

public class FeatureDriftConfig {
    public List<String> features = new ArrayList<>();
    public String method;
    public double significance; // p-value / significance threshold for the feature test

    @Override
    public String toString() {
        return "  Feature Drift:\n" +
               "    Features: " + features + "\n" +
               "    Method: " + method + "\n" +
               "    Significance: " + significance + "\n";
    }
}