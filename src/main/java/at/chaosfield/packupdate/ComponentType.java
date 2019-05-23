package at.chaosfield.packupdate;

public enum ComponentType {
    Mod,
    Resource,
    Config,
    Unknown;

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