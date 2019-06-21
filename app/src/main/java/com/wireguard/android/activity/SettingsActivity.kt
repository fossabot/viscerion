/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.SparseArray
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.commit
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.BuildConfig
import com.wireguard.android.R
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.di.ext.getBackendAsync
import com.wireguard.android.di.ext.getPrefs
import com.wireguard.android.di.ext.getTunnelManager
import com.wireguard.android.fragment.AppListDialogFragment
import com.wireguard.android.services.TaskerIntegrationService
import com.wireguard.android.util.asString
import com.wireguard.android.util.isPermissionGranted
import com.wireguard.android.util.isSystemDarkThemeEnabled
import com.wireguard.android.util.updateAppTheme
import java.io.File

/**
 * Interface for changing application-global persistent settings.
 */

typealias ClickListener = Preference.OnPreferenceClickListener
typealias ChangeListener = Preference.OnPreferenceChangeListener
typealias SummaryProvider<T> = Preference.SummaryProvider<T>

class SettingsActivity : AppCompatActivity() {
    private val permissionRequestCallbacks = SparseArray<(permissions: Array<String>, granted: IntArray) -> Unit>()
    private var permissionRequestCounter: Int = 0

    fun ensurePermissions(
        permissions: Array<String>,
        function: (permissions: Array<String>, granted: IntArray) -> Unit
    ) {
        val needPermissions = ArrayList<String>(permissions.size)
        for (permission in permissions) {
            if (!this.isPermissionGranted(permission))
                needPermissions.add(permission)
        }
        if (needPermissions.isEmpty()) {
            val granted = IntArray(permissions.size) {
                PackageManager.PERMISSION_GRANTED
            }
            function(permissions, granted)
            return
        }
        val idx = permissionRequestCounter++
        permissionRequestCallbacks.put(idx, function)
        ActivityCompat.requestPermissions(
                this,
                needPermissions.toTypedArray(), idx
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.commit {
                add(android.R.id.content, SettingsFragment())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val f = permissionRequestCallbacks.get(requestCode)
        if (f != null) {
            permissionRequestCallbacks.remove(requestCode)
            f(permissions, grantResults)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(), AppListDialogFragment.AppExclusionListener {

        private val prefs = getPrefs()

        override fun onCreatePreferences(savedInstanceState: Bundle?, key: String?) {
            addPreferencesFromResource(R.xml.preferences)
            val screen = preferenceScreen
            val ctx = requireContext()
            val wgQuickOnlyPrefs = arrayOf(
                    screen.findPreference<Preference>("tools_installer"),
                    screen.findPreference<CheckBoxPreference>("restore_on_boot")
            )
            val debugOnlyPrefs = arrayOf(
                    screen.findPreference<SwitchPreferenceCompat>("force_userspace_backend")
            )
            val wgOnlyPrefs = arrayOf(
                    screen.findPreference<CheckBoxPreference>("whitelist_exclusions")
            )
            val exclusionsPref = preferenceManager.findPreference<Preference>("global_exclusions")
            val taskerPref = preferenceManager.findPreference<SwitchPreferenceCompat>("allow_tasker_integration")
            val integrationSecretPref =
                    preferenceManager.findPreference<EditTextPreference>("intent_integration_secret")
            val altIconPref = preferenceManager.findPreference<CheckBoxPreference>("use_alt_icon")
            val darkThemePref = preferenceManager.findPreference<CheckBoxPreference>("dark_theme")
            for (pref in wgQuickOnlyPrefs + wgOnlyPrefs + debugOnlyPrefs)
                pref?.isVisible = false

            if (BuildConfig.DEBUG && File("/sys/module/wireguard").exists())
                debugOnlyPrefs.filterNotNull().forEach { it.isVisible = true }

            getBackendAsync().thenAccept { backend ->
                wgQuickOnlyPrefs.filterNotNull().forEach {
                    if (backend is WgQuickBackend)
                        it.isVisible = true
                    else
                        screen.removePreference(it)
                }
                wgOnlyPrefs.filterNotNull().forEach {
                    if (backend is GoBackend)
                        it.isVisible = true
                    else
                        screen.removePreference(it)
                }
            }

            integrationSecretPref?.isVisible = prefs.allowTaskerIntegration

            exclusionsPref?.onPreferenceClickListener = ClickListener {
                val fragment = AppListDialogFragment.newInstance(prefs.exclusionsArray, true, this)
                fragment.show(requireFragmentManager(), null)
                true
            }

            taskerPref?.onPreferenceChangeListener = ChangeListener { _, newValue ->
                val isEnabled = newValue as Boolean
                integrationSecretPref?.isVisible = isEnabled
                val intent = Intent(ctx, TaskerIntegrationService::class.java)
                ctx.apply { if (isEnabled) startService(intent) else stopService(intent) }
                true
            }

            integrationSecretPref?.summaryProvider = SummaryProvider<EditTextPreference> { preference ->
                if (prefs.allowTaskerIntegration &&
                        preference.isEnabled &&
                        prefs.taskerIntegrationSecret.isEmpty()
                )
                    getString(R.string.tasker_integration_summary_empty_secret)
                else
                    getString(R.string.tasker_integration_secret_summary)
            }

            altIconPref?.onPreferenceClickListener = ClickListener {
                val checked = (it as CheckBoxPreference).isChecked
                ctx.packageManager.apply {
                    setComponentEnabledSetting(
                            ComponentName(ctx.packageName, "${ctx.packageName}.LauncherActivity"),
                            if (checked)
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            else
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                    )
                    setComponentEnabledSetting(
                            ComponentName(ctx.packageName, "${ctx.packageName}.AltIconLauncherActivity"),
                            if (checked)
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            else
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                    )
                }
                Snackbar.make(
                        requireView(),
                        getString(R.string.pref_alt_icon_apply_message),
                        Snackbar.LENGTH_SHORT
                ).show()
                true
            }

            darkThemePref?.apply {
                val isSystemDark = ctx.isSystemDarkThemeEnabled()
                val darkThemeOverride = prefs.useDarkTheme
                summaryProvider = SummaryProvider<CheckBoxPreference> {
                    if (isSystemDark) {
                        getString(R.string.dark_theme_summary_auto)
                    } else {
                        getString(R.string.pref_dark_theme_summary)
                    }
                }
                onPreferenceClickListener = ClickListener {
                    updateAppTheme(prefs.useDarkTheme)
                    true
                }
                if (isSystemDark && !darkThemeOverride) {
                    isEnabled = false
                    isChecked = true
                    /*
                    HACK ALERT: Open for better solutions
                    Toggling checked state here causes the preference key's value to flip as well
                    which causes a plethora of bugs later on. So as a "fix" we'll just restore the
                    original value back to avoid the whole mess.
                     */
                    prefs.useDarkTheme = darkThemeOverride
                } else {
                    isEnabled = true
                    isChecked = darkThemeOverride
                }
            }
        }

        override fun onExcludedAppsSelected(excludedApps: List<String>) {
            if (excludedApps.asString() == prefs.exclusions) return
            getTunnelManager().getTunnels().thenAccept { tunnels ->
                if (excludedApps.isNotEmpty()) {
                    tunnels.forEach { tunnel ->
                        val oldConfig = tunnel.getConfig()
                        oldConfig?.let {
                            prefs.exclusionsArray.forEach { exclusion ->
                                it.`interface`.excludedApplications.remove(
                                        exclusion
                                )
                            }
                            it.`interface`.excludedApplications.addAll(excludedApps.toCollection(ArrayList()))
                            tunnel.setConfig(it)
                        }
                    }
                    prefs.exclusions = excludedApps.asString()
                } else {
                    tunnels.forEach { tunnel ->
                        prefs.exclusionsArray.forEach { exclusion ->
                            tunnel.getConfig()?.`interface`?.excludedApplications?.remove(exclusion)
                        }
                    }
                    prefs.exclusions = ""
                }
            }
        }
    }
}
