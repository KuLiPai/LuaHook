package com.kulipai.luahook.feature.home

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.base.BaseFragment
import com.kulipai.luahook.core.pm.PackageUtils.getAppVersionCode
import com.kulipai.luahook.core.pm.PackageUtils.getAppVersionName
import com.kulipai.luahook.core.shell.ShellMode
import com.kulipai.luahook.core.shizuku.ShizukuApi
import com.kulipai.luahook.core.theme.ColorUtils.getDynamicColor
import com.kulipai.luahook.core.xposed.XposedScope
import com.kulipai.luahook.databinding.FragmentHomeHomeBinding
import com.kulipai.luahook.feature.script.editor.global.EditActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment<FragmentHomeHomeBinding>() {


    override fun initView() {
        super.initView()

        var frameworkName = ""

        binding.version.text =
            getAppVersionName(requireContext()) + " (" + getAppVersionCode(requireContext()).toString() + ")"

        XposedScope.withService {
            frameworkName = " + " + it.frameworkName
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppContainer.instance.shellExecutor.mode.collect { newMode ->
                    if (newMode == ShellMode.ROOT) {
                        // 取消显示教程
                        binding.howToActivate.visibility = View.GONE

                        binding.status.text = "Root$frameworkName"
                        binding.card.setCardBackgroundColor(
                            getDynamicColor(
                                requireContext(),
                                R.attr.colorPrimary
                            )
                        )
                        binding.status.setTextColor(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorOnPrimary
                            )
                        )
                        binding.version.setTextColor(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorOnPrimary
                            )
                        )
                        binding.img.setImageResource(com.kulipai.luahook.R.drawable.check_circle_24px)
                        binding.img.setColorFilter(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorOnPrimary
                            )
                        )
                    } else if (newMode == ShellMode.SHIZUKU) {
                        // 取消显示教程
                        binding.howToActivate.visibility = View.GONE

                        binding.status.text = "Shizuku$frameworkName"
                        binding.card.setCardBackgroundColor(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorTertiary
                            )
                        )
                        binding.status.setTextColor(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorOnTertiary
                            )
                        )
                        binding.version.setTextColor(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorOnTertiary
                            )
                        )
                        binding.img.setImageResource(com.kulipai.luahook.R.drawable.shizuku_logo)
                        binding.img.setColorFilter(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorOnTertiary
                            )
                        )
                    } else if (newMode == ShellMode.SHIZUKU_FALLBACK) {
                        // 取消显示教程
                        binding.howToActivate.visibility = View.GONE

                        binding.status.text = "Shizuku Base$frameworkName"
                        binding.card.setCardBackgroundColor(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorTertiary
                            )
                        )
                        binding.status.setTextColor(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorOnTertiary
                            )
                        )
                        binding.version.setTextColor(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorOnTertiary
                            )
                        )
                        binding.img.setImageResource(com.kulipai.luahook.R.drawable.shizuku_logo)
                        binding.img.setColorFilter(
                            getDynamicColor(
                                requireContext(),
                                com.google.android.material.R.attr.colorOnTertiary
                            )
                        )
                    } else {
                        // 显示教程
                        delay(2000)
                        if (AppContainer.instance.shellExecutor.mode.value == ShellMode.NONE) {
                            binding.howToActivate.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        binding.card1.setOnClickListener {
            if (AppContainer.instance.shellExecutor.mode.value != ShellMode.NONE) {
                val intent = Intent(requireActivity(), EditActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    resources.getString(com.kulipai.luahook.R.string.Inactive_modules),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.card.setOnClickListener {
            if (ShizukuApi.isPermissionGranted.value == true && ShizukuApi.isServiceConnected.value == false) {
                ShizukuApi.bindShizuku(requireContext())

            }
        }


    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeHomeBinding {
        return FragmentHomeHomeBinding.inflate(inflater, container, false)

    }
}