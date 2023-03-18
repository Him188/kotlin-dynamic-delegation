package me.him188.kotlin.dynamic.delegation.idea

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules

class DynamicDelegationProjectImportListener : Disposable, ExternalSystemTaskNotificationListenerAdapter() {
    override fun dispose() {
    }


    override fun onEnd(id: ExternalSystemTaskId) {
        if (id.type == ExternalSystemTaskType.RESOLVE_PROJECT) {
            // At this point changes might be still not applied to project structure yet.
            val project = id.findResolvedProject() ?: return
            BackgroundTaskUtil.runUnderDisposeAwareIndicator(this) {
                for (module in project.modules.asList()) {
                    module.getServiceIfCreated(DynamicDelegationModuleCacheService::class.java)?.initialized = false
                }
            }
        }
    }
}

internal fun ExternalSystemTaskId.findResolvedProject(): Project? {
    if (type != ExternalSystemTaskType.RESOLVE_PROJECT) return null
    return findProject()
}
