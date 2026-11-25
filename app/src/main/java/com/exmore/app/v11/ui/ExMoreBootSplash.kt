package com.exmore.app.v11.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exmore.app.R

/**
 * A readable, layout-driven loading screen instead of text baked into a 1080x2340 bitmap.
 * It appears only while essential data is loading and never introduces an artificial delay.
 * The platform-controlled Android 12+ starting window is intentionally separate.
 */
@Composable
internal fun ExMoreBootSplash() {
    val dark = isSystemInDarkTheme()
    val compact = LocalConfiguration.current.screenHeightDp < 680
    val backdrop = if (dark) Color(0xFF071F45) else Color(0xFFFCFBF8)
    val main = if (dark) Color(0xFFF7FAFD) else Color(0xFF112B51)
    val secondary = if (dark) Color(0xFFD2DEE7) else Color(0xFF516778)
    val gold = Color(0xFFC79D39)
    val teal = if (dark) Color(0xFF70D9CA) else Color(0xFF127D77)

    Column(
        modifier = Modifier.fillMaxSize()
            .background(backdrop)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.exmore_splash_center),
            contentDescription = null,
            modifier = Modifier.size(if (compact) 146.dp else 196.dp),
        )
        Spacer(Modifier.height(if (compact) 20.dp else 28.dp))
        Text(
            text = "ExMore",
            color = main,
            fontSize = if (compact) 39.sp else 46.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.width(22.dp).height(1.dp).background(gold))
            Spacer(Modifier.width(12.dp))
            Text(
                "LIFE PLANNER",
                color = secondary,
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(Modifier.width(12.dp))
            Box(Modifier.width(22.dp).height(1.dp).background(gold))
        }
        Spacer(Modifier.height(if (compact) 20.dp else 28.dp))
        Text(
            "Your Day. Your Work. Your Money.",
            color = secondary,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            maxLines = 2,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(Modifier.width(36.dp).height(3.dp).background(main))
            Box(Modifier.width(28.dp).height(3.dp).background(teal))
            Box(Modifier.width(20.dp).height(3.dp).background(gold))
        }
    }
}
