package at.chaosfield.packupdate;

import scala.Option;
import scala.Some;

public enum ComponentFlag {

    ServerOnly("server_only"),
    ClientOnly("client_only");

    public String internalName;

    ComponentFlag(String internalName) {

        this.internalName = internalName;
    }

    public static Option<ComponentFlag> fromString(String data) {
        switch (data) {
            case "server_only":
                return new Some<>(ServerOnly);
            case "client_only":
                return new Some<>(ClientOnly);
            default:
                return Option.empty();
        }
    }
}
