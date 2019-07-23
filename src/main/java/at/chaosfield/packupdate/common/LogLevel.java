package at.chaosfield.packupdate.common;

public enum LogLevel {
    Debug("DEBG"),
    Info("INFO"),
    Warning("WARN"),
    Error("ERR"),
    Trace("TRCE");

    String name;

    LogLevel(String name) {
        this.name = name;
    }
}
