/*
 * FICHIER : LogAdapter.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/LogAdapter.kt
 * LIGNES : 38 | RÔLE : Adaptateur de la liste de l'historique | DATE : 2026-10-16 | VERSION : 1.7.0
 * ORIGINE : Refonte journal | AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : item_log.xml, JournalVue.kt
 * LIEN : LogsActivity.kt | NOTES : le rendu d'une ligne est dans JournalVue.lier
 */
package fr.kobayashi.shield

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.kobayashi.shield.databinding.ItemLogBinding
import fr.kobayashi.shield.model.LogEntry

class LogAdapter(private val scope: LifecycleCoroutineScope) : ListAdapter<LogEntry, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        JournalVue.lier(holder.binding, getItem(position), scope)
    }

    class LogViewHolder(val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root)

    class LogDiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean = oldItem == newItem
    }
}
