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

package me.gm.cleaner.plugin.module

import android.content.Intent
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.ApplistItemBinding
import me.gm.cleaner.plugin.drawer.DrawerActivity
import me.gm.cleaner.plugin.util.AppIconCache
import me.gm.cleaner.plugin.util.buildStyledTitle
import me.gm.cleaner.plugin.util.setOnMenuItemClickListener

class AppListAdapter(fragment: AppListFragment) :
    ListAdapter<PreferencesPackageInfo, AppListAdapter.ViewHolder>(CALLBACK) {
    private val activity = fragment.requireActivity() as DrawerActivity
    private lateinit var selectedHolder: ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ApplistItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val pi = getItem(position)
        holder.loadIconJob = AppIconCache.loadIconBitmapAsync(
            activity, pi.applicationInfo, pi.applicationInfo.uid / 100000, binding.icon
        )
        binding.title.text = pi.label
        binding.summary.text = if (pi.srCount > 0) {
            activity.buildStyledTitle(pi.srCount.toString())
        } else {
            pi.packageName
        }
        binding.root.setOnClickListener {
            activity.startActivity(
                Intent(activity, SettingsActivity::class.java).putExtra(
                    SettingsConstants.APP_INFO, pi.applicationInfo
                )
            )
        }
        binding.root.setOnLongClickListener {
            selectedHolder = holder
            false
        }
        binding.root.setOnCreateContextMenuListener { menu: ContextMenu, _: View?, _: ContextMenuInfo? ->
            activity.menuInflater.inflate(R.menu.item_delete_all_rules, menu)
            menu.setHeaderTitle(pi.label)
            if (pi.srCount == 0) {
                menu.removeItem(R.id.menu_delete_all_rules)
            } else {
                menu.setOnMenuItemClickListener(::onContextItemSelected)
            }
        }
    }

    private fun onContextItemSelected(item: MenuItem): Boolean {
        if (!::selectedHolder.isInitialized) return false
        val position = selectedHolder.bindingAdapterPosition
        val pi = getItem(position)!!
        if (item.itemId == R.id.menu_delete_all_rules) {
            ModulePreferences.removePackage(pi.packageName)
            return true
        }
        return false
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.onViewRecycled()
    }

    class ViewHolder(val binding: ApplistItemBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var loadIconJob: Job

        fun onViewRecycled() {
            if (::loadIconJob.isInitialized && loadIconJob.isActive) {
                loadIconJob.cancel()
            }
        }
    }

    companion object {
        private val CALLBACK: DiffUtil.ItemCallback<PreferencesPackageInfo> =
            object : DiffUtil.ItemCallback<PreferencesPackageInfo>() {
                override fun areItemsTheSame(
                    oldItem: PreferencesPackageInfo, newItem: PreferencesPackageInfo
                ) = oldItem.applicationInfo.packageName == newItem.applicationInfo.packageName

                override fun areContentsTheSame(
                    oldItem: PreferencesPackageInfo, newItem: PreferencesPackageInfo
                ) = oldItem.srCount == newItem.srCount
                // TODO: oldItem == newItem
            }
    }
}
