/*
 * Copyright (c) 2021, Alibaba Group Holding Limited;
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.gaiax.render.view.container

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import app.visly.stretch.Size
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.alibaba.gaiax.GXTemplateEngine
import com.alibaba.gaiax.context.GXTemplateContext
import com.alibaba.gaiax.render.node.GXNode
import com.alibaba.gaiax.render.node.GXNodeUtils
import com.alibaba.gaiax.render.node.GXTemplateNode
import com.alibaba.gaiax.template.GXTemplateKey
import com.alibaba.gaiax.utils.getStringExt
import com.alibaba.gaiax.utils.getStringExtCanNull

/**
 * @suppress
 */
class GXViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var childTemplateItem: GXTemplateEngine.GXTemplateItem? = null
    var childItemViewPort: Size<Float?>? = null
    var childVisualNestTemplateNode: GXTemplateNode? = null
}

/**
 * @suppress
 */
class GXContainerViewAdapter(val gxTemplateContext: GXTemplateContext, val gxNode: GXNode, val container: GXContainer) : RecyclerView.Adapter<GXViewHolder>() {

    private var containerData: JSONArray = JSONArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GXViewHolder {
        val childTemplateItem = viewTypeMap[viewType] ?: throw IllegalArgumentException("GXTemplateItem not exist, viewType = $viewType, viewTypeMap = $viewTypeMap")
        val childVisualNestTemplateNode = getVisualNestTemplateNode(childTemplateItem)
        val childItemViewPort = GXNodeUtils.computeItemViewPort(gxTemplateContext, gxNode)
        val childView = GXTemplateEngine.instance.createView(childTemplateItem, GXTemplateEngine.GXMeasureSize(childItemViewPort.width, childItemViewPort.height), childVisualNestTemplateNode)

//        val childContainer = GXViewFactory.createView<View>(parent.context, GXViewKey.VIEW_TYPE_VIEW)
//        val containerWidthLP = childItemViewPort.width?.toInt() ?: FrameLayout.LayoutParams.WRAP_CONTENT
//        val containerHeightLP = childItemViewPort.height?.toInt() ?: FrameLayout.LayoutParams.WRAP_CONTENT
//        childContainer.layoutParams = FrameLayout.LayoutParams(containerWidthLP, containerHeightLP)

        return GXViewHolder(childView).apply {
            this.childTemplateItem = childTemplateItem
            this.childVisualNestTemplateNode = childVisualNestTemplateNode
            this.childItemViewPort = childItemViewPort
        }
    }

    private fun getVisualNestTemplateNode(gxTemplateItem: GXTemplateEngine.GXTemplateItem): GXTemplateNode? {
        gxNode.childTemplateItems?.forEach {
            if (it.first.templateId == gxTemplateItem.templateId) {
                return it.second
            }
        }
        return null
    }

    override fun onBindViewHolder(holder: GXViewHolder, position: Int) {
        val itemData = containerData.getJSONObject(holder.adapterPosition) ?: JSONObject()
        val templateData = GXTemplateEngine.GXTemplateData(itemData)

//        val childTemplateItem = holder.childTemplateItem ?: throw IllegalArgumentException("childTemplateItem is null")
//        val childVisualNestTemplateNode = holder.childVisualNestTemplateNode ?: throw IllegalArgumentException("childVisualNestTemplateNode is null")
//        val childItemViewPort = holder.childItemViewPort ?: throw IllegalArgumentException("childItemViewPort is null")
//         (holder.itemView as ViewGroup).addView(childView)

        GXTemplateEngine.instance.bindData(holder.itemView, templateData)
    }

    /**
     * key: viewType
     * value: templateItem
     */
    private val viewTypeMap: MutableMap<Int, GXTemplateEngine.GXTemplateItem> = mutableMapOf()

    /**
     * key: position
     * value: templateItem
     */
    private val positionMap: MutableMap<Int, GXTemplateEngine.GXTemplateItem> = mutableMapOf()

    override fun getItemViewType(position: Int): Int {
        val templateItem = getCurrentPositionTemplateItem(position)
        if (templateItem != null) {
            val viewType: Int = templateItem.templateId.hashCode()
            viewTypeMap[viewType] = templateItem
            positionMap[position] = templateItem
            return viewType
        }
        return super.getItemViewType(position)
    }

    /**
     * Gets the template ID of the current pit
     */
    private fun getCurrentPositionTemplateItem(position: Int): GXTemplateEngine.GXTemplateItem? {
        gxNode.childTemplateItems?.let { items ->
            if (items.size > 1) {
                val itemData = containerData.getJSONObject(position)
                val dataBinding = gxNode.templateNode.dataBinding
                dataBinding?.reset()
                dataBinding?.getExtend(itemData)?.let { typeData ->
                    val path = typeData.getStringExt("${GXTemplateKey.GAIAX_DATABINDING_ITEM_TYPE}.${GXTemplateKey.GAIAX_DATABINDING_ITEM_TYPE_PATH}")
                    val templateId = typeData.getStringExtCanNull("${GXTemplateKey.GAIAX_DATABINDING_ITEM_TYPE}.${GXTemplateKey.GAIAX_DATABINDING_ITEM_TYPE_CONFIG}.${path}")
                    if (templateId != null) {
                        return items.firstOrNull { it.first.templateId == templateId }?.first
                    }
                }
            } else {
                return items.firstOrNull()?.first
            }
        }
        return null
    }

    override fun getItemCount(): Int {
        return containerData.size
    }

    fun setContainerData(data: JSONArray) {
        viewTypeMap.clear()
        positionMap.clear()
        containerData = data
        // TODO The efficiency is not high here, so it needs the same data comparison scheme on both ends.
        // TODO After discussion with @Shenmeng, there is no similar scheme on iOS terminal temporarily,
        // TODO but one can be implemented. Set this change to low priority
        notifyDataSetChanged()
    }

    fun isNeedForceRefresh(targetWidth: Float): Boolean {
        return gxNode.stretchNode.finalLayout?.width != targetWidth
    }

}