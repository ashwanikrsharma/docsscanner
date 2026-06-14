package com.pocketscan.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    val screenHorizontal = 16.dp
    val cardInternal = 16.dp
    val sectionGap = 24.dp
}

// Y2K Bubble shapes — extra-rounded, bubbly feel
object Shapes {
    val card = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(24.dp)
    val xLarge = RoundedCornerShape(32.dp)
    val pill = RoundedCornerShape(50.dp)
    val blob = RoundedCornerShape(topStart = 40.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 40.dp)
}
