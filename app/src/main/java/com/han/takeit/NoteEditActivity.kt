package com.han.takeit

import android.widget.DatePicker
import android.widget.TimePicker
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import com.han.takeit.db.Tag
import android.graphics.Color
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.han.takeit.databinding.ActivityNoteEditBinding
import com.han.takeit.db.NoteRepository
import java.text.SimpleDateFormat
import java.util.*

class NoteEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditBinding
    private lateinit var noteRepository: NoteRepository
    private var noteId: Long = -1
    private var isNewNote = true
    private var currentNote: Note? = null
    private val autoSaveHandler = Handler(Looper.getMainLooper())
    private var autoSaveRunnable: Runnable? = null
    private val autoSaveDelay = 1000L // 1秒延迟自动保存

    companion object {
        private const val EXTRA_NOTE_ID = "extra_note_id"
        
        fun newIntent(context: Context, noteId: Long = -1): Intent {
            return Intent(context, NoteEditActivity::class.java).apply {
                if (noteId != -1L) {
                    putExtra(EXTRA_NOTE_ID, noteId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用语言设置
        val savedLanguage = SettingsActivity.getSavedLanguage(this)
        if (savedLanguage == SettingsActivity.LANGUAGE_SYSTEM) {
            // 跟随系统语言，获取真正的系统首选语言
            val systemLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.os.LocaleList.getDefault().get(0)
            } else {
                java.util.Locale.getDefault()
            }
            val config = android.content.res.Configuration(resources.configuration)
            config.setLocale(systemLocale)
            resources.updateConfiguration(config, resources.displayMetrics)
        } else {
            SettingsActivity.updateLanguage(this, savedLanguage)
        }
        
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示，让内容延伸到状态栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化数据库
        noteRepository = NoteRepository(this)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)
        isNewNote = noteId == -1L
        
        if (isNewNote) {
            title = getString(R.string.new_note)
        } else {
            title = getString(R.string.edit_note)
            loadNote()
        }
        
        // 自动聚焦到内容输入框
        binding.editContent.requestFocus()
        
        // 设置自动保存监听器
        setupAutoSave()
        
        // 设置标签按钮点击事件
        binding.fabTags.setOnClickListener {
            showTagSelector()
        }
        
        // 初始化标签显示
        updateTagsDisplay()
    }
    
    private fun setupAutoSave() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // 取消之前的自动保存任务
                autoSaveRunnable?.let { autoSaveHandler.removeCallbacks(it) }
                
                // 创建新的自动保存任务
                autoSaveRunnable = Runnable {
                    autoSaveNote()
                }
                
                // 延迟执行自动保存
                autoSaveHandler.postDelayed(autoSaveRunnable!!, autoSaveDelay)
            }
        }
        
        // 为内容输入框添加文本变化监听器
        binding.editContent.addTextChangedListener(textWatcher)
    }
    
    private fun autoSaveNote() {
        val content = binding.editContent.text.toString().trim()
        
        // 如果内容为空，不保存
        if (content.isEmpty()) {
            return
        }
        
        // 创建或更新笔记对象
        val note = if (isNewNote) {
            // 新笔记，生成ID并标记为非新笔记
            val currentTime = System.currentTimeMillis()
            val newNote = Note(
                id = noteRepository.generateNoteId(),
                content = content,
                createdTime = currentTime,
                modifiedTime = currentTime,
                tags = currentNote?.tags ?: emptyList(),
                customProperties = currentNote?.customProperties ?: emptyMap()
            )
            // 更新状态
            noteId = newNote.id
            isNewNote = false
            currentNote = newNote
            newNote
        } else {
            // 更新现有笔记，只有内容改变时才更新修改时间
            val existingNote = currentNote
            val updatedNote = if (existingNote != null) {
                val contentChanged = existingNote.content != content
                existingNote.copy(
                    content = content,
                    modifiedTime = if (contentChanged) System.currentTimeMillis() else existingNote.modifiedTime
                )
            } else {
                val currentTime = System.currentTimeMillis()
                Note(
                    id = noteId,
                    content = content,
                    createdTime = currentTime,
                    modifiedTime = currentTime,
                    tags = emptyList(),
                    customProperties = emptyMap()
                )
            }
            currentNote = updatedNote
            updatedNote
        }
        
        // 保存到数据库
        noteRepository.saveNote(note)
    }
    
    private fun loadNote() {
        // 从数据库加载笔记
        currentNote = noteRepository.getNoteById(noteId)
        
        currentNote?.let { note ->
            binding.editContent.setText(note.content)
            updateTagsDisplay()
        } ?: run {
            // 如果笔记不存在，则关闭页面
            finish()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_edit, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_properties -> {
                showNotePropertiesDialog()
                true
            }
            R.id.action_delete -> {
                deleteNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showNotePropertiesDialog() {
        // 如果是新笔记，先创建一个临时笔记对象
        val note = if (isNewNote || currentNote == null) {
            val content = binding.editContent.text.toString().trim()
            val currentTime = System.currentTimeMillis()
            Note(
                id = if (isNewNote) noteRepository.generateNoteId() else noteId,
                content = content,
                createdTime = currentTime,
                modifiedTime = currentTime,
                tags = emptyList(),
                customProperties = emptyMap()
            )
        } else {
            currentNote!!
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_note_properties, null)
        
        // 设置创建时间和修改时间
        val etCreatedTime = dialogView.findViewById<TextInputEditText>(R.id.etCreatedTime)
        val etModifiedTime = dialogView.findViewById<TextInputEditText>(R.id.etModifiedTime)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        etCreatedTime.setText(dateFormat.format(Date(note.createdTime)))
        etModifiedTime.setText(dateFormat.format(Date(note.modifiedTime)))
        
        // 设置自定义属性列表
        val rvCustomProperties = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCustomProperties)
        val properties = note.customProperties.map { it.key to it.value }.toMutableList()
        val adapter = CustomPropertyAdapter(properties) {}
        
        rvCustomProperties.layoutManager = LinearLayoutManager(this)
        rvCustomProperties.adapter = adapter
        
        // 添加属性按钮
        val btnAddProperty = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddProperty)
        btnAddProperty.setOnClickListener {
            adapter.addProperty()
        }
        
        // 创建时间点击事件
        etCreatedTime.setOnClickListener {
            showDateTimePicker { selectedTimestamp ->
                etCreatedTime.setText(dateFormat.format(Date(selectedTimestamp)))
            }
        }
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        
        // 取消按钮
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // 保存按钮
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)
        btnSave.setOnClickListener {
            try {
                // 解析新的创建时间
                val newCreatedTime = dateFormat.parse(etCreatedTime.text.toString())?.time ?: note.createdTime
                
                // 确保获取最新的输入值
                adapter.updatePropertiesFromViews(rvCustomProperties)
                // 获取自定义属性
                val newCustomProperties = adapter.getProperties()
                
                // 更新笔记
                val updatedNote = note.copy(
                    content = binding.editContent.text.toString().trim(),
                    createdTime = newCreatedTime,
                    customProperties = newCustomProperties
                )
                
                // 如果是新笔记，更新状态
                if (isNewNote) {
                    noteId = updatedNote.id
                    isNewNote = false
                }
                
                currentNote = updatedNote
                noteRepository.saveNote(updatedNote)
                
                dialog.dismiss()
            } catch (e: Exception) {
                // 时间格式错误，显示提示
                etCreatedTime.error = "时间格式错误"
            }
        }
        
        dialog.show()
    }
    
    private fun showDateTimePicker(onDateTimeSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_datetime_picker, null)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        
        // 设置当前日期和时间
        datePicker.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            null
        )
        timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
        timePicker.minute = calendar.get(Calendar.MINUTE)
        
        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                // 获取选择的日期和时间
                val newCalendar = Calendar.getInstance()
                newCalendar.set(
                    datePicker.year,
                    datePicker.month,
                    datePicker.dayOfMonth,
                    timePicker.hour,
                    timePicker.minute,
                    0
                )
                onDateTimeSelected(newCalendar.timeInMillis)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun deleteNote() {
        if (isNewNote) {
            finish()
            return
        }
        
        // 显示确认对话框
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(getString(R.string.delete_confirm_message, 1))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                // 从数据库删除笔记
                noteRepository.deleteNote(noteId)
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showTagSelector() {
        // 确保有当前笔记对象
        if (currentNote == null && !isNewNote) {
            loadNote()
        }
        
        val currentTags = currentNote?.tags ?: emptyList()
        val dialog = TagSelectorDialog(this, noteRepository, currentTags) { selectedTags ->
            // 更新当前笔记的标签
            currentNote = if (isNewNote) {
                // 如果是新笔记，先创建笔记对象
                val content = binding.editContent.text.toString().trim()
                if (content.isNotEmpty()) {
                    val currentTime = System.currentTimeMillis()
                    val newNote = Note(
                        id = noteRepository.generateNoteId(),
                        content = content,
                        createdTime = currentTime,
                        modifiedTime = currentTime,
                        tags = selectedTags,
                        customProperties = emptyMap()
                    )
                    noteId = newNote.id
                    isNewNote = false
                    newNote
                } else {
                    val currentTime = System.currentTimeMillis()
                    Note(
                        id = -1,
                        content = "",
                        createdTime = currentTime,
                        modifiedTime = currentTime,
                        tags = selectedTags,
                        customProperties = emptyMap()
                    )
                }
            } else {
                currentNote?.copy(tags = selectedTags)
            }
            
            // 如果笔记有内容，立即保存
            currentNote?.let { note ->
                if (note.content.isNotEmpty() && note.id != -1L) {
                    noteRepository.saveNote(note)
                }
            }
            
            // 更新标签显示
            updateTagsDisplay()
        }
        dialog.show()
    }
    
    private fun updateTagsDisplay() {
        val tagsContainer = binding.tagsContainer
        tagsContainer.removeAllViews()
        
        // 获取Tag对象列表而不是字符串列表
        val tagObjects = noteRepository.getAllTagObjects()
        val currentTagNames = currentNote?.tags ?: emptyList()
        
        // 只显示当前笔记包含的标签
        val currentTagObjects = tagObjects.filter { tag -> 
            currentTagNames.contains(tag.name) 
        }
        
        currentTagObjects.forEach { tag ->
            val textView = TextView(this).apply {
                text = tag.name
                
                // 创建带颜色的背景drawable
                val drawable = ContextCompat.getDrawable(this@NoteEditActivity, R.drawable.tag_background)?.mutate()
                drawable?.setTint(Color.parseColor(tag.color))
                background = drawable
                
                // 根据背景色计算文本颜色
                val textColorString = Tag.getTextColor(tag.color)
                val textColor = Color.parseColor(textColorString)
                setTextColor(textColor)
                
                textSize = 14f
                setPadding(24, 14, 24, 14) // 与首页卡片标签保持一致的内边距
                
                val layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 16, 8) // 与首页卡片标签保持一致的外边距
                }
                this.layoutParams = layoutParams
            }
            tagsContainer.addView(textView)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 在Activity暂停时立即保存笔记，确保用户输入不丢失
        autoSaveRunnable?.let { autoSaveHandler.removeCallbacks(it) }
        
        val content = binding.editContent.text.toString().trim()
        
        if (content.isEmpty()) {
            // 如果没有内容，删除笔记（如果已保存）
            if (!isNewNote && noteId != -1L) {
                noteRepository.deleteNote(noteId)
            }
            // 不保存空笔记
        } else {
            // 有内容时才保存
            autoSaveNote()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 清理自动保存Handler，防止内存泄漏
        autoSaveRunnable?.let { autoSaveHandler.removeCallbacks(it) }
    }
}