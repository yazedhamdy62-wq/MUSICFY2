package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.88f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )

    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        startAnimation = true
        // Smooth loading progress bar animation from 0% to 100%
        progressAnim.animateTo(
            targetValue = 0.35f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        progressAnim.animateTo(
            targetValue = 0.75f,
            animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
        )
        progressAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        delay(250)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00B048), // Top vibrant emerald green
                        Color(0xFF00933C),
                        Color(0xFF015222),
                        Color(0xFF012B12),
                        Color(0xFF001006),
                        Color(0xFF000000)  // Bottom deep black
                    )
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    alpha = alphaAnim
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Central Branding: Original Logo, Title, Slogan
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // Exact Original Musicfy Logo scaled to match the design screenshot
                Image(
                    painter = painterResource(id = R.drawable.ic_musicfy_logo),
                    contentDescription = "Musicfy Logo",
                    modifier = Modifier.size(176.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Musicfy Title with exact scale and boldness
                Text(
                    text = "Musicfy",
                    color = Color(0xFF00E676),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Slogan with exact scale and font
                Text(
                    text = "a music you can trust",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.2.sp
                )
            }

            // Bottom Section: Animated Loading Bar & Powered by yazed
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                // Loading Progress Bar
                Box(
                    modifier = Modifier
                        .width(235.dp)
                        .height(4.5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progressAnim.value.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Powered by yazed
                Text(
                    text = "powered by yazed",
                    color = Color(0xFF9E9E9E),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}
