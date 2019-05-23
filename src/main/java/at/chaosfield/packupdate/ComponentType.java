package at.chaosfield.packupdate;

public enum ComponentType {
    Mod("mod"),
    Resource("resource"),
    Config("config"),
    Unknown("");

    public String stringValue;

    ComponentType(String type) {
        stringValue = type;
    }

    public static ComponentType parse(String input) {
        switch (input) {
            case "mod":
                return Mod;
            case "resource":
                return Resource;
            case "config":
                return Config;
            default:
                return Unknown;
        }
    }
}