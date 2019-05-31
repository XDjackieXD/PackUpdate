package at.chaosfield.packupdate.common;

import scala.Option;
import scala.Some;

public enum ComponentType {
    Mod("mod"),
    Resource("resources"),
    Config("config"),
    Minecraft("minecraft"),
    Forge("forge"),
    Unknown("")

    ;

    public String stringValue;

    ComponentType(String type) {
        stringValue = type;
    }

    public static Option<ComponentType> fromString(String data) {
        for (ComponentType type : ComponentType.values()) {
            if (type.stringValue.equals(data)) {
                return new Some<>(type);
            }
        }
        return Option.empty();
    }
}