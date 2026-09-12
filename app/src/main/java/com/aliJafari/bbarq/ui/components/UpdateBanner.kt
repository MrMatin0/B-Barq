package com.aliJafari.bbarq.ui.components

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.utils.UpdateInfo

/** "A new version is available" banner, now sharing the NoticeCard language. */
@Composable
fun UpdateBanner(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    NoticeCard(
        title = stringResource(R.string.update_available_title),
        modifier = modifier,
        message = stringResource(R.string.update_available_subtitle, updateInfo.latestVersion),
        iconRes = R.drawable.ic_renew,
        tone = NoticeTone.Accent,
        actionLabel = stringResource(R.string.update_button),
        onAction = {
            context.startActivity(Intent(Intent.ACTION_VIEW, updateInfo.downloadUrl.toUri()))
        },
        onDismiss = onDismiss,
    )
}
