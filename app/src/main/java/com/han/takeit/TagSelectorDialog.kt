package com.han.takeit

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import com.han.takeit.databinding.DialogTagSelectorBinding
import com.han.takeit.db.NoteRepository
import com.han.takeit.db.Tag

class TagSelectorDialog(
    context: Context,
    private val noteRepository: NoteRepository,
    private val currentTags: List<String>,
    private val onTagsSelected: (List<String>) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogTagSelectorBinding
    private lateinit var tagsAdapter: TagsAdapter
    private var allTags = mutableListOf<Tag>()
    private var filteredTags = mutableListOf<Tag>()
    private var selectedTags = mutableSetOf<String>()
    private var selectedNewTagColor = Tag.getRandomColor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogTagSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置对话框属性
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupViews()
        loadTags()
    }

    private fun setupViews() {
        // 初始化选中的标签
        selectedTags.addAll(currentTags)

        // 设置RecyclerView
        tagsAdapter = TagsAdapter(filteredTags, selectedTags, 
            onTagToggle = { tagName, isSelected ->
                if (isSelected) {
                    selectedTags.add(tagName)
                } else {
                    selectedTags.remove(tagName)
                }
            },
            onColorClick = { tag ->
                showColorPicker(tag)
            }
        )
        binding.recyclerTags.layoutManager = LinearLayoutManager(context)
        binding.recyclerTags.adapter = tagsAdapter
        
        // 初始化新标签颜色按钮
        updateNewTagColorButton()

        // 设置输入框监听
        binding.editTagInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                filterTags(query)
                updateAddButton(query)
            }
        })

        // 设置输入框回车监听
        binding.editTagInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                addCurrentTag()
                true
            } else {
                false
            }
        }

        // 设置颜色选择按钮
        binding.btnColorPicker.setOnClickListener {
            showNewTagColorPicker()
        }
        
        // 设置添加按钮
        binding.btnAddTag.setOnClickListener {
            addCurrentTag()
        }

        // 设置取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // 设置确定按钮
        binding.btnConfirm.setOnClickListener {
            onTagsSelected(selectedTags.toList())
            dismiss()
        }
    }

    private fun loadTags() {
        allTags.clear()
        allTags.addAll(noteRepository.getAllTagObjects())
        filterTags("")
    }

    private fun filterTags(query: String) {
        filteredTags.clear()
        if (query.isEmpty()) {
            filteredTags.addAll(allTags)
        } else {
            filteredTags.addAll(allTags.filter { it.name.contains(query, ignoreCase = true) })
        }
        tagsAdapter.updateTags(filteredTags)
    }

    private fun updateAddButton(query: String) {
        val hasExactMatch = allTags.any { it.name.equals(query, ignoreCase = true) }
        if (hasExactMatch) {
            binding.btnAddTag.setImageResource(R.drawable.ic_check)
        } else {
            binding.btnAddTag.setImageResource(R.drawable.ic_add)
        }
        binding.btnAddTag.isEnabled = query.isNotEmpty()
    }

    private fun addCurrentTag() {
        val tagName = binding.editTagInput.text.toString().trim()
        if (tagName.isNotEmpty() && !allTags.any { it.name == tagName }) {
            // 如果是新标签，插入到数据库
            noteRepository.insertTagWithColor(tagName, selectedNewTagColor)
            selectedTags.add(tagName)
            binding.editTagInput.text?.clear()
            selectedNewTagColor = Tag.getRandomColor() // 重置为随机颜色
            updateNewTagColorButton()
            loadTags() // 重新加载标签列表
        } else if (tagName.isNotEmpty()) {
            // 如果标签已存在，直接选中
            selectedTags.add(tagName)
            binding.editTagInput.text?.clear()
        }
        
        // 刷新列表
        filterTags("")
        tagsAdapter.updateSelectedTags(selectedTags)
    }
    
    private fun showColorPicker(tag: Tag) {
        val colorPicker = ColorPickerDialog(context) { selectedColor ->
            // 更新标签颜色
            noteRepository.updateTagColor(tag.name, selectedColor)
            loadTags() // 重新加载以更新显示
        }
        colorPicker.show()
    }
    
    private fun showNewTagColorPicker() {
        val colorPicker = ColorPickerDialog(context) { selectedColor ->
            selectedNewTagColor = selectedColor
            updateNewTagColorButton()
        }
        colorPicker.show()
    }
    
    private fun updateNewTagColorButton() {
        val drawable = binding.btnColorPicker.background as? GradientDrawable
        drawable?.setColor(Color.parseColor(selectedNewTagColor))
    }
}