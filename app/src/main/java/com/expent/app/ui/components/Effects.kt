package com.expent.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.expent.app.core.util.MoneyUtil

/**
 * Money that moves like a cash register: the figure counts up (or down) to its
 * value whenever [amountCents] changes, instead of snapping in place.
 */
@Composable
fun AnimatedMoneyText(
    amountCents: Long,
    symbol: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(amountCents) {
        animated.animateTo(
            targetValue = amountCents.toFloat(),
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }
    Text(
        text = MoneyUtil.format(animated.value.toLong(), symbol = symbol),
        style = style,
        color = color,
        modifier = modifier
    )
}

/**
 * A quiet entrance: content fades in and rises slightly on first composition.
 * Used sparingly so motion stays a moment, not a default.
 */
@Composable
fun RevealItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val visible = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = visible,
        modifier = modifier,
        enter = fadeIn(tween(420, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 5 }
    ) {
        content()
    }
}
