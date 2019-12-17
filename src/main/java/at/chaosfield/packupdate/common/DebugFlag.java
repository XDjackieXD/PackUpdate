package at.chaosfield.packupdate.common;

import scala.Option;
import scala.Some;

/**
 * Debug flags are passed comma-separated via the org.chaosfield.packupdate.debug java property
 * Example: java -D org.chaosfield.packupdate.debug=force_refresh -jar PackUpdate.jar
 */
public enum DebugFlag {
    /**
     * Pretend that all packages are corrupt and need to be reinstalled
     */
    ForceRefresh("force_refresh")

    ;

    public String internalName;

    DebugFlag(String internalName) {
        this.internalName = internalName;
    }

    public static Option<DebugFlag> fromString(String data) {
        for (DebugFlag flag : DebugFlag.values()) {
            if (flag.internalName.equals(data)) {
                return new Some<>(flag);
            }
        }
        return Option.empty();
    }
}
