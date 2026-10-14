package fr.kobayashi.shield

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.kobayashi.shield.databinding.ItemRuleBinding
import fr.kobayashi.shield.model.Rule

class RuleAdapter(
    private val onEdit: (Rule) -> Unit,
    private val onDelete: (Rule) -> Unit
) : ListAdapter<Rule, RuleAdapter.RuleViewHolder>(RuleDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return RuleViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class RuleViewHolder(private val binding: ItemRuleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rule: Rule) {
            binding.tvPattern.text = rule.pattern
            binding.tvDescription.text = rule.description ?: "Aucune description"
            binding.tvType.text = rule.ruleType.name
            binding.tvAction.text = rule.action.name
            binding.tvActive.text = if (rule.isActive) "Actif" else "Inactif"
            binding.tvActive.setTextColor(
                if (rule.isActive) android.graphics.Color.GREEN else android.graphics.Color.RED
            )
            
            binding.btnEdit.setOnClickListener { onEdit(rule) }
            binding.btnDelete.setOnClickListener { onDelete(rule) }
        }
    }
    
    class RuleDiffCallback : DiffUtil.ItemCallback<Rule>() {
        override fun areItemsTheSame(oldItem: Rule, newItem: Rule): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Rule, newItem: Rule): Boolean {
            return oldItem == newItem
        }
    }
}
