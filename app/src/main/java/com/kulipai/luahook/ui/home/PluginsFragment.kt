package com.kulipai.luahook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.kulipai.luahook.core.base.BaseFragment
import com.kulipai.luahook.databinding.FragmentHomePluginsBinding

class PluginsFragment : BaseFragment<FragmentHomePluginsBinding>() {

    private lateinit var adapter: PluginAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomePluginsBinding {
        return FragmentHomePluginsBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        adapter = PluginAdapter()
        binding.pluginRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.pluginRecycler.adapter = adapter
    }
}
