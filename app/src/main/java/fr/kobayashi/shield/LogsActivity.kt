package fr.kobayashi.shield

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import fr.kobayashi.shield.databinding.ActivityLogsBinding
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.RuleDatabase
import kotlinx.coroutines.launch

class LogsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLogsBinding
    private lateinit var logAdapter: LogAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupButtons()
        loadLogs()
    }
    
    private fun setupRecyclerView() {
        logAdapter = LogAdapter()
        
        binding.logsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@LogsActivity)
            adapter = logAdapter
        }
    }
    
    private fun setupButtons() {
        binding.btnClearLogs.setOnClickListener {
            clearLogs()
        }
    }
    
    private fun loadLogs() {
        lifecycleScope.launch {
            RuleDatabase.getDatabase().logDao().getAllLogs()
                .collect { logs ->
                    logAdapter.submitList(logs)
                    binding.emptyLogsText.visibility = if (logs.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }
        }
    }
    
    private fun clearLogs() {
        lifecycleScope.launch {
            RuleDatabase.getDatabase().logDao().clearAll()
        }
    }
}
