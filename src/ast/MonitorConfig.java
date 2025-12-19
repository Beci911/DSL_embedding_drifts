package ast;

public class MonitorConfig {
    public String monitorName;
    public String source;
    public String baseline;
    public DriftCheckConfig driftCheck;
    public FeatureDriftConfig featureDrift; // Optional — null when not present in the DSL
    public MetadataConfig metadata;         // Optional — null when not present in the DSL

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Monitor: ").append(monitorName).append("\n");
        sb.append("  Source: ").append(source).append("\n");
        sb.append("  Baseline: ").append(baseline).append("\n");
        if (driftCheck != null) sb.append(driftCheck.toString());
        if (featureDrift != null) sb.append(featureDrift.toString());
        if (metadata != null) sb.append(metadata.toString());
        return sb.toString();
    }
}