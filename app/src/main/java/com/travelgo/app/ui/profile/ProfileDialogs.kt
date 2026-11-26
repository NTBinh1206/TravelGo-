package com.travelgo.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

/** Hộp chọn ngôn ngữ. Ứng dụng hiện chỉ có tiếng Việt nên chỉ có một lựa chọn. */
@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Ngôn ngữ", color = Navy, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = true, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Blue))
                    Text("  Tiếng Việt", color = Navy, fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Phiên bản này mới có giao diện tiếng Việt. Tên địa điểm lấy theo OpenStreetMap nên có thể có tên tiếng Anh.",
                    color = Muted,
                    fontSize = 14.sp,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng", color = Blue) } },
    )
}

/** Nội dung chính sách: ứng dụng dùng dữ liệu gì, gửi đi đâu. Mỗi mục là (tiêu đề, nội dung). */
val POLICY_SECTIONS = listOf(
    "Tài khoản" to
        "Đăng nhập qua Firebase Authentication (Google hoặc Khách ẩn danh). Ứng dụng chỉ đọc tên, email và ảnh đại diện để hiện ở màn Cá nhân.",
    "Vị trí" to
        "Vị trí dùng để tìm địa điểm và quán quanh bạn. Vị trí gần nhất chỉ lưu trên máy để mở lại dùng ngay, không lưu lên máy chủ của nhóm. Khi tìm thêm địa điểm, tính quãng đường hoặc lấy ảnh, toạ độ được gửi tới OpenStreetMap (Overpass, Nominatim), OSRM và Wikipedia.",
    "Báo quán đã đóng cửa" to
        "Khi bạn báo, Cloud Firestore lưu mã quán, tên, toạ độ và mã tài khoản (uid) của bạn; không lưu email. Chỉ người đã đăng nhập mới đọc được, và không ai xoá được từ ứng dụng.",
    "Chống báo sai" to
        "Để chống một người dùng nhiều tài khoản báo sai, Cloud Firestore lưu thời điểm bạn bắt đầu dùng ứng dụng và số lần báo trong 24 giờ. Trước khi báo, ứng dụng đọc GPS để kiểm tra bạn ở gần quán; toạ độ này không gửi lên máy chủ.",
    "Yêu thích" to
        "Địa điểm bạn thả tim được lưu theo tài khoản trên Cloud Firestore (mã và tên địa điểm). Chỉ chính bạn đọc và sửa được; bỏ tim là xoá.",
    "Ghi chú OpenStreetMap" to
        "Chỉ gửi khi bạn tick ô đồng ý. Ghi chú gửi ẩn danh, chỉ gồm tên quán, số người đã báo và đường dẫn tới quán trên OpenStreetMap; ai cũng xem được trên openstreetmap.org.",
    "Google Maps" to
        "Nút xem ảnh, đánh giá và dẫn đường mở ứng dụng Google Maps với tên và toạ độ địa điểm.",
    "Đăng xuất" to
        "Đăng xuất sẽ thoát tài khoản trên máy này. Muốn xoá dữ liệu báo quán của bạn, liên hệ nhóm phát triển.",
)

/** Hộp chính sách quyền riêng tư, cuộn được. */
@Composable
fun PolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Chính sách quyền riêng tư", color = Navy, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                POLICY_SECTIONS.forEach { (heading, body) ->
                    Text(heading, color = Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(body, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đã hiểu", color = Blue) } },
    )
}
