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

package me.gm.cleaner.plugin.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.gm.cleaner.plugin.xposed.hooker.InsertHooker
import me.gm.cleaner.plugin.xposed.hooker.QueryHooker
import me.gm.cleaner.plugin.xposed.util.FileUtils
import me.gm.cleaner.plugin.xposed.util.MimeUtils

data class Template(
    @field:SerializedName("template_name") val templateName: String,
    @field:SerializedName("hook_operation") val hookOperation: List<String>,
    @field:SerializedName("apply_to_app") val applyToApp: List<String>?,
    @field:SerializedName("permitted_media_types") val permittedMediaTypes: List<Int>?,
    @field:SerializedName("filter_path") val filterPath: List<String>?,
)

class Templates(json: String?) : ArrayList<Template>() {
    init {
        if (!json.isNullOrEmpty()) {
            addAll(
                Gson().fromJson(json, Array<Template>::class.java)
            )
        }
    }

    fun matchedTemplates(cls: Class<*>, packageName: String): List<Template> = filter { template ->
        when (cls) {
            QueryHooker::class.java -> template.hookOperation.contains("query")
            InsertHooker::class.java -> template.hookOperation.contains("insert")
            else -> throw IllegalArgumentException()
        } && template.applyToApp?.contains(packageName) == true
    }

    companion object {
        fun List<Template>.filterNot(
            dataList: List<String>, mimeTypeList: List<String>
        ): List<Boolean> = dataList.zip(mimeTypeList).map { (data, mimeType) ->
            any { template ->
                MimeUtils.resolveMediaType(mimeType) !in template.permittedMediaTypes ?: emptyList() ||
                        template.filterPath?.any { FileUtils.startsWith(it, data) } == true
            }
        }
    }
}
