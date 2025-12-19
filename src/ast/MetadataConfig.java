package ast;

public class MetadataConfig {
    public String owner;
    public String version;
    public String description;

    @Override
    public String toString() {
        return "  Metadata:\n" +
               "    Owner: " + owner + "\n" +
               "    Version: " + version + "\n" +
               "    Description: " + (description != null ? description : "N/A") + "\n";
    }
}