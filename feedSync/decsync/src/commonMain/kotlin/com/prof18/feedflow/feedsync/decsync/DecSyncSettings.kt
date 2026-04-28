package com.prof18.feedflow.feedsync.decsync

import com.russhwolf.settings.Settings

class DecSyncSettings(private val settings: Settings) {

    fun setDirPath(path: String) {
        settings.putString(KEY_DIR_PATH, path)
    }

    fun getDirPath(): String? = settings.getStringOrNull(KEY_DIR_PATH)

    fun clearAll() {
        settings.remove(KEY_DIR_PATH)
    }

    companion object {
        private const val KEY_DIR_PATH = "decsync_dir_path"
    }
}
