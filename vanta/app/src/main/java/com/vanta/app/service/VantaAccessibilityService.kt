package com.vanta.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.vanta.app.data.AppDatabase
import com.vanta.app.engine.WorkflowExecutor
import kotlinx.coroutines.*

class VantaAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var database: AppDatabase
    private lateinit var workflowExecutor: WorkflowExecutor

    override fun onServiceConnected() {
        super.onServiceConnected()
        database = AppDatabase.getDatabase(this)
        workflowExecutor = WorkflowExecutor(this)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            
            // Invia il tasto all'interfaccia utente se è aperta per il "binding"
            val broadcastIntent = Intent("com.vanta.app.KEY_PRESSED")
            broadcastIntent.putExtra("KEYCODE", keyCode)
            sendBroadcast(broadcastIntent)
            
            var handled = false
            
            runBlocking {
                val binding = withContext(Dispatchers.IO) {
                    database.vantaDao().getBindingForButton(keyCode, 0)
                }
                
                if (binding != null) {
                    handled = true
                    serviceScope.launch {
                        val steps = withContext(Dispatchers.IO) {
                            database.vantaDao().getStepsForWorkflow(binding.workflowId)
                        }
                        workflowExecutor.executeSteps(steps)
                    }
                }
            }
            
            if (handled) return true 
        }
        
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
