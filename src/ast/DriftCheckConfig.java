package ast;
import java.util.ArrayList;
import java.util.List;

public class DriftCheckConfig {
    public int intervalNumber;
    public String intervalUnit;
    public String method;
    public double threshold;
    public List<String> alertChannels = new ArrayList<>();
    public String severity; // Optional — may be null if not specified

    @Override
    public String toString() {
        return "  Drift Check (every " + intervalNumber + " " + intervalUnit + "):\n" +
               "    Method: " + method + "\n" +
               "    Threshold: " + threshold + "\n" +
               "    Alerts: " + alertChannels + "\n" +
               "    Severity: " + (severity != null ? severity : "N/A") + "\n";
    }
}