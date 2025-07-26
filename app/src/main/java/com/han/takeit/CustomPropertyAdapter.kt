package com.han.takeit

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class CustomPropertyAdapter(
    private val properties: MutableList<Pair<String, String>>,
    private val onPropertyChanged: () -> Unit
) : RecyclerView.Adapter<CustomPropertyAdapter.PropertyViewHolder>() {

    class PropertyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etPropertyKey: TextInputEditText = itemView.findViewById(R.id.etPropertyKey)
        val etPropertyValue: TextInputEditText = itemView.findViewById(R.id.etPropertyValue)
        val btnRemoveProperty: ImageButton = itemView.findViewById(R.id.btnRemoveProperty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_custom_property, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = properties[position]
        
        // 清除之前的监听器
        holder.etPropertyKey.removeTextChangedListener(holder.etPropertyKey.tag as? TextWatcher)
        holder.etPropertyValue.removeTextChangedListener(holder.etPropertyValue.tag as? TextWatcher)
        
        holder.etPropertyKey.setText(property.first)
        holder.etPropertyValue.setText(property.second)
        
        // 监听键的文本变化
        val keyWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newKey = s.toString()
                properties[position] = newKey to properties[position].second
                onPropertyChanged()
            }
        }
        holder.etPropertyKey.addTextChangedListener(keyWatcher)
        holder.etPropertyKey.tag = keyWatcher
        
        // 监听值的文本变化
        val valueWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newValue = s.toString()
                properties[position] = properties[position].first to newValue
                onPropertyChanged()
            }
        }
        holder.etPropertyValue.addTextChangedListener(valueWatcher)
        holder.etPropertyValue.tag = valueWatcher
        
        // 删除按钮点击事件
        holder.btnRemoveProperty.setOnClickListener {
            properties.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, properties.size)
            onPropertyChanged()
        }
    }

    override fun getItemCount(): Int = properties.size

    fun addProperty() {
        properties.add("" to "")
        notifyItemInserted(properties.size - 1)
    }

    fun getProperties(): Map<String, String> {
        // 确保获取最新的文本内容
        return properties.filter { it.first.isNotBlank() }.toMap()
    }
    
    fun updatePropertiesFromViews(recyclerView: RecyclerView) {
        // 从当前显示的视图中更新属性值
        for (i in 0 until properties.size) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? PropertyViewHolder
            viewHolder?.let {
                val key = it.etPropertyKey.text.toString()
                val value = it.etPropertyValue.text.toString()
                properties[i] = key to value
            }
        }
    }
}