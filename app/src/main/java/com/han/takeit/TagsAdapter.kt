package com.han.takeit

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.han.takeit.databinding.ItemTagBinding
import com.han.takeit.db.Tag

class TagsAdapter(
    private var tags: List<Tag>,
    private var selectedTags: Set<String>,
    private val onTagToggle: (String, Boolean) -> Unit,
    private val onColorClick: (Tag) -> Unit
) : RecyclerView.Adapter<TagsAdapter.TagViewHolder>() {

    fun updateTags(newTags: List<Tag>) {
        tags = newTags
        notifyDataSetChanged()
    }
    
    fun updateSelectedTags(newSelectedTags: Set<String>) {
        selectedTags = newSelectedTags
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tags[position], selectedTags.contains(tags[position].name), onTagToggle, onColorClick)
    }

    override fun getItemCount(): Int = tags.size

    class TagViewHolder(private val binding: ItemTagBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tag: Tag, isSelected: Boolean, onTagToggle: (String, Boolean) -> Unit, onColorClick: (Tag) -> Unit) {
            binding.textTagName.text = tag.name
            binding.checkboxTag.isChecked = isSelected
            
            // 设置标签名称的胶囊样式背景和文本颜色
            val tagDrawable = ContextCompat.getDrawable(binding.root.context, R.drawable.tag_background)?.mutate()
            tagDrawable?.setTint(Color.parseColor(tag.color))
            binding.textTagName.background = tagDrawable
            
            // 根据背景色计算文本颜色
            val textColorString = Tag.getTextColor(tag.color)
            val textColor = Color.parseColor(textColorString)
            binding.textTagName.setTextColor(textColor)
            
            // 设置颜色按钮的背景色
            val drawable = binding.btnTagColor.background as? GradientDrawable
            drawable?.setColor(Color.parseColor(tag.color))
            
            // 颜色按钮点击事件
            binding.btnTagColor.setOnClickListener {
                onColorClick(tag)
            }
            
            binding.root.setOnClickListener {
                val newState = !binding.checkboxTag.isChecked
                binding.checkboxTag.isChecked = newState
                onTagToggle(tag.name, newState)
            }
            
            binding.checkboxTag.setOnCheckedChangeListener { _, isChecked ->
                onTagToggle(tag.name, isChecked)
            }
        }
    }
}