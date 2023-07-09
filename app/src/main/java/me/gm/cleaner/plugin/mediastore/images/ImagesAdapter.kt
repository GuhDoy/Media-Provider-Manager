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

package me.gm.cleaner.plugin.mediastore.images

import android.graphics.drawable.Drawable
import android.transition.TransitionInflater
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.ImagesItemBinding
import me.gm.cleaner.plugin.mediastore.MediaStoreAdapter

class ImagesAdapter(private val fragment: ImagesFragment) : MediaStoreAdapter(fragment) {
    private val viewModel: ImagesViewModel by fragment.viewModels()

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MediaStoreImage -> R.layout.images_item
        else -> super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        when (viewType) {
            R.layout.images_item -> ItemViewHolder(
                ImagesItemBinding.inflate(LayoutInflater.from(parent.context))
            )

            else -> super.onCreateViewHolder(parent, viewType)
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                val binding = holder.binding
                val item = getItem(position)
                // Load the image with Glide to prevent OOM error when the image drawables are very large.
                Glide.with(fragment)
                    .load(item.contentUri)
                    .listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?, model: Any?, target: Target<Drawable?>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (fragment.lastPosition == getUriPositionForAdapterPosition(holder.bindingAdapterPosition)) {
                                fragment.startPostponedEnterTransition()
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?, model: Any?, target: Target<Drawable?>?,
                            dataSource: DataSource?, isFirstResource: Boolean
                        ): Boolean {
                            if (fragment.lastPosition == getUriPositionForAdapterPosition(holder.bindingAdapterPosition)) {
                                fragment.startPostponedEnterTransition()
                            }
                            return false
                        }
                    })
                    .centerCrop()
                    .into(binding.image)
                binding.image.transitionName = item.contentUri.toString()
                binding.card.setOnClickListener {
                    val navController = fragment.findNavController()
                    if (fragment.isInActionMode() ||
                        navController.currentDestination?.id != R.id.images_fragment
                    ) {
                        return@setOnClickListener
                    }
                    val uriPosition = getUriPositionForAdapterPosition(
                        holder.bindingAdapterPosition
                    )
                    fragment.lastPosition = uriPosition

                    val exitTransition = TransitionInflater.from(fragment.requireContext())
                        .inflateTransition(R.transition.grid_exit_transition)
                    // Exclude the clicked card from the exit transition (e.g. the card will disappear immediately
                    // instead of fading out with the rest to prevent an overlapping animation of fade and move).
                    (exitTransition as TransitionSet).excludeTarget(binding.card, true)
                    fragment.exitTransition = exitTransition

                    val images = viewModel.medias
                    val direction = ImagesFragmentDirections.actionImagesToImagePager(
                        initialPosition = uriPosition,
                        isMediaStoreUri = true,
                        uris = images.map { it.contentUri }.toTypedArray(),
                        displayNames = images.map { it.displayName }.toTypedArray()
                    )
                    val extras = FragmentNavigatorExtras(
                        binding.image to binding.image.transitionName
                    )
                    navController.navigate(direction, extras)
                }

                holder.details = object : ItemDetails<Long>() {
                    override fun getPosition(): Int = holder.bindingAdapterPosition
                    override fun getSelectionKey(): Long = item.id
                    override fun inSelectionHotspot(e: MotionEvent): Boolean = false
                    override fun inDragRegion(e: MotionEvent): Boolean = true
                }
                if (selectionTrackerInitialized) {
                    binding.card.isChecked = selectionTracker.isSelected(item.id)
                }
            }

            else -> super.onBindViewHolder(holder, position)
        }
    }

    class ItemViewHolder(val binding: ImagesItemBinding) : ViewHolder(binding.root)
}
