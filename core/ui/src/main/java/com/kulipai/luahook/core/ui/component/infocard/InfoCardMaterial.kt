package com.kulipai.luahook.core.ui.component.infocard


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kulipai.luahook.core.ui.R
import com.kulipai.luahook.core.ui.component.material.TonalCard



@Composable
fun InfoCardMaterial(/*systemInfo: SystemInfo = rememberSystemInfo()*/) {
    TonalCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            @Composable
            fun InfoCardItem(label: String, content: String) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }


            // TODO)) 获取真实信息
            InfoCardItem(
                stringResource(R.string.home_kernel),
                content = "6.1.111111-android18-kulipai-mi"
            )

            Spacer(Modifier.height(16.dp))
            InfoCardItem(
                stringResource(R.string.home_manager_version),
                content = "v10086 (10012398)"
            )

            Spacer(Modifier.height(16.dp))
            InfoCardItem(
                stringResource(R.string.home_fingerprint),
                content = "XiaoMi/NB/666"
            )

            Spacer(Modifier.height(16.dp))
            InfoCardItem(
                stringResource(R.string.home_selinux_status),
                content = "强制执行",
            )
        }
    }
}