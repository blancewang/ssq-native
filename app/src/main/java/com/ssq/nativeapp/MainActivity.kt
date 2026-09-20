package com.ssq.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import kotlin.math.min

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
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0F14)) {
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
    var selectedBlues by remember { mutableStateOf((1..16).toSet()) }
    var selectedTab by remember { mutableStateOf(0) }

    // 过滤条件
    var sumMin by remember { mutableStateOf(60) }
    var sumMax by remember { mutableStateOf(140) }
    var oddCount by remember { mutableStateOf(setOf(2, 3, 4)) }
    var bigCount by remember { mutableStateOf(setOf(2, 3, 4)) }
    var zone1 by remember { mutableStateOf(setOf(1, 2, 3)) }
    var zone2 by remember { mutableStateOf(setOf(1, 2, 3)) }
    var zone3 by remember { mutableStateOf(setOf(1, 2, 3)) }

    var filterResult by remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var filtering by remember { mutableStateOf(false) }

    val latest = history.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("双色球原生版", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0A458))
            Text("V1.2", fontSize = 13.sp, color = Color(0xFF8B93A7))
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF151B24), edgePadding = 8.dp) {
            listOf("首页", "候选池", "蓝球", "条件", "结果", "统计").forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontSize = 13.sp) })
            }
        }

        when (selectedTab) {
            0 -> HomeTab(latest, history, loading, message) { loading = true; message = "正在获取..." }
            1 -> PoolTab(selectedReds) { num -> selectedReds = if (selectedReds.contains(num)) selectedReds - num else selectedReds + num }
            2 -> BlueTab(selectedBlues) { num -> selectedBlues = if (selectedBlues.contains(num)) selectedBlues - num else selectedBlues + num }
            3 -> ConditionTab(sumMin, sumMax, oddCount, bigCount, zone1, zone2, zone3,
                onSumMin = { sumMin = it }, onSumMax = { sumMax = it },
                onOdd = { oddCount = it }, onBig = { bigCount = it },
                onZ1 = { zone1 = it }, onZ2 = { zone2 = it }, onZ3 = { zone3 = it })
            4 -> ResultTab(selectedReds, selectedBlues, sumMin, sumMax, oddCount, bigCount, zone1, zone2, zone3, filterResult, filtering) {
                filtering = true
            }
            5 -> StatsTab(history)
        }
    }

    LaunchedEffect(loading) {
        if (loading) {
            val list = fetchHistory(40)
            if (list.isNotEmpty()) {
                history = list
                message = "已更新 ${list.size} 期"
            } else message = "获取失败"
            loading = false
        }
    }

    LaunchedEffect(filtering) {
        if (filtering) {
            filterResult = doFilter(selectedReds.toList().sorted(), sumMin, sumMax, oddCount, bigCount, zone1, zone2, zone3)
            filtering = false
            selectedTab = 4
        }
    }
}

@Composable
fun HomeTab(latest: DrawResult?, history: List<DrawResult>, loading: Boolean, message: String, onUpdate: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    } else Text(message, color = Color(0xFF8B93A7))
                }
            }
        }
        item {
            Button(onClick = onUpdate, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0A458)), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(if (loading) "获取中..." else "更新开奖数据", color = Color(0xFF1A1200), fontWeight = FontWeight.Bold)
            }
        }
        item { Text("历史开奖", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold) }
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
        Text("红球候选池", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
        Text("已选 ${selected.size} 个（至少选6个才能过滤）", color = Color(0xFF8B93A7), modifier = Modifier.padding(vertical = 8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(6), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items((1..33).toList()) { num ->
                val isSelected = selected.contains(num)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp).background(if (isSelected) Color(0xFFE0A458) else Color(0xFF2A3444), CircleShape).clickable { onToggle(num) }) {
                    Text(num.toString().padStart(2, '0'), color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BlueTab(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("蓝球选择（点选保留）", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
        Text("已保留 ${selected.size} 个", color = Color(0xFF8B93A7), modifier = Modifier.padding(vertical = 8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items((1..16).toList()) { num ->
                val isSelected = selected.contains(num)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).background(if (isSelected) Color(0xFF2F6FB0) else Color(0xFF2A3444), CircleShape).clickable { onToggle(num) }) {
                    Text(num.toString().padStart(2, '0'), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConditionTab(
    sumMin: Int, sumMax: Int, oddCount: Set<Int>, bigCount: Set<Int>,
    zone1: Set<Int>, zone2: Set<Int>, zone3: Set<Int>,
    onSumMin: (Int) -> Unit, onSumMax: (Int) -> Unit,
    onOdd: (Set<Int>) -> Unit, onBig: (Set<Int>) -> Unit,
    onZ1: (Set<Int>) -> Unit, onZ2: (Set<Int>) -> Unit, onZ3: (Set<Int>) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("和值范围：$sumMin ~ $sumMax", color = Color(0xFFE0A458))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("最小", color = Color.White, modifier = Modifier.width(40.dp))
                Slider(value = sumMin.toFloat(), onValueChange = { onSumMin(it.toInt()) }, valueRange = 21f..150f, modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("最大", color = Color.White, modifier = Modifier.width(40.dp))
                Slider(value = sumMax.toFloat(), onValueChange = { onSumMax(it.toInt()) }, valueRange = 50f..183f, modifier = Modifier.weight(1f))
            }
        }
        item { MultiSelectRow("奇号个数", (0..6).toList(), oddCount, onOdd) }
        item { MultiSelectRow("大号个数(≥17)", (0..6).toList(), bigCount, onBig) }
        item { MultiSelectRow("一区个数(01-11)", (0..6).toList(), zone1, onZ1) }
        item { MultiSelectRow("二区个数(12-22)", (0..6).toList(), zone2, onZ2) }
        item { MultiSelectRow("三区个数(23-33)", (0..6).toList(), zone3, onZ3) }
    }
}

@Composable
fun MultiSelectRow(title: String, options: List<Int>, selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    Column {
        Text(title, color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { v ->
                val on = selected.contains(v)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).background(if (on) Color(0xFFE0A458) else Color(0xFF2A3444), RoundedCornerShape(6.dp)).clickable {
                        onChange(if (on) selected - v else selected + v)
                    }
                ) {
                    Text("$v", color = if (on) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ResultTab(
    reds: Set<Int>, blues: Set<Int>, sumMin: Int, sumMax: Int,
    oddCount: Set<Int>, bigCount: Set<Int>, z1: Set<Int>, z2: Set<Int>, z3: Set<Int>,
    result: List<List<Int>>, filtering: Boolean, onStart: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("当前候选红球：${reds.size} 个", color = Color.White)
        Text("蓝球保留：${blues.size} 个", color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onStart,
            enabled = !filtering && reds.size >= 6,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0A458)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(if (filtering) "过滤中..." else "开始过滤", color = Color(0xFF1A1200), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("通过注数：${result.size}", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(result.take(100)) { combo ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2430))) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        combo.forEach { Ball(it, false, 26.dp) }
                    }
                }
            }
            if (result.size > 100) {
                item { Text("仅显示前100注，共${result.size}注", color = Color(0xFF8B93A7)) }
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
    val appearCount = IntArray(34)
    val omit = IntArray(34) { history.size }
    history.forEachIndexed { index, draw ->
        draw.reds.forEach { n ->
            appearCount[n]++
            if (omit[n] == history.size) omit[n] = index
        }
    }
    val hot = (1..33).sortedByDescending { appearCount[it] }.take(8)
    val cold = (1..33).sortedByDescending { omit[it] }.take(8)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("基于最近 ${history.size} 期", color = Color(0xFF8B93A7)) }
        item { StatCard("热号", hot, appearCount) }
        item { StatCard("冷号/遗漏", cold, omit, true) }
    }
}

@Composable
fun StatCard(title: String, numbers: List<Int>, values: IntArray, isOmit: Boolean = false) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151B24))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            numbers.forEach { n ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ball(n, false, 26.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(if (isOmit) "遗漏 ${values[n]} 期" else "出现 ${values[n]} 次", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun Ball(number: Int, isBlue: Boolean, size: androidx.compose.ui.unit.Dp = 36.dp) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size).background(if (isBlue) Color(0xFF2F6FB0) else Color(0xFFD1495B), CircleShape)) {
        Text(number.toString().padStart(2, '0'), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
    }
}

fun doFilter(
    pool: List<Int>, sumMin: Int, sumMax: Int,
    oddCount: Set<Int>, bigCount: Set<Int>,
    z1: Set<Int>, z2: Set<Int>, z3: Set<Int>
): List<List<Int>> {
    if (pool.size < 6) return emptyList()
    val result = mutableListOf<List<Int>>()
    val n = pool.size
    // 简单组合枚举（限制数量防止卡顿）
    fun dfs(start: Int, path: MutableList<Int>) {
        if (path.size == 6) {
            val sum = path.sum()
            if (sum < sumMin || sum > sumMax) return
            val odd = path.count { it % 2 == 1 }
            if (odd !in oddCount) return
            val big = path.count { it >= 17 }
            if (big !in bigCount) return
            val c1 = path.count { it <= 11 }
            val c2 = path.count { it in 12..22 }
            val c3 = path.count { it >= 23 }
            if (c1 !in z1 || c2 !in z2 || c3 !in z3) return
            result.add(path.toList())
            return
        }
        if (result.size >= 500) return // 限制最多500注
        for (i in start until n) {
            path.add(pool[i])
            dfs(i + 1, path)
            path.removeAt(path.lastIndex)
        }
    }
    dfs(0, mutableListOf())
    return result
}

suspend fun fetchHistory(limit: Int = 30): List<DrawResult> = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder().url("https://api.huiniao.top/interface/home/lotteryHistory?type=ssq&page=1&limit=$limit").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        val json = JSONObject(body)
        if (json.optInt("code") != 1) return@withContext emptyList()
        val list = json.getJSONObject("data").getJSONObject("data").getJSONArray("list")
        val result = mutableListOf<DrawResult>()
        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            val reds = listOf(item.getInt("one"), item.getInt("two"), item.getInt("three"), item.getInt("four"), item.getInt("five"), item.getInt("six")).sorted()
            result.add(DrawResult(item.getString("code"), reds, item.getInt("seven"), item.optString("day", "")))
        }
        result
    } catch (e: Exception) {
        emptyList()
    }
}
