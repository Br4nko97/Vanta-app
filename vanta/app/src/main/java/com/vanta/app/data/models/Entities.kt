package com.vanta.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "button_bindings")
data class ButtonBinding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyCode: Int,          // e.g., KeyEvent.KEYCODE_VOLUME_UP
    val actionType: Int,       // 0 = Single Press, 1 = Long Press, 2 = Double Press
    val workflowId: Long       // ID of the workflow to execute
)

@Entity(tableName = "workflows")
data class Workflow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "workflow_steps")
data class WorkflowStep(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workflowId: Long,
    val orderIndex: Int,
    val actionType: String,    // "LAUNCH_APP", "TOGGLE_WIFI", "DELAY", "SET_VOLUME"
    val actionData: String     // JSON or String data for the action (e.g. package name, delay in ms)
)
