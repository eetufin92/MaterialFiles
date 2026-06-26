/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.settings

import android.content.ComponentName
import android.os.Bundle
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.zhanghai.android.files.R
import me.zhanghai.android.files.app.packageManager
import me.zhanghai.android.files.file.FileAssociation
import me.zhanghai.android.files.ui.PreferenceFragmentCompat
import me.zhanghai.android.files.util.valueCompat

class FileAssociationListPreferenceFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {}

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        Settings.FILE_ASSOCIATIONS.observe(viewLifecycleOwner) { onFileAssociationsChanged(it) }
    }

    private fun onFileAssociationsChanged(fileAssociations: List<FileAssociation>) {
        var preferenceScreen = preferenceScreen
        val context = requireContext()
        if (preferenceScreen == null) {
            preferenceScreen = preferenceManager.createPreferenceScreen(context)
            setPreferenceScreen(preferenceScreen)
        } else {
            preferenceScreen.removeAll()
        }

        for (association in fileAssociations) {
            val preference = Preference(context).apply {
                key = association.extension
                title = association.extension.uppercase()
                val componentName = association.componentName?.let {
                    ComponentName.unflattenFromString(it)
                }
                summary = if (componentName != null) {
                    try {
                        packageManager.getActivityInfo(componentName, 0).loadLabel(packageManager)
                    } catch (e: Exception) {
                        association.componentName
                    }
                } else {
                    context.getString(R.string.settings_file_association_none)
                }
                isPersistent = false
                onPreferenceClickListener = this@FileAssociationListPreferenceFragment
            }
            preferenceScreen.addPreference(preference)
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val extension = preference.key
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(extension.uppercase())
            .setMessage(R.string.settings_file_association_remove_message)
            .setPositiveButton(R.string.remove) { _, _ ->
                val associations = Settings.FILE_ASSOCIATIONS.valueCompat.toMutableList()
                associations.removeAll { it.extension == extension }
                Settings.FILE_ASSOCIATIONS.putValue(associations)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        return true
    }
}
