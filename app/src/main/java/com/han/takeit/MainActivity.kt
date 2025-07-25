package com.han.takeit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.navigation.NavigationView
import com.han.takeit.databinding.ActivityMainBinding
import com.han.takeit.db.NoteRepository
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var noteRepository: NoteRepository
    private val notesList = mutableListOf<Note>()
    private val allNotesList = mutableListOf<Note>()
    private var isSelectionMode = false
    private var isEnteringSelectionMode = false
    private var isSearchMode = false
    private var currentSearchQuery = ""
    
    companion object {
        private const val PREFS_NAME = "TakeItPrefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
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

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        
        // 初始化数据库
        noteRepository = NoteRepository(this)
        
        // 设置侧边栏
        setupDrawer()
        
        // 设置瀑布流布局
        setupRecyclerView()
        
        // 设置悬浮按钮
        binding.fab.setOnClickListener {
            createNewNote()
        }
        
        // 首次启动时加载示例数据
        loadNotesData()
        
        // 延迟同步DrawerToggle状态，确保汉堡图标正确显示
        binding.root.post {
            drawerToggle.syncState()
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // 更新最大行数设置
        val maxLines = SettingsActivity.getMaxLines(this)
        notesAdapter.updateMaxLines(maxLines)
        
        // 刷新笔记列表
        refreshNotesFromDatabase()
        showAllNotes()
    }
    
    private fun setupDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        
        // 确保显示汉堡菜单图标
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        drawerToggle.isDrawerIndicatorEnabled = true
        
        // 强制同步状态，确保显示正确的汉堡图标
        drawerToggle.syncState()
        
        binding.navView.setNavigationItemSelectedListener(this)
        
        // 启用DrawerLayout的滑动手势
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }
    

    
    private fun setupRecyclerView() {
        val maxLines = SettingsActivity.getMaxLines(this)
        notesAdapter = NotesAdapter(
            notes = notesList,
            noteRepository = noteRepository,
            onNoteClick = { note ->
                // 点击笔记的处理
                editNote(note)
            },
            onNoteLongClick = { note ->
                // 长按进入多选模式
                enterSelectionMode(note)
            },
            onSelectionChanged = { count ->
                // 选择数量变化时更新UI
                updateSelectionUI(count)
            },
            maxLines = maxLines
        )
        
        binding.recyclerViewNotes.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = notesAdapter
        }
    }
    
    private fun createNewNote() {
        val intent = NoteEditActivity.newIntent(this)
        startActivity(intent)
    }
    
    private fun editNote(note: Note) {
        val intent = NoteEditActivity.newIntent(this, note.id)
        startActivity(intent)
    }
    
    private fun loadNotesData() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        
        if (isFirstLaunch) {
            // 首次启动，创建示例笔记
            noteRepository.createSampleNotes()
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        }
        
        // 从数据库加载所有笔记
        refreshNotesFromDatabase()
        
        // 显示所有笔记
        showAllNotes()
    }
    
    private fun refreshNotesFromDatabase() {
        // 清空当前列表
        allNotesList.clear()
        
        // 从数据库加载所有笔记
        allNotesList.addAll(noteRepository.getAllNotes())
    }
    
    private fun showAllNotes() {
        notesList.clear()
        notesList.addAll(allNotesList)
        notesAdapter.notifyDataSetChanged()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        if (isSelectionMode) {
            menuInflater.inflate(R.menu.menu_selection, menu)
        } else {
            menuInflater.inflate(R.menu.menu_main, menu)
            setupSearchView(menu)
        }
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmDialog()
                return true
            }
            R.id.action_search -> {
                // 搜索菜单项点击处理（SearchView会自动处理）
                return true
            }
            android.R.id.home -> {
                if (isSelectionMode) {
                    exitSelectionMode()
                    return true
                }
            }
        }
        
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    

    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            isSelectionMode -> {
                exitSelectionMode()
            }
            isSearchMode -> {
                // 关闭搜索模式
                val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as? SearchView
                searchView?.onActionViewCollapsed()
                isSearchMode = false
                currentSearchQuery = ""
                showAllNotes()
            }
            binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // 如果在搜索模式，先关闭搜索
        if (isSearchMode) {
            val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
            val searchView = searchItem?.actionView as? SearchView
            searchView?.onActionViewCollapsed()
            isSearchMode = false
            currentSearchQuery = ""
        }
        
        when (item.itemId) {
            R.id.nav_all_notes -> {
                // 显示所有笔记
                showAllNotes()
            }
            R.id.nav_today -> {
                // 显示今天的笔记
                filterNotesByDate(0)
            }
            R.id.nav_yesterday -> {
                // 显示昨天的笔记
                filterNotesByDate(1)
            }
            R.id.nav_this_week -> {
                // 显示本周的笔记
                filterNotesByDate(7)
            }
            R.id.nav_this_month -> {
                // 显示本月的笔记
                filterNotesByDate(30)
            }
            R.id.nav_settings -> {
                // 打开设置页面
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_about -> {
                // 显示关于信息
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    private fun filterNotesByDate(daysAgo: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val startTime = calendar.timeInMillis
        
        val filteredNotes = if (daysAgo == 0) {
            // 今天的笔记
            allNotesList.filter { it.timestamp >= endTime }
        } else {
            // 指定天数内的笔记
            allNotesList.filter { it.timestamp >= startTime && it.timestamp < endTime }
        }
        
        notesList.clear()
        notesList.addAll(filteredNotes)
        notesAdapter.notifyDataSetChanged()
    }
    
    private fun enterSelectionMode(note: Note) {
        isSelectionMode = true
        isEnteringSelectionMode = true
        notesAdapter.setSelectionMode(true)
        
        // 自动选中触发长按的笔记
        notesAdapter.toggleSelection(note)
        
        // 更新工具栏
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        
        // 更新选择UI（这会触发菜单刷新）
        val selectedCount = notesAdapter.getSelectedNotes().size
        updateSelectionUI(selectedCount)
        
        // 重置标志
        isEnteringSelectionMode = false
        
        // 禁用侧边栏
        binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }
    
    private fun exitSelectionMode() {
        isSelectionMode = false
        isEnteringSelectionMode = false
        notesAdapter.setSelectionMode(false)
        
        // 恢复工具栏
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(null)
        supportActionBar?.title = getString(R.string.app_name)
        invalidateOptionsMenu()
        
        // 启用侧边栏
        binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
        drawerToggle.syncState()
    }
    
    private fun updateSelectionUI(count: Int) {
        if (isSelectionMode) {
            supportActionBar?.title = getString(R.string.selected_count, count)
            invalidateOptionsMenu() // 刷新菜单以显示删除按钮
            // 只有在不是刚进入选择模式且没有选中任何笔记时才退出选择模式
            if (count == 0 && !isEnteringSelectionMode) {
                exitSelectionMode()
            }
        }
    }
    
    private fun showDeleteConfirmDialog() {
        val selectedNotes = notesAdapter.getSelectedNotes()
        if (selectedNotes.isEmpty()) return
        
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(getString(R.string.delete_confirm_message, selectedNotes.size))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deleteSelectedNotes(selectedNotes)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun deleteSelectedNotes(notesToDelete: List<Note>) {
        // 获取要删除的笔记ID列表
        val noteIdsToDelete = notesToDelete.map { it.id }
        
        // 从数据库中删除笔记
        noteRepository.deleteNotes(noteIdsToDelete)
        
        // 从所有笔记列表中删除
        allNotesList.removeAll(notesToDelete)
        
        // 从当前显示列表中删除
        notesList.removeAll(notesToDelete)
        
        // 退出选择模式
        exitSelectionMode()
        
        // 刷新列表
        notesAdapter.notifyDataSetChanged()
    }
    
    private fun setupSearchView(menu: Menu) {
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        
        searchView?.apply {
            queryHint = getString(R.string.search_hint)
            maxWidth = Integer.MAX_VALUE
            
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    currentSearchQuery = newText ?: ""
                    filterNotesBySearch(currentSearchQuery)
                    return true
                }
            })
            
            setOnSearchClickListener {
                isSearchMode = true
            }
            
            setOnCloseListener {
                isSearchMode = false
                currentSearchQuery = ""
                showAllNotes()
                false
            }
        }
    }
    
    private fun filterNotesBySearch(query: String) {
        if (query.isEmpty()) {
            showAllNotes()
            return
        }
        
        val filteredNotes = allNotesList.filter { note ->
            note.content.contains(query, ignoreCase = true) ||
            note.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        
        notesList.clear()
        notesList.addAll(filteredNotes)
        notesAdapter.notifyDataSetChanged()
    }
}