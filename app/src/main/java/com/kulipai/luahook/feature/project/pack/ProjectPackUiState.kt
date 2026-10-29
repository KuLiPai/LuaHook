package com.kulipai.luahook.feature.project.pack

import com.kulipai.luahook.core.project.Project
import com.kulipai.luahook.feature.project.pack.engine.PackResult
import com.kulipai.luahook.feature.project.pack.engine.PackStage

data class ProjectPackUiState(
    val isLoadingProject: Boolean = true,
    val project: Project? = null,
    val suggestedPackageName: String = "",
    val isPacking: Boolean = false,
    val packStage: PackStage = PackStage.PREPARE,
    val packProgress: Int = 0,
    val packResult: PackResult? = null,
    val errorMessage: String? = null
)

object PackageNameValidator {

    private val PACKAGE_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    fun isValid(packageName: String): Boolean {
        val trimmed = packageName.trim()
        if (trimmed.length < 3 || trimmed.length > 150) return false
        return PACKAGE_REGEX.matches(trimmed)
    }

    fun suggestPackageName(projectName: String): String {
        val asciiSlug = projectName
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "")
            .let {
                if (it.isEmpty()) {
                    "m" + (projectName.hashCode().toLong() and 0xFFFFFFL).toString(16)
                } else if (it.first().isDigit() || it.first() == '_') {
                    "m_$it"
                } else {
                    it
                }
            }
        return "com.luahook.project.$asciiSlug"
    }
}
