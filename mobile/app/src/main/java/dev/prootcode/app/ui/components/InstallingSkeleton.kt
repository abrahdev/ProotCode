package dev.prootcode.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import dev.prootcode.app.data.environment.InstallStage

@Composable
fun InstallingSkeleton(
    stage: InstallStage?,
    progress: Pair<Long, Long>?,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "installing_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "installing_skeleton_alpha",
    )
    val shimmer = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
    val fraction =
        when {
            progress != null && progress.second > 0 -> progress.first.toFloat() / progress.second
            stage == InstallStage.EXTRACT_ROOTFS -> 0.35f
            stage == InstallStage.INSTALL_PACKAGES -> 0.6f
            stage == InstallStage.INSTALL_CODE_SERVER -> 0.85f
            stage == InstallStage.READY -> 1f
            else -> 0.1f
        }
    val status =
        stringResource(
            when (stage) {
                InstallStage.EXTRACT_ROOTFS -> R.string.install_stage_rootfs
                InstallStage.INSTALL_PACKAGES -> R.string.install_stage_packages
                InstallStage.INSTALL_CODE_SERVER -> R.string.install_stage_code_server
                InstallStage.READY -> R.string.install_stage_ready
                null ->
                    if (progress != null) {
                        R.string.install_downloading
                    } else {
                        R.string.wizard_preparing
                    }
            },
        )
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(shimmer, MaterialTheme.shapes.large),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(0.6f)
                                .height(14.dp)
                                .background(shimmer, MaterialTheme.shapes.small),
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(0.4f)
                                .height(12.dp)
                                .background(shimmer, MaterialTheme.shapes.small),
                    )
                }
            }
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
            Text(
                text = stringResource(R.string.home_installing_percent, (fraction * 100).toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
