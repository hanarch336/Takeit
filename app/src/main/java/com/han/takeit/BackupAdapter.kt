package com.han.takeit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.han.takeit.databinding.ItemBackupBinding
import com.han.takeit.db.BackupInfo

class BackupAdapter(
    private val backups: List<BackupInfo>,
    private val onDeleteClick: (BackupInfo) -> Unit,
    private val onRestoreClick: (BackupInfo) -> Unit,
    private val onMergeClick: (BackupInfo) -> Unit,
    private val onSaveClick: (BackupInfo) -> Unit
) : RecyclerView.Adapter<BackupAdapter.BackupViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val binding = ItemBackupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BackupViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        holder.bind(backups[position])
    }
    
    override fun getItemCount(): Int = backups.size
    
    inner class BackupViewHolder(private val binding: ItemBackupBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(backup: BackupInfo) {
            binding.apply {
                // 从文件名中提取显示名称
                val fileName = backup.fileName
                val displayName = fileName
                    .removePrefix("notes_backup_")
                    .removeSuffix(".db")
                
                tvBackupName.text = displayName
                tvBackupDate.text = backup.getFormattedDate()
                tvBackupSize.text = backup.getFormattedSize()
                
                btnDeleteBackup.setOnClickListener {
                    onDeleteClick(backup)
                }
                
                btnRestoreBackup.setOnClickListener {
                    onRestoreClick(backup)
                }
                
                btnMergeBackup.setOnClickListener {
                    onMergeClick(backup)
                }
                
                btnSaveBackup.setOnClickListener {
                    onSaveClick(backup)
                }
            }
        }
    }
}