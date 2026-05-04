package com.vanta.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanta.app.data.AppDatabase
import com.vanta.app.data.models.ButtonBinding
import com.vanta.app.data.models.Workflow
import com.vanta.app.data.models.WorkflowStep
import com.vanta.app.ui.theme.VantaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)

        setContent {
            VantaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    val workflows by database.vantaDao().getAllWorkflows().collectAsState(initial = emptyList())
                    val bindings by database.vantaDao().getAllBindings().collectAsState(initial = emptyList())

                    DashboardScreen(
                        workflows = workflows,
                        bindings = bindings,
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onSeedData = {
                            scope.launch(Dispatchers.IO) {
                                val dao = database.vantaDao()
                                if (dao.getBindingForButton(KeyEvent.KEYCODE_VOLUME_UP, 0) == null) {
                                    // 1. Create Workflow
                                    val wfId = dao.insertWorkflow(Workflow(name = "Volume Up Demo"))
                                    
                                    // 2. Add Steps
                                    dao.insertWorkflowStep(WorkflowStep(workflowId = wfId, orderIndex = 0, actionType = "SET_VOLUME", actionData = "70"))
                                    dao.insertWorkflowStep(WorkflowStep(workflowId = wfId, orderIndex = 1, actionType = "DELAY", actionData = "1000"))
                                    // Just toggling settings panel to show it works natively
                                    dao.insertWorkflowStep(WorkflowStep(workflowId = wfId, orderIndex = 2, actionType = "TOGGLE_WIFI", actionData = "")) 
                                    
                                    // 3. Bind to Volume Up
                                    dao.insertBinding(ButtonBinding(keyCode = KeyEvent.KEYCODE_VOLUME_UP, actionType = 0, workflowId = wfId))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    workflows: List<Workflow>,
    bindings: List<ButtonBinding>,
    onOpenAccessibilitySettings: () -> Unit,
    onSeedData: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "VANTA",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Setup", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onOpenAccessibilitySettings,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Accessibility Service", color = MaterialTheme.colorScheme.onSecondary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSeedData,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Demo Workflow (Vol Up)", color = MaterialTheme.colorScheme.onTertiary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Active Bindings",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        if (bindings.isEmpty()) {
            Text("No bindings found. Click 'Create Demo Workflow'.", color = MaterialTheme.colorScheme.tertiary)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bindings) { binding ->
                    BindingRow(binding, workflows.find { it.id == binding.workflowId })
                }
            }
        }
    }
}

@Composable
fun BindingRow(binding: ButtonBinding, workflow: Workflow?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Key Code: ${binding.keyCode}", 
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Workflow: ${workflow?.name ?: "Unknown"}", 
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
