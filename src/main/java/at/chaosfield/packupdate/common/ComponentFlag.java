package at.chaosfield.packupdate.common;

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
     * Setting both this option and [[ServeOnly]] is undefined behaviour (In current implementation,
     * a component with both flags would not be installed on any side)
     */
    ClientOnly("client_only"),

    /**
     * Force Overwriting of files by this component
     *
     * Normally PackUpdate will error if a file not maintained by PackUpdate would be overwritten. Setting this option will silently overwrite
     * any files. For config files this additionally means that any user changes to the config get overwritten each update
     */
    ForceOverwrite("force_overwrite"),

    /**
     * Marks this mod as optional.
     *
     * An optional mod can be disabled by the end user in MultiMC and PackUpdate will leave it that way.
     * Any update will be applied without enabling the mod
     */
    Optional("optional"),

    /**
     * Makes the mod be disabled from MultiMCs perspective
     *
     * This Option is only really useful in combination with Optional. It makes the mod disabled by default, but possible to enable by the user
     */
    Disabled("disabled")

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
