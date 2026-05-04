package com.vanta.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.vanta.app.data.models.ButtonBinding
import com.vanta.app.data.models.Workflow
import com.vanta.app.data.models.WorkflowStep
import kotlinx.coroutines.flow.Flow

@Dao
interface VantaDao {
    
    @Query("SELECT * FROM button_bindings WHERE keyCode = :keyCode AND actionType = :actionType LIMIT 1")
    suspend fun getBindingForButton(keyCode: Int, actionType: Int): ButtonBinding?

    @Query("SELECT * FROM workflow_steps WHERE workflowId = :workflowId ORDER BY orderIndex ASC")
    suspend fun getStepsForWorkflow(workflowId: Long): List<WorkflowStep>

    @Insert
    suspend fun insertBinding(binding: ButtonBinding): Long

    @Insert
    suspend fun insertWorkflow(workflow: Workflow): Long

    @Insert
    suspend fun insertWorkflowStep(step: WorkflowStep): Long

    @Query("SELECT * FROM workflows")
    fun getAllWorkflows(): Flow<List<Workflow>>
    
    @Query("SELECT * FROM button_bindings")
    fun getAllBindings(): Flow<List<ButtonBinding>>
}
