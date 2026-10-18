package com.kulipai.luahook.ui.setting

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.text.InputType
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kulipai.luahook.R
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.language.LanguageUtils
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.data.model.McpInfo
import com.kulipai.luahook.databinding.ActivitySettingsBinding
import com.kulipai.luahook.mcp.McpForegroundService
import com.kulipai.luahook.mcp.McpManager
import com.kulipai.luahook.mcp.McpNetworkAccess
import com.kulipai.luahook.ui.about.AboutActivity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.activity.result.contract.ActivityResultContracts
import java.net.Inet4Address
import java.net.NetworkInterface

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    /**
     * Android 17 起，局域网访问需要在运行时授予 ACCESS_LOCAL_NETWORK；
     * 授权成功后直接把 MCP 打开，失败则提示并回滚开关。
     */
    private val localNetworkPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val mcp = McpManager.getMcpInfo(this)
                if (McpManager.update(true, mcp.port, this)) {
                    McpManager.apply(this)
                }
            } else {
                toast(R.string.mcp_local_network_denied)
            }
            updateMcpView()
        }

    /** 本次页面生命周期内是否已经主动请求过局域网权限，避免反复弹窗。 */
    private var localNetworkRequested = false

    override fun inflateBinding(inflater: LayoutInflater): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(inflater)
    }

    override fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun initEvent() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.language.setOnClickListener {
            showLanguagePickerDialog()
        }

        binding.about.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        binding.mcpCard.setOnClickListener {
            showStatus(McpManager.getMcpInfo(this))
        }

        binding.mcpSettings.setOnClickListener {
            showStatus(McpManager.getMcpInfo(this))
        }

        ShellManager.mode.observe(this) {
            updateMcpView()
        }
    }

    override fun onResume() {
        super.onResume()
        updateMcpView()
        // 从旧版本升级过来时 MCP 已开启但还没拿到局域网权限，进设置页补一次授权请求
        val mcp = McpManager.getMcpInfo(this)
        if (!localNetworkRequested && mcp.enabled && McpNetworkAccess.needsPermission(this)) {
            localNetworkRequested = true
            localNetworkPermission.launch(McpNetworkAccess.PERMISSION)
        }
    }

    private fun updateMcpView() {
        val mcp = McpManager.getMcpInfo(this)
        binding.mcpDesc.text = when {
            !mcp.enabled -> getString(R.string.mcp_status_off)
            McpForegroundService.isListening() -> getString(R.string.mcp_status_on, mcp.port)
            else -> getString(R.string.mcp_status_down, mcp.port)
        }

        binding.mcpSwitch.setOnCheckedChangeListener(null)
        binding.mcpSwitch.isChecked = mcp.enabled
        binding.mcpSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (ShellManager.mode.value == ShellManager.Mode.NONE) {
                toast(R.string.Inactive_modules)
                updateMcpView()
                return@setOnCheckedChangeListener
            }
            // Android 17 起局域网访问需要运行时授权，授权后再真正开启
            if (isChecked && McpNetworkAccess.needsPermission(this)) {
                localNetworkPermission.launch(McpNetworkAccess.PERMISSION)
                return@setOnCheckedChangeListener
            }
            if (McpManager.update(isChecked, mcp.port, this)) {
                McpManager.apply(this)
            }
            updateMcpView()
        }
    }

    private fun showLanguagePickerDialog() {
        val languages = arrayOf(
            resources.getString(R.string.follow_system),
            "Deutsch",
            "English",
            "Español",
            "Français",
            "हिन्दी",
            "日本語",
            "繁體中文",
            "简体中文",
            "한국어",
            "Português"
        )
        val languageCodes = arrayOf(
            LanguageUtils.LANGUAGE_FOLLOW_SYSTEM,
            LanguageUtils.LANGUAGE_GERMAN,
            LanguageUtils.LANGUAGE_ENGLISH,
            LanguageUtils.LANGUAGE_SPANISH,
            LanguageUtils.LANGUAGE_FRENCH,
            LanguageUtils.LANGUAGE_HINDI,
            LanguageUtils.LANGUAGE_JAPANESE,
            LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL,
            LanguageUtils.LANGUAGE_CHINESE_SIMPLIFIED,
            LanguageUtils.LANGUAGE_KOREAN,
            LanguageUtils.LANGUAGE_PORTUGUESE
        )

        var currentLanguage = LanguageUtils.getCurrentLanguage(this)
        if (currentLanguage == "zh") currentLanguage = LanguageUtils.LANGUAGE_CHINESE_SIMPLIFIED

        var checkedItem = languageCodes.indexOf(currentLanguage)
        if (checkedItem == -1) checkedItem = 0

        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.Select_language))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedLanguageCode = languageCodes[which]
                LanguageUtils.changeLanguage(this, selectedLanguageCode)
                recreate()
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /** 点卡片或齿轮：运行状态、本地和局域网的 /mcp 地址，以及接口说明。 */
    private fun showStatus(mcp: McpInfo) {
        val status = when {
            !mcp.enabled -> getString(R.string.mcp_status_off)
            McpForegroundService.isListening() -> getString(R.string.mcp_status_on, mcp.port).substringBefore(" ·")
            else -> getString(R.string.mcp_status_down, mcp.port).substringBefore(" ·")
        }
        val localUrl = "http://127.0.0.1:${mcp.port}/mcp"
        val lanIps = getLanAddresses()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
            addView(label(getString(R.string.mcp_status_line, status)))
            addView(addressRow(getString(R.string.mcp_local), localUrl))
            if (lanIps.isEmpty()) {
                addView(addressRow(getString(R.string.mcp_lan), getString(R.string.mcp_no_lan), null))
            } else {
                for ((desc, ip) in lanIps) {
                    val url = "http://$ip:${mcp.port}/mcp"
                    val title = getString(R.string.mcp_lan_item, getString(R.string.mcp_lan), desc)
                    addView(addressRow(title, url))
                }
            }
            val skillUrl = "http://127.0.0.1:${mcp.port}/skill.md"
            addView(addressRow(getString(R.string.mcp_ai_rules), skillUrl))

            val copyRulesBtn = MaterialButton(this@SettingsActivity).apply {
                text = getString(R.string.mcp_copy_ai_rules)
                setOnClickListener {
                    val rules = com.kulipai.luahook.mcp.McpStore.getFullSkillMarkdown()
                    val clipboard = getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("luahook_ai_rules", rules))
                    toast(R.string.mcp_ai_rules_copied)
                }
            }
            addView(copyRulesBtn)

            addView(label("\n" + getString(R.string.mcp_api_list)))
        }
        val scroll = ScrollView(this).apply { addView(content) }
        MaterialAlertDialogBuilder(this)
            .setTitle("MCP")
            .setView(scroll)
            .setPositiveButton(R.string.mcp_port) { _, _ -> showPortDialog(McpManager.getMcpInfo(this)) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** 一行地址加可见的复制按钮。没有局域网地址时不给复制。 */
    private fun addressRow(title: String, shown: String, copyUrl: String? = shown): LinearLayout {
        val text = label("$title\n$shown").apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 8)
            addView(text)
        }
        if (copyUrl != null) {
            val button = MaterialButton(this).apply {
                this.text = getString(R.string.mcp_copy)
                setOnClickListener { confirmCopy(copyUrl) }
            }
            row.addView(button)
        }
        return row
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextIsSelectable(true)
        }
    }

    /** 先问是否复制，再写入剪贴板。 */
    private fun confirmCopy(url: String) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.mcp_copy_ask) + "\n" + url)
            .setPositiveButton(R.string.sure) { _, _ ->
                val clipboard = this.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("mcp", url))
                toast(R.string.mcp_copied)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** 获取合法的局域网 IPv4 地址列表，过滤掉 VPN（tun/tap）、虚拟网卡等接口。 */
    private fun getLanAddresses(): List<Pair<String, String>> {
        val interfaces = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        // 1. 通过 WifiManager 获取当前连接的 Wi-Fi 客户端 IPv4 地址
        val wifiStationIp = try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                java.lang.String.format(
                    java.util.Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            } else null
        } catch (_: Exception) {
            null
        }

        // 2. 通过 ConnectivityManager 精准识别 VPN 虚拟网卡以及当前作为上行连接的 Wi-Fi 客户端网卡
        val vpnIfaces = mutableSetOf<String>()
        val vpnIps = mutableSetOf<String>()
        val wifiIfaces = mutableSetOf<String>()
        val wifiIps = mutableSetOf<String>()
        if (wifiStationIp != null) {
            wifiIps.add(wifiStationIp)
        }
        try {
            val cm = getSystemService(ConnectivityManager::class.java)
            if (cm != null) {
                for (network in cm.allNetworks) {
                    val caps = cm.getNetworkCapabilities(network) ?: continue
                    val props = cm.getLinkProperties(network)

                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        props?.interfaceName?.let { vpnIfaces.add(it.lowercase()) }
                        props?.linkAddresses?.forEach { linkAddr ->
                            linkAddr.address?.hostAddress?.let { vpnIps.add(it) }
                        }
                    }

                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        props?.interfaceName?.let { wifiIfaces.add(it.lowercase()) }
                        props?.linkAddresses?.forEach { linkAddr ->
                            linkAddr.address?.hostAddress?.let { wifiIps.add(it) }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val result = mutableListOf<Pair<String, String>>()
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            val name = intf.name.lowercase()

            // 过滤 VPN、虚拟网卡、隧道接口及 464XLAT 接口
            if (name in vpnIfaces ||
                name.startsWith("tun") || name.startsWith("tap") || name.startsWith("ppp") ||
                name.startsWith("p2p") || name.startsWith("dummy") || name.startsWith("sit") ||
                name.startsWith("ip6tnl") || name.startsWith("veth") || name.startsWith("docker") ||
                name.startsWith("vgate") || name.startsWith("wg") || name.startsWith("clat") ||
                name.startsWith("v4-") || name.startsWith("ipsec") || name.startsWith("tailscale") ||
                name.startsWith("zt") || name.startsWith("utun") || name.startsWith("virbr") ||
                name.startsWith("secbr")
            ) {
                continue
            }

            for (addr in intf.inetAddresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                    val ip = addr.hostAddress ?: continue
                    if (ip in vpnIps || ip == "192.0.0.4") continue
                    if (result.any { it.second == ip }) continue

                    val isStationWifi = (wifiStationIp != null && ip == wifiStationIp) ||
                            name in wifiIfaces ||
                            ip in wifiIps

                    val labelRes = when {
                        isStationWifi -> R.string.mcp_net_wifi
                        name.startsWith("ap") || name.startsWith("softap") || name.startsWith("swlan") -> R.string.mcp_net_hotspot
                        name.startsWith("wlan") -> R.string.mcp_net_hotspot
                        name.startsWith("eth") -> R.string.mcp_net_ethernet
                        name.startsWith("rndis") || name.startsWith("usb") -> R.string.mcp_net_usb
                        name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("pdp") || name.startsWith("wwan") -> R.string.mcp_net_cellular
                        ip.startsWith("192.168.43.") || ip.endsWith(".1") -> R.string.mcp_net_hotspot
                        else -> null
                    }
                    val label = if (labelRes != null) getString(labelRes) else name
                    result.add(label to ip)
                }
            }
        }
        return result
    }

    private fun showPortDialog(mcp: McpInfo) {
        if (ShellManager.mode.value == ShellManager.Mode.NONE) {
            toast(R.string.Inactive_modules)
            return
        }
        val view = layoutInflater.inflate(R.layout.dialog_edit, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.text_input_layout)
        val edit = view.findViewById<TextInputEditText>(R.id.edit)
        inputLayout.hint = getString(R.string.mcp_port)
        edit.inputType = InputType.TYPE_CLASS_NUMBER
        edit.setText(mcp.port.toString())
        MaterialAlertDialogBuilder(this)
            .setTitle("MCP")
            .setView(view)
            .setPositiveButton(R.string.sure) { _, _ ->
                val port = edit.text?.toString()?.toIntOrNull()
                if (port == null || port !in 1..65535) {
                    toast(R.string.mcp_port_invalid)
                    return@setPositiveButton
                }
                if (McpManager.update(mcp.enabled, port, this)) {
                    McpManager.apply(this)
                    toast(R.string.save_ok)
                    updateMcpView()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toast(res: Int) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
    }
}
