package com.prof18.feedflow.feedsync.decsync;

import org.decsync.library.Decsync;
import org.decsync.library.DecsyncDroidKt;
import java.io.File;

/**
 * Java bridge to create a Decsync instance from a filesystem path.
 * Required because the Decsync class constructor is internal in Kotlin
 * and the top-level factory function conflicts with the class name.
 */
public class DecSyncFactory {
    @SuppressWarnings("unchecked")
    public static <T> Decsync<T> create(File dir, String syncType, String collection, String ownAppId) {
        return DecsyncDroidKt.Decsync(dir, syncType, collection, ownAppId);
    }
}
