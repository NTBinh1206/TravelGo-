package com.travelgo.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.R
import com.travelgo.app.components.softShadow
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.FacebookBlue
import com.travelgo.app.ui.theme.Navy

private val textShadow = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 3f), 10f))

/**
 * 01 · Đăng nhập (Người 1).
 * TODO(Người 1):
 *  - Google: Credential Manager + Firebase Auth
 *  - Facebook: Facebook Login SDK + Firebase Auth
 *  - Khách: Firebase Anonymous Auth
 *  - Gán errorMessage khi đăng nhập thất bại
 */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.img_splash),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.10f),
                    0.45f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.40f),
                ),
            ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 76.dp),
        ) {
            val logoShape = RoundedCornerShape(20.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp).softShadow(logoShape).background(Color.White, logoShape),
            ) {
                Icon(Icons.Outlined.NearMe, contentDescription = null, tint = Blue, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("TravelGo", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, style = textShadow)
            Spacer(Modifier.height(10.dp))
            Text("Khám phá thế giới theo cách của bạn", color = Color.White, fontSize = 16.sp, style = textShadow)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 22.dp),
        ) {
            errorMessage?.let {
                Text(it, color = Color(0xFFFFD6D6), textAlign = TextAlign.Center, style = textShadow)
            }
            SocialButton(
                text = "Tiếp tục với Google",
                icon = painterResource(R.drawable.ic_google),
                iconTint = Color.Unspecified,
                container = Color.White,
                content = Navy,
                onClick = { /* TODO(Người 1): đăng nhập Google */ onLoggedIn() },
            )
            SocialButton(
                text = "Tiếp tục với Facebook",
                icon = painterResource(R.drawable.ic_facebook),
                iconTint = Color.White,
                container = FacebookBlue,
                content = Color.White,
                onClick = { /* TODO(Người 1): đăng nhập Facebook */ onLoggedIn() },
            )
            Text(
                "Dùng thử không cần tài khoản",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                style = textShadow,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable { /* TODO(Người 1): chế độ khách */ onLoggedIn() },
            )
            Text(
                "Bằng việc tiếp tục, bạn đồng ý với Điều khoản & Chính sách bảo mật",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                style = textShadow,
            )
        }
    }
}

@Composable
private fun SocialButton(
    text: String,
    icon: Painter,
    iconTint: Color,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth().height(54.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
