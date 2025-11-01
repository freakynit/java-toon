package com.freakynit.toon;

public class ToonConfig {
    private String delimiter;
    private int indent;
    private String lengthMarker;

    public ToonConfig() {
        this.delimiter = ",";
        this.indent = 2;
        this.lengthMarker = "";
    }

    public ToonConfig(String delimiter, int indent, String lengthMarker) {
        this.delimiter = delimiter != null ? delimiter : ",";
        this.indent = Math.max(1, indent);
        this.lengthMarker = lengthMarker != null ? lengthMarker : "";
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public int getIndent() {
        return indent;
    }

    public void setIndent(int indent) {
        this.indent = Math.max(1, indent);
    }

    public String getLengthMarker() {
        return lengthMarker;
    }

    public void setLengthMarker(String lengthMarker) {
        this.lengthMarker = lengthMarker;
    }

    public String getDelimiterDisplay() {
        if (",".equals(delimiter)) {
            return "";
        }
        return delimiter;
    }
}
