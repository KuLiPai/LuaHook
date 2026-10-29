package com.kulipai.luahook.feature.home

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kulipai.luahook.R
import com.kulipai.luahook.core.base.BaseFragment
import com.kulipai.luahook.databinding.FragmentHomeProjectBinding
import com.kulipai.luahook.feature.project.ProjectViewModel
import com.kulipai.luahook.feature.project.create.CreateProjectActivity
import com.kulipai.luahook.feature.project.editor.ProjectEditorActivity
import com.kulipai.luahook.feature.project.pack.ProjectPackActivity
import kotlinx.coroutines.launch

class ProjectFragment : BaseFragment<FragmentHomeProjectBinding>() {

    private val viewModel: ProjectViewModel by viewModels()
    private lateinit var adapter: ProjectAdapter
    private var isFabOpen = false
    private var emptyTextView: TextView? = null

    private val pickFileLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                viewModel.importProject(requireContext(), it) { success ->
                    if (success) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_import_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_import_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                viewModel.setProjectEnabled(project.name, isEnabled)
            },
            onProjectLongClick = { project ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.title_delete_project))
                    .setMessage(getString(R.string.msg_confirm_delete_project, project.name))
                    .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                        viewModel.deleteProject(project.name) { success ->
                            if (success) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.msg_deleted_project, project.name),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.msg_delete_failed, ""),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .setNeutralButton(getString(R.string.action_pack_apk)) { _, _ ->
                        val intent = Intent(requireContext(), ProjectPackActivity::class.java)
                        intent.putExtra("project_name", project.name)
                        startActivity(intent)
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        )
        binding.projectRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.projectRecyclerView.adapter = adapter
    }

    override fun initEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.updateData(state.projects)
                    updateEmptyState(state.projects.isEmpty() && !state.isLoading)
                    state.errorMessage?.let { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                        viewModel.clearErrorMessage()
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

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            if (emptyTextView == null) {
                emptyTextView = TextView(requireContext()).apply {
                    text = "暂无项目\n点击右下角按钮创建或导入项目"
                    textSize = 15f
                    gravity = Gravity.CENTER
                    val typedValue = TypedValue()
                    context.theme.resolveAttribute(
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        typedValue,
                        true
                    )
                    setTextColor(typedValue.data)
                    val params = CoordinatorLayout.LayoutParams(
                        CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                        CoordinatorLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                    binding.root.addView(this, params)
                }
            }
            emptyTextView?.isVisible = true
        } else {
            emptyTextView?.isVisible = false
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
        viewModel.loadProjects()
    }
}
