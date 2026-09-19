package com.ssq.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFE0A458),
                    background = Color(0xFF0B0F14),
                    surface = Color(0xFF151B24)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0B0F14)
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

data class DrawResult(
    val period: String = "",
    val reds: List<Int> = emptyList(),
    val blue: Int = 0
)

@Composable
fun HomeScreen() {
    var draw by remember { mutableStateOf(DrawResult()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("点击下方按钮获取最新开奖") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "双色球原生版",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE0A458)
        )
        Text(
            text = "V1.0 基础版",
            fontSize = 13.sp,
            color = Color(0xFF8B93A7),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151B24)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "最新开奖",
                    color = Color(0xFFE0A458),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (draw.period.isNotEmpty()) {
                    Text(
                        text = "第 ${draw.period} 期",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        draw.reds.forEach { num ->
                            Ball(number = num, isBlue = false)
                        }
                        Ball(number = draw.blue, isBlue = true)
                    }
                } else {
                    Text(text = message, color = Color(0xFF8B93A7))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                loading = true
                message = "正在获取..."
            },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0A458)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = if (loading) "获取中..." else "更新开奖数据",
                color = Color(0xFF1A1200),
                fontWeight = FontWeight.Bold
            )
        }

        // 简单的获取逻辑
        LaunchedEffect(loading) {
            if (loading) {
                val result = fetchLatestDraw()
                if (result != null) {
                    draw = result
                    message = "更新成功"
                } else {
                    message = "获取失败，请检查网络"
                }
                loading = false
            }
        }
    }
}

@Composable
fun Ball(number: Int, isBlue: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .background(
                color = if (isBlue) Color(0xFF2F6FB0) else Color(0xFFD1495B),
                shape = CircleShape
            )
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

suspend fun fetchLatestDraw(): DrawResult? = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.huiniao.top/interface/home/lotteryHistory?type=ssq&page=1&limit=1")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext null
        val json = JSONObject(body)
        if (json.optInt("code") != 1) return@withContext null

        val list = json.getJSONObject("data")
            .getJSONObject("data")
            .getJSONArray("list")
        if (list.length() == 0) return@withContext null

        val item = list.getJSONObject(0)
        val reds = listOf(
            item.getInt("one"),
            item.getInt("two"),
            item.getInt("three"),
            item.getInt("four"),
            item.getInt("five"),
            item.getInt("six")
        ).sorted()
        val blue = item.getInt("seven")
        val period = item.getString("code")

        DrawResult(period, reds, blue)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
