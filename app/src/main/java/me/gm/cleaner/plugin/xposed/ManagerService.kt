/*
 * Copyright 2021 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.xposed

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.IManagerService
import me.gm.cleaner.plugin.ParceledListSlice

@SuppressLint("PrivateApi")
abstract class ManagerService : IManagerService.Stub() {
    val context: Context by lazy {
        val activityThread = XposedHelpers.callStaticMethod(
            Class.forName("android.app.ActivityThread"), "systemMain"
        )
        val systemContext = XposedHelpers.callMethod(activityThread, "getSystemContext") as Context
        systemContext
        // TODO: create package context
    }

    override fun getServerVersion(): Int {
        return BuildConfig.VERSION_CODE
    }

    override fun getInstalledPackages(): ParceledListSlice<PackageInfo> {
        return ParceledListSlice(ArrayList())
    }

    @SuppressLint("SoonBlockedPrivateApi")
    override fun notifyPreferencesChanged() {
        try {
            val context: Context = context.createDeviceProtectedStorageContext()
            context.javaClass.getDeclaredMethod("reloadSharedPreferences").invoke(context)
        } catch (tr: Throwable) {
            tr.printStackTrace()
            System.exit(1)
        }
    }
}
