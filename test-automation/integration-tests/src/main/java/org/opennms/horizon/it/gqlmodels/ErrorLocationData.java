package org.opennms.horizon.it.gqlmodels;

public class ErrorLocationData {
    private int line;
    private int column;

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    @Override
    public String toString() {
        return "ErrorLocationData{" +
            "line=" + line +
            ", column=" + column +
            '}';
    }
}
