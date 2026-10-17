package com.kulipai.luahook.ui.home

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kulipai.luahook.core.file.WorkspaceEventManager
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.project.ProjectManager
import com.kulipai.luahook.core.base.BaseFragment
import com.kulipai.luahook.databinding.FragmentHomeProjectBinding
import com.kulipai.luahook.ui.project.create.CreateProjectActivity
import com.kulipai.luahook.ui.project.editor.ProjectEditorActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectFragment: BaseFragment<FragmentHomeProjectBinding>() {
    
    private lateinit var adapter: ProjectAdapter
    private var isFabOpen = false

    private val pickFileLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val success = ProjectManager.importProject(requireContext(), it)
                if (success) {
                    Toast.makeText(requireContext(), getString(com.kulipai.luahook.R.string.msg_import_success), Toast.LENGTH_SHORT).show()
                    loadProjects()
                } else {
                    Toast.makeText(requireContext(), getString(com.kulipai.luahook.R.string.msg_import_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeProjectBinding {
        return FragmentHomeProjectBinding.inflate(inflater, container, false)
    }
    
    override fun initView() {
        adapter = ProjectAdapter(
            mutableListOf(),
            onProjectClick = { project ->
                 val intent = Intent(requireContext(), ProjectEditorActivity::class.java)
                 intent.putExtra("project_name", project.name)
                 startActivity(intent)
            },
            onProjectToggle = { project, isEnabled ->
                ProjectManager.setProjectEnabled(project.name, isEnabled)
            },
            onProjectLongClick = { project ->
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(com.kulipai.luahook.R.string.title_delete_project))
                    .setMessage(getString(com.kulipai.luahook.R.string.msg_confirm_delete_project, project.name))
                    .setPositiveButton(getString(com.kulipai.luahook.R.string.action_delete)) { _, _ ->
                        try {
                            ProjectManager.deleteProject(project.name)
                            loadProjects()
                             Toast.makeText(requireContext(), getString(com.kulipai.luahook.R.string.msg_deleted_project, project.name), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), getString(com.kulipai.luahook.R.string.msg_delete_failed, e.message), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(getString(com.kulipai.luahook.R.string.cancel), null)
                    .show()
            }
        )
        binding.projectRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.projectRecyclerView.adapter = adapter
    }
    
    override fun initEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WorkspaceEventManager.fileEvents.collect { relativePath ->
                    if (relativePath == ProjectManager.INFO_JSON || relativePath.startsWith("${WorkspaceFileManager.Project}/")) {
                        loadProjects()
                    }
                }
            }
        }

        binding.fab.setOnClickListener {
            toggleFab()
        }

        binding.fabCreateProject.setOnClickListener {
            toggleFab()
            val intent = Intent(requireContext(), CreateProjectActivity::class.java)
            startActivity(intent)
        }

        binding.fabImportProject.setOnClickListener {
            toggleFab()
            pickFileLauncher.launch(arrayOf("*/*"))
        }
    }
    
    private fun toggleFab() {
        val rotateAnimator = ObjectAnimator.ofFloat(
            binding.fab,
            "rotation",
            binding.fab.rotation,
            binding.fab.rotation + 45f
        )
        rotateAnimator.duration = 300
        rotateAnimator.start()

        if (isFabOpen) {
            hideFabWithAnimation(binding.fabCreateProject)
            hideFabWithAnimation(binding.fabImportProject, 300)
        } else {
            showFabWithAnimation(binding.fabImportProject)
            showFabWithAnimation(binding.fabCreateProject, 350)
        }
        isFabOpen = !isFabOpen
    }

    private fun showFabWithAnimation(fab: View, time: Long = 300) {
        if (fab.isVisible && fab.alpha == 1f && fab.scaleX == 1f) return

        fab.alpha = 0f
        fab.scaleX = 0f
        fab.scaleY = 0f
        fab.visibility = View.VISIBLE

        val scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 0f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 0f, 1f)
        val alphaAnimator = ObjectAnimator.ofFloat(fab, "alpha", 0f, 1f)

        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
            duration = time
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
    }

    private fun hideFabWithAnimation(fab: View, time: Long = 250) {
        if (fab.isInvisible && fab.alpha == 0f && fab.scaleX == 0f) return

        val scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 1f, 0f)
        val scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 1f, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(fab, "alpha", 1f, 0f)

        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
            duration = time
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    fab.visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }.start()
    }

    override fun onResume() {
        super.onResume()
        loadProjects()
    }

    private fun loadProjects() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val projects = ProjectManager.getProjects()
            withContext(Dispatchers.Main) {
                adapter.updateData(projects)
            }
        }
    }
}
