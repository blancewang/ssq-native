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

data class CustomGroup(
    val name: String,
    val numbers: Set<Int>,
    val allow: Set<Int>,
    val enabled: Boolean = true
)

@Composable
fun MainScreen() {
    var history by remember { mutableStateOf<List<DrawResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("点击更新获取数据") }
    var selectedReds by remember { mutableStateOf(setOf<Int>()) }
    var selectedBlues by remember { mutableStateOf((1..16).toSet()) }
    var selectedTab by remember { mutableStateOf(0) }

    var sumMin by remember { mutableStateOf(70) }
    var sumMax by remember { mutableStateOf(140) }
    var spanMin by remember { mutableStateOf(12) }
    var spanMax by remember { mutableStateOf(28) }
    var oddCount by remember { mutableStateOf(setOf(2, 3, 4)) }
    var bigCount by remember { mutableStateOf(setOf(2, 3, 4)) }
    var primeCount by remember { mutableStateOf(setOf(1, 2, 3)) }
    var zone1 by remember { mutableStateOf(setOf(1, 2, 3)) }
    var zone2 by remember { mutableStateOf(setOf(1, 2, 3)) }
    var zone3 by remember { mutableStateOf(setOf(1, 2, 3)) }
    var road0 by remember { mutableStateOf(setOf(1, 2, 3)) }
    var road1 by remember { mutableStateOf(setOf(1, 2, 3)) }
    var road2 by remember { mutableStateOf(setOf(1, 2, 3)) }

    var customGroups by remember { mutableStateOf(listOf<CustomGroup>()) }
    var filterResult by remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var filtering by remember { mutableStateOf(false) }
    var filterTime by remember { mutableStateOf(0L) }

    val latest = history.firstOrNull()
    val primes = setOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("双色球原生版", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0A458))
            Text("V1.5", fontSize = 13.sp, color = Color(0xFF8B93A7))
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF151B24), edgePadding = 6.dp) {
            listOf("首页", "红池", "蓝球", "条件", "自定义", "结果", "统计").forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 13.sp) })
            }
        }

        when (selectedTab) {
            0 -> HomeTab(latest, history, loading, message) { loading = true; message = "正在获取..." }
            1 -> PoolTab(
                selected = selectedReds,
                onToggle = { num -> selectedReds = if (selectedReds.contains(num)) selectedReds - num else selectedReds + num },
                onSelectAll = { selectedReds = (1..33).toSet() },
                onClear = { selectedReds = emptySet() },
                onInvert = { selectedReds = (1..33).toSet() - selectedReds }
            )
            2 -> BlueTab(selectedBlues) { num ->
                selectedBlues = if (selectedBlues.contains(num)) selectedBlues - num else selectedBlues + num
            }
            3 -> ConditionTab(
                sumMin, sumMax, spanMin, spanMax, oddCount, bigCount, primeCount,
                zone1, zone2, zone3, road0, road1, road2,
                onSumMin = { sumMin = it }, onSumMax = { sumMax = it },
                onSpanMin = { spanMin = it }, onSpanMax = { spanMax = it },
                onOdd = { oddCount = it }, onBig = { bigCount = it }, onPrime = { primeCount = it },
                onZ1 = { zone1 = it }, onZ2 = { zone2 = it }, onZ3 = { zone3 = it },
                onR0 = { road0 = it }, onR1 = { road1 = it }, onR2 = { road2 = it },
                onPreset = { level ->
                    when (level) {
                        "loose" -> {
                            sumMin = 50; sumMax = 160; spanMin = 8; spanMax = 32
                            oddCount = setOf(1, 2, 3, 4, 5); bigCount = setOf(1, 2, 3, 4, 5)
                            primeCount = setOf(0, 1, 2, 3, 4)
                            zone1 = setOf(0, 1, 2, 3, 4); zone2 = setOf(0, 1, 2, 3, 4); zone3 = setOf(0, 1, 2, 3, 4)
                            road0 = setOf(0, 1, 2, 3, 4); road1 = setOf(0, 1, 2, 3, 4); road2 = setOf(0, 1, 2, 3, 4)
                        }
                        "mid" -> {
                            sumMin = 70; sumMax = 140; spanMin = 12; spanMax = 28
                            oddCount = setOf(2, 3, 4); bigCount = setOf(2, 3, 4)
                            primeCount = setOf(1, 2, 3)
                            zone1 = setOf(1, 2, 3); zone2 = setOf(1, 2, 3); zone3 = setOf(1, 2, 3)
                            road0 = setOf(1, 2, 3); road1 = setOf(1, 2, 3); road2 = setOf(1, 2, 3)
                        }
                        "tight" -> {
                            sumMin = 85; sumMax = 125; spanMin = 15; spanMax = 25
                            oddCount = setOf(3); bigCount = setOf(3)
                            primeCount = setOf(2)
                            zone1 = setOf(2); zone2 = setOf(2); zone3 = setOf(2)
                            road0 = setOf(2); road1 = setOf(2); road2 = setOf(2)
                        }
                    }
                }
            )
            4 -> CustomGroupTab(customGroups) { customGroups = it }
            5 -> ResultTab(selectedReds, selectedBlues, filterResult, filtering, filterTime) { filtering = true }
            6 -> StatsTab(history)
        }
    }

    LaunchedEffect(loading) {
        if (loading) {
            val list = fetchHistory(50)
            history = list
            message = if (list.isNotEmpty()) "已更新 ${list.size} 期" else "获取失败"
            loading = false
        }
    }

    LaunchedEffect(filtering) {
        if (filtering) {
            val start = System.currentTimeMillis()
            filterResult = doFilter(
                pool = selectedReds.toList().sorted(),
                sumMin = sumMin, sumMax = sumMax,
                spanMin = spanMin, spanMax = spanMax,
                oddCount = oddCount, bigCount = bigCount, primeCount = primeCount,
                z1 = zone1, z2 = zone2, z3 = zone3,
                r0 = road0, r1 = road1, r2 = road2,
                groups = customGroups,
                primes = primes
            )
            filterTime = System.currentTimeMillis() - start
            filtering = false
            selectedTab = 5
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
                        Text("第 ${latest.period} 期  ${latest.day}", color = Color.White)
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
            Text("历史开奖（最近${history.size}期）", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
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
fun PoolTab(
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onInvert: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("红球候选池", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
        Text("已选 ${selected.size} 个（至少6个才能过滤）", color = Color(0xFF8B93A7), modifier = Modifier.padding(vertical = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            ActionChip("全选", onSelectAll)
            ActionChip("清空", onClear)
            ActionChip("反选", onInvert)
        }
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
                        .background(if (isSelected) Color(0xFFE0A458) else Color(0xFF2A3444), CircleShape)
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
fun ActionChip(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color(0xFF2A3444), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = Color(0xFFE0A458), fontSize = 13.sp)
    }
}

@Composable
fun BlueTab(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("蓝球选择（点选保留）", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
        Text("已保留 ${selected.size} 个", color = Color(0xFF8B93A7), modifier = Modifier.padding(vertical = 8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items((1..16).toList()) { num ->
                val isSelected = selected.contains(num)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isSelected) Color(0xFF2F6FB0) else Color(0xFF2A3444), CircleShape)
                        .clickable { onToggle(num) }
                ) {
                    Text(num.toString().padStart(2, '0'), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConditionTab(
    sumMin: Int, sumMax: Int, spanMin: Int, spanMax: Int,
    oddCount: Set<Int>, bigCount: Set<Int>, primeCount: Set<Int>,
    zone1: Set<Int>, zone2: Set<Int>, zone3: Set<Int>,
    road0: Set<Int>, road1: Set<Int>, road2: Set<Int>,
    onSumMin: (Int) -> Unit, onSumMax: (Int) -> Unit,
    onSpanMin: (Int) -> Unit, onSpanMax: (Int) -> Unit,
    onOdd: (Set<Int>) -> Unit, onBig: (Set<Int>) -> Unit, onPrime: (Set<Int>) -> Unit,
    onZ1: (Set<Int>) -> Unit, onZ2: (Set<Int>) -> Unit, onZ3: (Set<Int>) -> Unit,
    onR0: (Set<Int>) -> Unit, onR1: (Set<Int>) -> Unit, onR2: (Set<Int>) -> Unit,
    onPreset: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("快捷预设", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                ActionChip("宽松") { onPreset("loose") }
                ActionChip("适中") { onPreset("mid") }
                ActionChip("偏严") { onPreset("tight") }
            }
        }
        item {
            Text("和值：$sumMin ~ $sumMax", color = Color(0xFFE0A458))
            Slider(value = sumMin.toFloat(), onValueChange = { onSumMin(it.toInt()) }, valueRange = 30f..150f)
            Slider(value = sumMax.toFloat(), onValueChange = { onSumMax(it.toInt()) }, valueRange = 60f..180f)
        }
        item {
            Text("跨度：$spanMin ~ $spanMax", color = Color(0xFFE0A458))
            Slider(value = spanMin.toFloat(), onValueChange = { onSpanMin(it.toInt()) }, valueRange = 5f..30f)
            Slider(value = spanMax.toFloat(), onValueChange = { onSpanMax(it.toInt()) }, valueRange = 10f..32f)
        }
        item { MultiSelectRow("奇号个数", (0..6).toList(), oddCount, onOdd) }
        item { MultiSelectRow("大号个数(≥17)", (0..6).toList(), bigCount, onBig) }
        item { MultiSelectRow("质数个数", (0..6).toList(), primeCount, onPrime) }
        item { MultiSelectRow("一区(01-11)", (0..6).toList(), zone1, onZ1) }
        item { MultiSelectRow("二区(12-22)", (0..6).toList(), zone2, onZ2) }
        item { MultiSelectRow("三区(23-33)", (0..6).toList(), zone3, onZ3) }
        item { MultiSelectRow("0路个数", (0..6).toList(), road0, onR0) }
        item { MultiSelectRow("1路个数", (0..6).toList(), road1, onR1) }
        item { MultiSelectRow("2路个数", (0..6).toList(), road2, onR2) }
    }
}

@Composable
fun CustomGroupTab(groups: List<CustomGroup>, onChange: (List<CustomGroup>) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var tempNums by remember { mutableStateOf(setOf<Int>()) }
    var tempAllow by remember { mutableStateOf(setOf(0, 1, 2)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("自定义号码组", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
        Text("开启后参与过滤（允许出现个数很重要）", color = Color(0xFF8B93A7), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                showAdd = true
                tempName = "自定义${groups.size + 1}"
                tempNums = emptySet()
                tempAllow = setOf(0, 1, 2)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0A458)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ 添加自定义组", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(groups.size) { index ->
                val g = groups[index]
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2430))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = g.enabled,
                                onCheckedChange = { checked ->
                                    val newList = groups.toMutableList()
                                    newList[index] = g.copy(enabled = checked)
                                    onChange(newList)
                                }
                            )
                            Text(g.name, color = Color.White, modifier = Modifier.weight(1f).padding(start = 8.dp))
                            TextButton(onClick = {
                                onChange(groups.toMutableList().also { it.removeAt(index) })
                            }) {
                                Text("删除", color = Color(0xFFD1495B))
                            }
                        }
                        Text("号码：${g.numbers.sorted().joinToString(",")}", color = Color(0xFF8B93A7), fontSize = 12.sp)
                        Text("允许出现：${g.allow.sorted().joinToString(",")}", color = Color(0xFF8B93A7), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加自定义组") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("名称") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("选择号码：", color = Color.White)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.height(180.dp)
                    ) {
                        items((1..33).toList()) { n ->
                            val on = tempNums.contains(n)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(36.dp)
                                    .background(if (on) Color(0xFFE0A458) else Color(0xFF2A3444), CircleShape)
                                    .clickable {
                                        tempNums = if (on) tempNums - n else tempNums + n
                                    }
                            ) {
                                Text("$n", color = if (on) Color.Black else Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("允许出现个数：", color = Color.White)
                    Row {
                        (0..6).forEach { v ->
                            val on = tempAllow.contains(v)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(3.dp)
                                    .size(32.dp)
                                    .background(if (on) Color(0xFFE0A458) else Color(0xFF2A3444), RoundedCornerShape(4.dp))
                                    .clickable {
                                        tempAllow = if (on) tempAllow - v else tempAllow + v
                                    }
                            ) {
                                Text("$v", color = if (on) Color.Black else Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempNums.isNotEmpty()) {
                        onChange(
                            groups + CustomGroup(
                                name = tempName.ifBlank { "自定义" },
                                numbers = tempNums,
                                allow = tempAllow,
                                enabled = true
                            )
                        )
                    }
                    showAdd = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ResultTab(
    reds: Set<Int>,
    blues: Set<Int>,
    result: List<List<Int>>,
    filtering: Boolean,
    filterTime: Long,
    onStart: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("红球候选：${reds.size} 个", color = Color.White)
        Text("蓝球保留：${blues.size} 个", color = Color.White)
        if (result.isNotEmpty()) {
            Text("红球通过：${result.size} 注", color = Color(0xFFE0A458))
            Text("理论总组合：约 ${result.size * blues.size} 注", color = Color(0xFF8B93A7), fontSize = 13.sp)
            if (filterTime > 0) {
                Text("耗时：${filterTime} ms", color = Color(0xFF8B93A7), fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onStart,
            enabled = !filtering && reds.size >= 6,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0A458)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                if (filtering) "过滤中，请稍候..." else "开始过滤",
                color = Color(0xFF1A1200),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (result.isNotEmpty()) {
            Text("红球结果（最多显示150注）", color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(result.take(150)) { combo ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2430))) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        combo.forEach { Ball(it, false, 26.dp) }
                    }
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
    val appear = IntArray(34)
    val omit = IntArray(34) { history.size }
    history.forEachIndexed { idx, d ->
        d.reds.forEach { n ->
            appear[n]++
            if (omit[n] == history.size) omit[n] = idx
        }
    }
    val hot = (1..33).sortedByDescending { appear[it] }.take(8)
    val cold = (1..33).sortedByDescending { omit[it] }.take(8)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("基于最近 ${history.size} 期数据", color = Color(0xFF8B93A7)) }
        item { StatCard("热号（出现多）", hot, appear) }
        item { StatCard("冷号 / 遗漏大", cold, omit, true) }
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
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Ball(n, false, 26.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (isOmit) "遗漏 ${values[n]} 期" else "出现 ${values[n]} 次",
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MultiSelectRow(title: String, options: List<Int>, selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    Column {
        Text(title, color = Color(0xFFE0A458), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { v ->
                val on = selected.contains(v)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .background(if (on) Color(0xFFE0A458) else Color(0xFF2A3444), RoundedCornerShape(6.dp))
                        .clickable {
                            onChange(if (on) selected - v else selected + v)
                        }
                ) {
                    Text(
                        "$v",
                        color = if (on) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
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

fun doFilter(
    pool: List<Int>,
    sumMin: Int,
    sumMax: Int,
    spanMin: Int,
    spanMax: Int,
    oddCount: Set<Int>,
    bigCount: Set<Int>,
    primeCount: Set<Int>,
    z1: Set<Int>,
    z2: Set<Int>,
    z3: Set<Int>,
    r0: Set<Int>,
    r1: Set<Int>,
    r2: Set<Int>,
    groups: List<CustomGroup>,
    primes: Set<Int>
): List<List<Int>> {
    if (pool.size < 6) return emptyList()
    val result = mutableListOf<List<Int>>()
    val n = pool.size

    fun dfs(start: Int, path: MutableList<Int>) {
        if (result.size >= 500) return
        if (path.size == 6) {
            val sum = path.sum()
            if (sum !in sumMin..sumMax) return
            val span = path.last() - path.first()
            if (span !in spanMin..spanMax) return
            val odd = path.count { it % 2 == 1 }
            if (odd !in oddCount) return
            val big = path.count { it >= 17 }
            if (big !in bigCount) return
            val prime = path.count { it in primes }
            if (prime !in primeCount) return
            val c1 = path.count { it <= 11 }
            val c2 = path.count { it in 12..22 }
            val c3 = path.count { it >= 23 }
            if (c1 !in z1 || c2 !in z2 || c3 !in z3) return
            val rd0 = path.count { it % 3 == 0 }
            val rd1 = path.count { it % 3 == 1 }
            val rd2 = path.count { it % 3 == 2 }
            if (rd0 !in r0 || rd1 !in r1 || rd2 !in r2) return

            // 自定义组检查（关键修复点）
            for (g in groups) {
                if (!g.enabled || g.numbers.isEmpty()) continue
                val cnt = path.count { it in g.numbers }
                if (cnt !in g.allow) return
            }

            result.add(path.toList())
            return
        }
        for (i in start until n) {
            path.add(pool[i])
            dfs(i + 1, path)
            path.removeAt(path.lastIndex)
        }
    }
    dfs(0, mutableListOf())
    return result
}

suspend fun fetchHistory(limit: Int = 50): List<DrawResult> = withContext(Dispatchers.IO) {
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
        emptyList()
    }
}