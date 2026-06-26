/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filelist

import android.app.Dialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import me.zhanghai.android.files.R
import me.zhanghai.android.files.app.packageManager
import me.zhanghai.android.files.file.FileAssociation
import me.zhanghai.android.files.file.FileItem
import me.zhanghai.android.files.file.MimeType
import me.zhanghai.android.files.file.fileProviderUri
import me.zhanghai.android.files.filelist.name
import me.zhanghai.android.files.settings.Settings
import me.zhanghai.android.files.util.ParcelableArgs
import me.zhanghai.android.files.util.ParcelableParceler
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.asFileName
import me.zhanghai.android.files.util.createViewIntent
import me.zhanghai.android.files.util.extraPath
import me.zhanghai.android.files.util.finish
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.show
import me.zhanghai.android.files.util.startActivitySafe
import me.zhanghai.android.files.util.valueCompat

class OpenWithDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    private lateinit var alwaysCheckBox: CheckBox

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.open_with_dialog, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        alwaysCheckBox = view.findViewById(R.id.always_check_box)

        val intent = args.path.fileProviderUri.createViewIntent(args.mimeType)
        val resolveInfos = packageManager.queryIntentActivities(
            intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        )
        val adapter = ResolveInfoAdapter(resolveInfos) { onResolveInfoClicked(it) }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        return MaterialAlertDialogBuilder(context, theme)
            .setTitle(R.string.file_item_action_open_with)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun onResolveInfoClicked(resolveInfo: ResolveInfo) {
        val activityInfo = resolveInfo.activityInfo
        val componentName = ComponentName(activityInfo.packageName, activityInfo.name)
        if (alwaysCheckBox.isChecked) {
            val extension = args.path.name.asFileName().extensions
            if (extension.isNotEmpty()) {
                val associations = Settings.FILE_ASSOCIATIONS.valueCompat.toMutableList()
                associations.removeAll { it.extension.equals(extension, ignoreCase = true) }
                associations.add(FileAssociation(extension, componentName.flattenToString()))
                Settings.FILE_ASSOCIATIONS.putValue(associations)
            }
        }
        val intent = args.path.fileProviderUri.createViewIntent(args.mimeType)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            .apply {
                component = componentName
                extraPath = args.path
            }
        startActivitySafe(intent)
        dismiss()
        finish()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        finish()
    }

    private class ResolveInfoAdapter(
        private val resolveInfos: List<ResolveInfo>,
        private val listener: (ResolveInfo) -> Unit
    ) : RecyclerView.Adapter<ResolveInfoAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.open_with_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val resolveInfo = resolveInfos[position]
            holder.iconImage.setImageDrawable(resolveInfo.loadIcon(packageManager))
            holder.titleText.text = resolveInfo.loadLabel(packageManager)
            holder.itemView.setOnClickListener { listener(resolveInfo) }
        }

        override fun getItemCount(): Int = resolveInfos.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconImage: ImageView = view.findViewById(R.id.icon_image)
            val titleText: TextView = view.findViewById(R.id.title_text)
        }
    }

    @Parcelize
    class Args(
        val path: @WriteWith<ParcelableParceler> Path,
        val mimeType: MimeType
    ) : ParcelableArgs

    companion object {
        fun show(file: FileItem, fragment: Fragment) {
            OpenWithDialogFragment().putArgs(Args(file.path, file.mimeType)).show(fragment)
        }
    }
}
