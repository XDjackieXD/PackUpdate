package at.chaosfield.packupdate;

import scala.Option;
import scala.Some;

public enum ComponentFlag {

    /**
     * This mod is only needed on the server and wont be installed on the client
     *
     * Setting both this option and [[ClientOnly]] is undefined behaviour (In current implementation,
     * a component with both flags would not be installed on any side)
     */
    ServerOnly("server_only"),

    /**
     * This mod is only needed on the client and wont be installed on the server
     *
     * Setting both this option and [[ClientOnly]] is undefined behaviour (In current implementation,
     * a component with both flags would not be installed on any side)
     */
    ClientOnly("client_only"),

    /**
     * Force Overwriting of files by this component
     *
     * Normally PackUpdate will error if a file not maintained by PackUpdate would be overwritten. Setting this option will silently overwrite
     * any files. For config files this additionally means that any user changes to the config get overwritten each update
     */
    ForceOverwrite("force_overwrite")

    ;

    public String internalName;

    ComponentFlag(String internalName) {

        this.internalName = internalName;
    }

    public static Option<ComponentFlag> fromString(String data) {
        for (ComponentFlag flag : ComponentFlag.values()) {
            if (flag.internalName.equals(data)) {
                return new Some<>(flag);
            }
        }
        return Option.empty();
    }
}
