package com.vanta.app.service

import android.accessibilityservice.AccessibilityService
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
        Log.d("VantaService", "Accessibility Service Connected")
        database = AppDatabase.getDatabase(this)
        workflowExecutor = WorkflowExecutor(this)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // We only care about hardware keys, mostly Volume buttons for this prototype.
        // Power button is often swallowed by OS before getting here, 
        // Bixby/Assistant can often be caught depending on OS version.
        
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            Log.d("VantaService", "Key pressed: $keyCode")
            
            // Let's check if this key is bound
            var handled = false
            
            // We use runBlocking here to quickly check if a binding exists so we can return true/false 
            // synchronously to consume the event. In production, we'd cache bindings in memory 
            // rather than hitting the DB synchronously on the UI thread for every key press.
            runBlocking {
                val binding = withContext(Dispatchers.IO) {
                    database.vantaDao().getBindingForButton(keyCode, 0) // 0 = Single press
                }
                
                if (binding != null) {
                    handled = true
                    Log.d("VantaService", "Executing workflow: ${binding.workflowId}")
                    // Execute the workflow async
                    serviceScope.launch {
                        val steps = withContext(Dispatchers.IO) {
                            database.vantaDao().getStepsForWorkflow(binding.workflowId)
                        }
                        workflowExecutor.executeSteps(steps)
                    }
                }
            }
            
            if (handled) return true // Consume the event so default action (like Volume UI) doesn't show
        }
        
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for button interception, but required to override
    }

    override fun onInterrupt() {
        Log.d("VantaService", "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
