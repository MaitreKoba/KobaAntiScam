package fr.kobayashi.shield

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.kobayashi.shield.databinding.ItemLogBinding
import fr.kobayashi.shield.model.LogEntry

class LogAdapter : ListAdapter<LogEntry, LogAdapter.LogViewHolder>(LogDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class LogViewHolder(private val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(log: LogEntry) {
            binding.tvTimestamp.text = log.formattedTimestamp()
            binding.tvType.text = log.type.name
            binding.tvMessage.text = log.displayText()
            
            // Couleur selon le type
            val color = when (log.type) {
                fr.kobayashi.shield.model.LogType.CALL_BLOCKED -> android.graphics.Color.RED
                fr.kobayashi.shield.model.LogType.SMS_BLOCKED -> android.graphics.Color.RED
                fr.kobayashi.shield.model.LogType.ERROR -> android.graphics.Color.RED
                fr.kobayashi.shield.model.LogType.CALL_SILENCED -> android.graphics.Color.parseColor("#FF9800")
                else -> android.graphics.Color.parseColor("#4CAF50")
            }
            binding.tvType.setTextColor(color)
        }
    }
    
    class LogDiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem == newItem
        }
    }
}
