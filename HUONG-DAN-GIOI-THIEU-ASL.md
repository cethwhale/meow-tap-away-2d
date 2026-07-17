# Bộ Giới Thiệu ASLDEV.VN dùng chung

Tài liệu này dùng cho các ứng dụng Android viết bằng **Kotlin + Jetpack Compose Material 3**.

## 1. Chép logo ASL vào dự án

Chép file:

```text
asl_logo.png
```

vào thư mục:

```text
app/src/main/res/drawable/asl_logo.png
```

Tên file phải viết thường, không có khoảng trắng. Chép thêm icon hoặc linh vật riêng của ứng dụng vào cùng thư mục `drawable`.

Logo đang dùng trong Meow Tap Away 2D nằm tại:

```text
app/src/main/res/drawable/asl_logo.png
```

## 2. Tạo file `AslAboutDialog.kt`

Tạo file trong package của ứng dụng, ví dụ:

```text
app/src/main/java/com/example/myapp/AslAboutDialog.kt
```

Đổi dòng `package com.example.myapp` thành package thật của dự án, sau đó dán toàn bộ mã sau:

```kotlin
package com.example.myapp

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AslAboutDialog(
    visible: Boolean,
    appName: String,
    @DrawableRes mascotRes: Int,
    @DrawableRes aslLogoRes: Int = R.drawable.asl_logo,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val ink = Color(0xFF1D2747)
    val blue = Color(0xFF2998EE)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.92f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAA1D2747))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(mascotRes),
                    contentDescription = appName,
                    modifier = Modifier.size(78.dp),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = appName.uppercase(),
                    color = ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Image(
                    painter = painterResource(aslLogoRes),
                    contentDescription = "ASLDEV.VN",
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(78.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Copyright: © 2026 ",
                        color = Color(0xFF657087),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "ASLDEV.VN",
                        color = blue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .clickable {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://asldev.vn")
                                    )
                                )
                            }
                            .padding(horizontal = 3.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = blue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Quay lại",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}
```

## 3. Gọi bảng Giới Thiệu trong màn hình

Khai báo trạng thái:

```kotlin
var showAbout by remember { mutableStateOf(false) }
```

Nút mở Giới Thiệu:

```kotlin
Button(onClick = { showAbout = true }) {
    Text("Giới thiệu")
}
```

Đặt component gần cuối `Box` chính của màn hình để bảng luôn nằm trên giao diện:

```kotlin
AslAboutDialog(
    visible = showAbout,
    appName = "TÊN ỨNG DỤNG",
    mascotRes = R.drawable.ten_icon_hoac_linh_vat,
    onClose = { showAbout = false }
)
```

Ví dụ cho Meow Tap Away 2D:

```kotlin
AslAboutDialog(
    visible = showAbout,
    appName = "Meow Tap Away 2D",
    mascotRes = R.drawable.cat_center,
    onClose = { showAbout = false }
)
```

## 4. Cho nút Back đóng bảng Giới Thiệu

Nếu ứng dụng sử dụng `BackHandler`, thêm:

```kotlin
BackHandler(enabled = showAbout) {
    showAbout = false
}
```

Import:

```kotlin
import androidx.activity.compose.BackHandler
```

## 5. Lưu ý

- Không cần khai báo quyền gọi điện hoặc quyền Internet chỉ để mở website bằng trình duyệt.
- Website sử dụng liên kết: `https://asldev.vn`.
- Nên dùng PNG nền trong suốt cho logo và linh vật.
- Nếu logo quá lớn hoặc quá nhỏ, chỉnh `fillMaxWidth(0.62f)` và `height(78.dp)`.
- Nếu tên ứng dụng dài, giảm `fontSize` từ `24.sp` xuống `20.sp`.
- Có thể đổi màu `blue` để phù hợp nhận diện của từng ứng dụng nhưng nên giữ nguyên logo và nội dung bản quyền.

## Nội dung chuẩn

```text
[Hình linh vật hoặc icon ứng dụng]
TÊN ỨNG DỤNG

[Logo ASLDEV.VN]
Copyright: © 2026 ASLDEV.VN
```
