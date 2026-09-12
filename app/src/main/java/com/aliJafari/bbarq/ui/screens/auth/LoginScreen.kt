package com.aliJafari.bbarq.ui.screens.auth

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.ui.components.BBarqButton
import com.aliJafari.bbarq.ui.components.BBarqButtonTone
import com.aliJafari.bbarq.ui.components.GlassCard
import com.aliJafari.bbarq.ui.components.NoticeCard
import com.aliJafari.bbarq.ui.components.NoticeTone
import com.aliJafari.bbarq.ui.components.PersianText
import com.aliJafari.bbarq.ui.theme.Spacing

/**
 * Sign in to Barghe Man.
 *
 * Stateless: renders [LoginUiState], emits [LoginEvent]. The only Android-side
 * concern left is [onRestart], which the Activity performs.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        // Ambient brand glow behind the content.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.18f),
                            scheme.tertiary.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                        radius = 900f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlowingLogo()

            Box(Modifier.padding(top = Spacing.lg))
            PersianText(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            PersianText(
                text = stringResource(R.string.login_subtitle),
                modifier = Modifier.padding(top = Spacing.xs),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Box(Modifier.padding(top = Spacing.xxl))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    state.error?.let { error ->
                        NoticeCard(
                            title = error,
                            tone = NoticeTone.Error,
                            iconRes = R.drawable.baseline_error_24,
                        )
                    }

                    Crossfade(targetState = state.step, label = "loginStep") { step ->
                        when (step) {
                            LoginStep.Phone -> PhoneStep(state = state, onEvent = onEvent)
                            LoginStep.Code -> CodeStep(state = state, onEvent = onEvent)
                            LoginStep.Success -> SuccessStep(onRestart = onRestart)
                        }
                    }
                }
            }

            PersianText(
                text = stringResource(R.string.login_attention),
                modifier = Modifier.padding(top = Spacing.lg),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PhoneStep(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        PhoneNumberField(
            value = state.phone,
            onValueChange = { onEvent(LoginEvent.PhoneChanged(it)) },
            isError = state.error != null,
            onImeAction = { onEvent(LoginEvent.SendOtp) },
        )
        BBarqButton(
            text = stringResource(R.string.login_send_code),
            onClick = { onEvent(LoginEvent.SendOtp) },
            enabled = state.canSendOtp,
            loading = state.isSubmitting,
            fillWidth = true,
        )
    }
}

@Composable
private fun CodeStep(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            PersianText(
                text = stringResource(R.string.login_code_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            PersianText(
                text = stringResource(R.string.login_code_subtitle, state.phone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OtpCodeField(
            value = state.code,
            onValueChange = { onEvent(LoginEvent.CodeChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            isError = state.error != null,
            onFilled = { onEvent(LoginEvent.VerifyOtp) },
        )

        BBarqButton(
            text = stringResource(R.string.login_verify_code),
            onClick = { onEvent(LoginEvent.VerifyOtp) },
            enabled = state.canVerify,
            loading = state.isSubmitting,
            fillWidth = true,
        )

        BBarqButton(
            text = if (state.canResend) {
                stringResource(R.string.login_resend)
            } else {
                stringResource(R.string.login_resend_in, state.resendSecondsLeft)
            },
            onClick = { onEvent(LoginEvent.ResendOtp) },
            tone = BBarqButtonTone.Ghost,
            enabled = state.canResend,
            fillWidth = true,
        )

        BBarqButton(
            text = stringResource(R.string.login_change_number),
            onClick = { onEvent(LoginEvent.ChangeNumber) },
            tone = BBarqButtonTone.Ghost,
            fillWidth = true,
        )
    }
}

@Composable
private fun SuccessStep(onRestart: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PersianText(
            text = stringResource(R.string.login_success_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        PersianText(
            text = stringResource(R.string.login_success_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        BBarqButton(
            text = stringResource(R.string.login_restart_app),
            onClick = onRestart,
            iconRes = R.drawable.ic_renew,
            fillWidth = true,
        )
    }
}

@Composable
private fun GlowingLogo(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "logoGlow")
    val bloom by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logoBloom",
    )

    Box(
        modifier = modifier
            .size(Spacing.huge * 2.5f)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        scheme.primary.copy(alpha = 0.10f + 0.22f * bloom),
                        Color.Transparent,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(Spacing.huge + Spacing.xl),
        )
    }
}
