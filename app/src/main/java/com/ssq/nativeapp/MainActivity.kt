package com.ssq.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    MainScreen()
                }
            }
        }
    }
}

data class DrawResult(
    val period: String = "",
    val reds: List<Int> = emptyList(),
    val blue: Int = 0,
    val day: String = ""
)

@Composable
fun MainScreen() {
    var history by remember { mutableStateOf<List<DrawResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("点击更新获取数据") }
    var selectedReds by remember { mutableStateOf(setOf<Int>()) }
    var selectedTab by remember { mutableStateOf(0) } // 0=首页 1=候选池 2=统计

    val latest = history.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题
        Column(modifier = Modifier.padding(16.dp)) {
            Text("双色球原生版", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0A458))
            Text("V1.1", fontSize = 13.sp, color = Color(0xFF8B93A7))
        }

        // 标签栏
        TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF151B24)) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("首页") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("候选池") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("统计") })
        }

        when (selectedTab) {
            0 -> HomeTab(latest, history, loading, message) {
                loading = true
                message = "正在获取..."
            }
            1 -> PoolTab(selectedReds) { num ->
                selectedReds = if (selectedReds.contains(num)) selectedReds - num else selectedReds + num
            }
            2 -> StatsTab(history)
        }
    }

    // 网络请求
    LaunchedEffect(loading) {
        if (loading) {
            val list = fetchHistory(30)
            if (list.isNotEmpty()) {
                history = list
                message = "已更新 ${list.size} 期"
            } else {
                message = "获取失败，请检查网络"
            }
            loading = false
        }
    }
}

@Composable
fun HomeTab(
    latest: DrawResult?,
    history: List<DrawResult>,
    loading: Boolean,
    message: String,
    onUpdate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151B24))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("最新开奖", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (latest != null) {
                        Text("第 ${latest.period} 期 ${latest.day}", color = Color.White)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            latest.reds.forEach { Ball(it, false) }
                            Ball(latest.blue, true)
                        }
                    } else {
                        Text(message, color = Color(0xFF8B93A7))
                    }
                }
            }
        }

        item {
            Button(
                onClick = onUpdate,
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0A458)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (loading) "获取中..." else "更新开奖数据", color = Color(0xFF1A1200), fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text("历史开奖", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        }

        items(history) { draw ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2430))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${draw.period}  ${draw.day}", color = Color(0xFF8B93A7), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        draw.reds.forEach { Ball(it, false, 28.dp) }
                        Ball(draw.blue, true, 28.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun PoolTab(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("红球候选池（点选加入/取消）", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
        Text("已选 ${selected.size} 个", color = Color(0xFF8B93A7), modifier = Modifier.padding(vertical = 8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items((1..33).toList()) { num ->
                val isSelected = selected.contains(num)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isSelected) Color(0xFFE0A458) else Color(0xFF2A3444),
                            CircleShape
                        )
                        .clickable { onToggle(num) }
                ) {
                    Text(
                        text = num.toString().padStart(2, '0'),
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatsTab(history: List<DrawResult>) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请先更新开奖数据", color = Color(0xFF8B93A7))
        }
        return
    }

    // 计算遗漏和出现次数
    val appearCount = IntArray(34)
    val omit = IntArray(34) { history.size }
    history.forEachIndexed { index, draw ->
        draw.reds.forEach { n ->
            appearCount[n]++
            if (omit[n] == history.size) omit[n] = index
        }
    }

    val hot = (1..33).sortedByDescending { appearCount[it] }.take(6)
    val cold = (1..33).sortedByDescending { omit[it] }.take(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("基于最近 ${history.size} 期", color = Color(0xFF8B93A7))
        }
        item {
            StatCard("热号（出现次数多）", hot, appearCount)
        }
        item {
            StatCard("冷号/遗漏（长时间未出现）", cold, omit, isOmit = true)
        }
    }
}

@Composable
fun StatCard(title: String, numbers: List<Int>, values: IntArray, isOmit: Boolean = false) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151B24))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            numbers.forEach { n ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Ball(n, false, 28.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isOmit) "遗漏 ${values[n]} 期" else "出现 ${values[n]} 次",
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun Ball(number: Int, isBlue: Boolean, size: androidx.compose.ui.unit.Dp = 36.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(if (isBlue) Color(0xFF2F6FB0) else Color(0xFFD1495B), CircleShape)
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp
        )
    }
}

suspend fun fetchHistory(limit: Int = 30): List<DrawResult> = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.huiniao.top/interface/home/lotteryHistory?type=ssq&page=1&limit=$limit")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        val json = JSONObject(body)
        if (json.optInt("code") != 1) return@withContext emptyList()

        val list = json.getJSONObject("data").getJSONObject("data").getJSONArray("list")
        val result = mutableListOf<DrawResult>()
        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            val reds = listOf(
                item.getInt("one"), item.getInt("two"), item.getInt("three"),
                item.getInt("four"), item.getInt("five"), item.getInt("six")
            ).sorted()
            result.add(
                DrawResult(
                    period = item.getString("code"),
                    reds = reds,
                    blue = item.getInt("seven"),
                    day = item.optString("day", "")
                )
            )
        }
        result
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
