package com.xspy8.tapaway

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private data class Cell(val x: Int, val y: Int)
private data class ScoreEntry(val name: String, val score: Int, val level: Int, val difficulty: Int)

private fun parseScores(raw: String): List<ScoreEntry> = raw.lineSequence().mapNotNull { line ->
    val parts = line.split('|')
    if (parts.size != 4) null else ScoreEntry(parts[0], parts[1].toIntOrNull() ?: return@mapNotNull null, parts[2].toIntOrNull() ?: return@mapNotNull null, parts[3].toIntOrNull() ?: 0)
}.sortedByDescending { it.score }.take(10).toList()

private fun encodeScores(scores: List<ScoreEntry>) = scores.joinToString("\n") { "${it.name.replace("|", " ")}|${it.score}|${it.level}|${it.difficulty}" }

private data class ArrowLine(val id: Int, val points: List<Cell>) {
    val direction: Cell get() {
        val a = points[points.lastIndex - 1]; val b = points.last()
        return Cell((b.x - a.x).coerceIn(-1, 1), (b.y - a.y).coerceIn(-1, 1))
    }
    fun cells(): Set<Cell> = buildSet {
        points.zipWithNext().forEach { (a, b) ->
            if (a.x == b.x) for (y in min(a.y, b.y)..max(a.y, b.y)) add(Cell(a.x, y))
            else for (x in min(a.x, b.x)..max(a.x, b.x)) add(Cell(x, a.y))
        }
    }
}

// A denser board lets the level read like a real puzzle instead of four large
// children's arrows. Drawing is small, while ArrowBoard keeps a generous hit box.
private const val GRID_W = 30
private const val GRID_H = 42
private const val LAYOUT_VERSION = 3
private val levelMaps = listOf(
    listOf(
        // Four immediately removable outer arrows.
        listOf(1 to 1, 10 to 1),
        listOf(10 to 2, 10 to 10),
        listOf(9 to 10, 1 to 10),
        listOf(1 to 9, 1 to 2),
        // Each inner arrow is deliberately blocked by one outer arrow.
        listOf(5 to 3, 6 to 3, 6 to 4, 8 to 4),
        listOf(8 to 5, 7 to 5, 7 to 8),
        listOf(6 to 8, 6 to 7, 3 to 7),
        listOf(3 to 6, 4 to 6, 4 to 5, 3 to 5, 3 to 3),
        // Tempting decoys: both look open nearby but are blocked farther away.
        listOf(9 to 5, 9 to 8),
        listOf(2 to 4, 2 to 7)
    ),
    listOf(
        listOf(2 to 1, 10 to 1),
        listOf(10 to 2, 10 to 9),
        listOf(9 to 10, 1 to 10),
        listOf(1 to 9, 1 to 2),
        listOf(4 to 3, 8 to 3),
        listOf(8 to 4, 8 to 5, 7 to 5, 7 to 8),
        listOf(6 to 8, 3 to 8),
        listOf(3 to 7, 4 to 7, 4 to 4, 3 to 4, 3 to 3),
        listOf(9 to 4, 9 to 8),
        listOf(2 to 3, 2 to 7),
        listOf(5 to 5, 6 to 5)
    )
)

private fun makeLevel(level: Int, difficulty: Int): List<ArrowLine> {
    // Build in solution order. A candidate may only be added when its own head has
    // a clear ray through everything already placed. Later candidates are encouraged
    // to cross those rays, so playing in reverse insertion order always solves the
    // board while still creating deep, non-trivial dependency chains.
    val random = Random(0x4D454F57 + level * 7919 + difficulty * 104729)
    val directions = listOf(Cell(1, 0), Cell(-1, 0), Cell(0, 1), Cell(0, -1))
    val targetCount = when {
        difficulty == 1 -> min(58, 42 + level * 2)
        level == 0 -> 18
        else -> min(46, 30 + level * 2)
    }
    val minLength = if (difficulty == 1) 8 else 5
    val maxLength = if (difficulty == 1) 19 else 15
    val result = mutableListOf<ArrowLine>()
    val occupied = mutableSetOf<Cell>()

    fun inside(c: Cell) = c.x in 1 until GRID_W - 1 && c.y in 1 until GRID_H - 1
    fun rayCells(head: Cell, direction: Cell): List<Cell> {
        val cells = mutableListOf<Cell>()
        var p = Cell(head.x + direction.x, head.y + direction.y)
        while (p.x in 0 until GRID_W && p.y in 0 until GRID_H) {
            cells += p
            p = Cell(p.x + direction.x, p.y + direction.y)
        }
        return cells
    }
    fun compressed(cells: List<Cell>): List<Cell> {
        if (cells.size <= 2) return cells
        val points = mutableListOf(cells.first())
        for (i in 1 until cells.lastIndex) {
            val before = Cell(cells[i].x - cells[i - 1].x, cells[i].y - cells[i - 1].y)
            val after = Cell(cells[i + 1].x - cells[i].x, cells[i + 1].y - cells[i].y)
            if (before != after) points += cells[i]
        }
        points += cells.last()
        return points
    }

    repeat(targetCount) { id ->
        var best: Pair<List<Cell>, Int>? = null
        repeat(if (difficulty == 1) 90 else 65) candidate@{
            val head = Cell(random.nextInt(1, GRID_W - 1), random.nextInt(1, GRID_H - 1))
            if (head in occupied) return@candidate
            val exits = directions.shuffled(random).filter { direction ->
                val behind = Cell(head.x - direction.x, head.y - direction.y)
                val behindTwo = Cell(head.x - direction.x * 2, head.y - direction.y * 2)
                inside(behind) && inside(behindTwo) && behind !in occupied && behindTwo !in occupied &&
                    rayCells(head, direction).none { it in occupied }
            }
            if (exits.isEmpty()) return@candidate
            val exit = exits.first()
            val reverseCells = mutableListOf(
                head,
                Cell(head.x - exit.x, head.y - exit.y),
                Cell(head.x - exit.x * 2, head.y - exit.y * 2)
            )
            val wanted = random.nextInt(minLength, maxLength + 1)
            var previousStep = Cell(-exit.x, -exit.y)
            while (reverseCells.size < wanted) {
                val current = reverseCells.last()
                val choices = directions.filter { step ->
                    val next = Cell(current.x + step.x, current.y + step.y)
                    inside(next) && next !in occupied && next !in reverseCells
                }
                if (choices.isEmpty()) break
                // Frequent turns produce the compact maze-like silhouettes in the
                // reference levels; occasional straight runs keep paths readable.
                val turning = choices.filter { it != previousStep && it != Cell(-previousStep.x, -previousStep.y) }
                val step = if (turning.isNotEmpty() && random.nextFloat() < .68f) turning.random(random) else choices.random(random)
                reverseCells += Cell(current.x + step.x, current.y + step.y)
                previousStep = step
            }
            if (reverseCells.size < minLength) return@candidate
            val cells = reverseCells.asReversed()
            val candidateSet = cells.toSet()
            val blockedOlder = result.count { older -> rayCells(older.points.last(), older.direction).any { it in candidateSet } }
            val turns = compressed(cells).size - 2
            val centreBonus = cells.count { abs(it.x - GRID_W / 2) < GRID_W / 5 && abs(it.y - GRID_H / 2) < GRID_H / 5 }
            val score = blockedOlder * 1000 + turns * 16 + cells.size * 3 + centreBonus * 5 + random.nextInt(12)
            if (score > (best?.second ?: Int.MIN_VALUE)) best = cells to score
        }
        val cells = best?.first ?: return@repeat
        val line = ArrowLine(id, compressed(cells))
        result += line
        occupied += cells
    }
    return result
}

private fun startingLives(level: Int) = when {
    level >= 4 -> 1
    level >= 1 -> 2
    else -> 3
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TapAwayArrows() }
    }
}

@Composable
private fun TapAwayArrows() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tap_arrows", 0) }
    var level by remember { mutableIntStateOf(prefs.getInt("level", 0)) }
    var selectedDifficulty by remember { mutableIntStateOf(prefs.getInt("selected_difficulty", 0)) }
    var gameDifficulty by remember { mutableIntStateOf(prefs.getInt("game_difficulty", 0)) }
    val initialLevel = remember { makeLevel(level, gameDifficulty) }
    val savedIds = remember {
        if (prefs.getInt("layout_version", 0) != LAYOUT_VERSION) null
        else prefs.getString("remaining_ids", null)?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet()
    }
    val arrows = remember { mutableStateListOf<ArrowLine>().apply { addAll(if (savedIds == null) initialLevel else initialLevel.filter { it.id in savedIds }) } }
    fun livesForLevel(value: Int) = if (gameDifficulty == 1) 1 else startingLives(value) + 1
    var lives by remember { mutableIntStateOf(prefs.getInt("current_lives", livesForLevel(level))) }
    var moves by remember { mutableIntStateOf(prefs.getInt("current_moves", 0)) }
    var score by remember { mutableIntStateOf(prefs.getInt("current_score", 0)) }
    var showGameOver by remember { mutableStateOf(prefs.getBoolean("game_over", false)) }
    var scoreSubmitted by remember { mutableStateOf(prefs.getBoolean("score_submitted", false)) }
    var playerName by remember { mutableStateOf(prefs.getString("last_player_name", "") ?: "") }
    var leaderboard by remember { mutableStateOf(parseScores(prefs.getString("leaderboard", "") ?: "")) }
    var musicVolume by remember { mutableFloatStateOf(prefs.getFloat("music_volume", .42f)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    var showOptions by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var inMenu by remember { mutableStateOf(true) }
    var hintCount by remember { mutableIntStateOf(prefs.getInt("hints", 2)) }
    var usedHintThisLevel by remember { mutableStateOf(prefs.getBoolean("used_hint", false)) }
    var won by remember { mutableStateOf(prefs.getBoolean("level_won", false)) }
    var movingId by remember { mutableStateOf<Int?>(null) }
    var queuedId by remember { mutableStateOf<Int?>(null) }
    var escaping by remember { mutableStateOf(false) }
    var motion by remember { mutableFloatStateOf(0f) }
    var hintId by remember { mutableStateOf<Int?>(null) }
    var catCelebrating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val ink = Color(0xFF1D2747)
    val blue = Color(0xFF2998EE)
    val music = remember {
        MediaPlayer.create(context, com.xspy8.tapaway.R.raw.meow_breeze).apply {
            isLooping = true
            val initialVolume = if (soundEnabled) musicVolume * .34f else 0f
            setVolume(initialVolume, initialVolume)
            start()
        }
    }
    LaunchedEffect(musicVolume, soundEnabled) {
        val audibleVolume = if (soundEnabled) musicVolume * .34f else 0f
        music.setVolume(audibleVolume, audibleVolume)
        prefs.edit().putFloat("music_volume", musicVolume).putBoolean("sound_enabled", soundEnabled).apply()
    }
    DisposableEffect(Unit) { onDispose { music.stop(); music.release() } }
    val remainingIds = arrows.joinToString(",") { it.id.toString() }
    LaunchedEffect(level, remainingIds, lives, moves, score, gameDifficulty, usedHintThisLevel, won, showGameOver, scoreSubmitted) {
        prefs.edit()
            .putInt("level", level)
            .putInt("game_difficulty", gameDifficulty)
            .putString("remaining_ids", remainingIds)
            .putInt("current_lives", lives)
            .putInt("current_moves", moves)
            .putInt("current_score", score)
            .putBoolean("used_hint", usedHintThisLevel)
            .putBoolean("level_won", won)
            .putBoolean("game_over", showGameOver)
            .putBoolean("score_submitted", scoreSubmitted)
            .putInt("layout_version", LAYOUT_VERSION)
            .apply()
    }

    fun tone(ok: Boolean) {
        if (!soundEnabled) return
        ToneGenerator(AudioManager.STREAM_MUSIC, 28).apply {
            startTone(if (ok) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_NACK, 90)
            scope.launch { delay(120); release() }
        }
    }

    fun canEscape(line: ArrowLine): Boolean {
        val others = arrows.filter { it.id != line.id }.flatMap { it.cells() }.toSet()
        // A bent line behaves like a rope, not a rigid polyomino. Its body retracts
        // through the route it already occupies, so only the new cells in front of
        // the arrowhead can collide with another line.
        val head = line.points.last()
        for (step in 1..max(GRID_W, GRID_H) * 2) {
            val next = Cell(head.x + line.direction.x * step, head.y + line.direction.y * step)
            if (next.x !in 0 until GRID_W || next.y !in 0 until GRID_H) return true
            if (next in others) return false
        }
        return false
    }

    fun reset() {
        arrows.clear(); arrows.addAll(makeLevel(level, gameDifficulty)); lives = livesForLevel(level); moves = 0; won = false; catCelebrating = false; showGameOver = false; scoreSubmitted = false; movingId = null; queuedId = null; escaping = false; motion = 0f; usedHintThisLevel = false
    }

    BackHandler(enabled = !inMenu || showOptions || showAbout) {
        when {
            showAbout -> showAbout = false
            showOptions -> showOptions = false
            else -> inMenu = true
        }
    }

    lateinit var attemptLine: (ArrowLine) -> Unit
    fun playQueued() {
        val id = queuedId
        queuedId = null
        if (id != null) arrows.firstOrNull { it.id == id }?.let { next ->
            scope.launch { delay(45); attemptLine(next) }
        }
    }
    attemptLine = attempt@{ line ->
        if (movingId != null) {
            queuedId = line.id
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            return@attempt
        }
        hintId = null; moves++
        if (canEscape(line)) {
            tone(true); escaping = true; movingId = line.id
            scope.launch {
                val acceleration = .15f
                val speed = 1f / (1f - acceleration)
                val durationNanos = 1_100_000_000L
                var startedAt = 0L
                var t: Float
                do {
                    val now = withFrameNanos { it }
                    if (startedAt == 0L) startedAt = now
                    t = ((now - startedAt).toFloat() / durationNanos).coerceIn(0f, 1f)
                    motion = when {
                        t < acceleration -> speed * t * t / (2f * acceleration)
                        t > 1f - acceleration -> 1f - speed * (1f - t) * (1f - t) / (2f * acceleration)
                        else -> speed * (t - acceleration / 2f)
                    }
                } while (t < 1f)
                arrows.remove(line)
                score += if (gameDifficulty == 1) 200 else 100
                movingId = null; escaping = false; motion = 0f
                if (arrows.isEmpty()) {
                    queuedId = null
                    score += (level + 1) * if (gameDifficulty == 1) 1000 else 500
                    if (!usedHintThisLevel) {
                        hintCount++
                        prefs.edit().putInt("hints", hintCount).apply()
                    }
                    catCelebrating = true
                    delay(1_150)
                    won = true
                } else playQueued()
            }
        } else {
            tone(false); lives = max(0, lives - 1); escaping = false; movingId = line.id
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                val frames = listOf(.018f, .038f, .055f, .065f, .055f, .032f, .008f, -.012f, -.018f, -.010f, 0f)
                for (v in frames) { motion = v; delay(32) }
                movingId = null
                if (lives == 0) {
                    queuedId = null
                    delay(180)
                    showGameOver = true
                    scoreSubmitted = false
                } else playQueued()
            }
        }
    }

    val menuAnimation = rememberInfiniteTransition(label = "menu_cat")
    val catScale by menuAnimation.animateFloat(
        initialValue = .94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "cat_scale"
    )
    val catRoll by menuAnimation.animateFloat(
        initialValue = -7f, targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "cat_roll"
    )

    Surface(Modifier.fillMaxSize(), color = Color(0xFFFDFEFF)) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(20.dp))
                Text("Meow Tap Away\n2D", color = Color(0xFF46506C), fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", color = blue, fontSize = 36.sp, modifier = Modifier.clip(CircleShape).clickable { inMenu = true }.padding(horizontal = 10.dp))
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Level ${level + 1}", color = Color(0xFF222222), fontSize = 14.sp)
                        Text(if (gameDifficulty == 1) "CỰC KHÓ" else if (level == 0) "Làm quen" else if (level < 4) "Khó" else "Mèo cao thủ", color = if (gameDifficulty == 1) Color(0xFFFF5967) else blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        Text("♥ ".repeat(lives).trim(), color = Color(0xFFFF4352), fontSize = 28.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(13.dp)).background(Color(0xFFF0F7FD)).padding(horizontal = 10.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
                        Text("★ $score", color = blue, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(8.dp))
                    CircleAction("⚙", blue) { showOptions = true }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(490.dp), contentAlignment = Alignment.Center) {
                    ArrowBoard(arrows, movingId, queuedId, escaping, motion, hintId, ink, blue, attemptLine)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomAction("💡", hintCount.toString()) {
                        if (hintCount > 0) arrows.firstOrNull { canEscape(it) }?.let {
                            hintCount--; usedHintThisLevel = true
                            prefs.edit().putInt("hints", hintCount).apply()
                            hintId = it.id; scope.launch { delay(1700); hintId = null }
                        }
                    }
                    CatMascot(celebrating = catCelebrating, startled = movingId != null && !escaping)
                    BottomAction(if (soundEnabled) "♪ ON" else "♪ OFF", null) { soundEnabled = !soundEnabled }
                }
                Text("$moves lượt", color = Color(0xFF99A1B3), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }

            AnimatedVisibility(won, enter = fadeIn() + scaleIn(initialScale = .8f)) {
                Box(Modifier.fillMaxSize().background(Color(0xAA1D2747)), contentAlignment = Alignment.Center) {
                    Column(Modifier.fillMaxWidth(.86f).clip(RoundedCornerShape(30.dp)).background(Color.White).padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("★", color = Color(0xFFFFC94F), fontSize = 66.sp)
                        Text("Tuyệt vời!", color = ink, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("Bạn đã gỡ hết các đường mũi tên.", color = Color(0xFF7C8499), modifier = Modifier.padding(10.dp))
                        Button(onClick = {
                            level++; prefs.edit().putInt("level", level).apply(); arrows.clear(); arrows.addAll(makeLevel(level, gameDifficulty)); lives = livesForLevel(level); moves = 0; usedHintThisLevel = false; catCelebrating = false; won = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = blue), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("Màn tiếp theo  →", fontWeight = FontWeight.Bold, modifier = Modifier.padding(5.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(showGameOver && !inMenu, enter = fadeIn() + scaleIn(initialScale = .88f)) {
                Box(Modifier.fillMaxSize().background(Color(0xCC1D2747)).clickable { }, contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.fillMaxWidth(.9f).clip(RoundedCornerShape(28.dp)).background(Color.White).padding(25.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HẾT TIM!", color = Color(0xFFFF5967), fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("${score} điểm", color = ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        Text("Đạt Level ${level + 1}  •  ${if (gameDifficulty == 1) "Khó" else "Dễ"}", color = Color(0xFF6E7990), fontSize = 14.sp)
                        Spacer(Modifier.height(18.dp))

                        if (!scoreSubmitted) {
                            OutlinedTextField(
                                value = playerName,
                                onValueChange = { if (it.length <= 18) playerName = it.replace("|", "") },
                                label = { Text("Tên người chơi") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(11.dp))
                            Button(
                                onClick = {
                                    val cleanName = playerName.trim()
                                    if (cleanName.isNotEmpty()) {
                                        val entry = ScoreEntry(cleanName, score, level + 1, gameDifficulty)
                                        leaderboard = (leaderboard + entry).sortedByDescending { it.score }.take(10)
                                        prefs.edit().putString("last_player_name", cleanName).putString("leaderboard", encodeScores(leaderboard)).apply()
                                        scoreSubmitted = true
                                    }
                                },
                                enabled = playerName.trim().isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = blue), shape = RoundedCornerShape(15.dp)
                            ) { Text("Xác nhận", fontWeight = FontWeight.Black, modifier = Modifier.padding(4.dp)) }
                        } else {
                            Text("BẢNG ĐIỂM", color = blue, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Spacer(Modifier.height(9.dp))
                            leaderboard.take(5).forEachIndexed { index, entry ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${index + 1}", color = if (index == 0) Color(0xFFFFA928) else Color(0xFF9AA3B5), fontWeight = FontWeight.Black, modifier = Modifier.width(25.dp))
                                    Text(entry.name, color = ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("${entry.score}", color = blue, fontWeight = FontWeight.Black)
                                    Text("  LV${entry.level}", color = Color(0xFF7D879B), fontSize = 12.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                                }
                            }
                            Spacer(Modifier.height(13.dp))
                            Button(onClick = {
                                gameDifficulty = selectedDifficulty
                                level = 0
                                arrows.clear(); arrows.addAll(makeLevel(0, selectedDifficulty))
                                lives = if (selectedDifficulty == 1) 1 else startingLives(0) + 1
                                moves = 0; score = 0; won = false; catCelebrating = false; showGameOver = false; scoreSubmitted = false; usedHintThisLevel = false
                                queuedId = null; movingId = null
                                prefs.edit().putInt("level", 0).putInt("game_difficulty", selectedDifficulty).remove("remaining_ids").apply()
                            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF36C79A)), shape = RoundedCornerShape(15.dp)) {
                                Text("Chơi mới  •  ${if (selectedDifficulty == 1) "Khó" else "Dễ"}", fontWeight = FontWeight.Black, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(inMenu, enter = fadeIn()) {
                Box(Modifier.fillMaxSize().background(Color(0xFFF7FBFF)).clickable { }, contentAlignment = Alignment.Center) {
                    Column(Modifier.fillMaxWidth(.86f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(155.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("←", color = Color(0xFF38C9A5), fontSize = 44.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterStart).rotate(-catRoll))
                            Image(
                                painter = painterResource(com.xspy8.tapaway.R.drawable.meow_tap_away_icon),
                                contentDescription = "Mèo và các mũi tên",
                                modifier = Modifier.size(142.dp).scale(catScale).rotate(catRoll).clip(RoundedCornerShape(30.dp))
                            )
                            Text("→", color = Color(0xFFFFA12B), fontSize = 44.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterEnd).rotate(catRoll))
                        }
                        Text("Meow Tap Away 2D", color = ink, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text("Gỡ rối từng đường, dẫn mèo về nhà!", color = Color(0xFF7C8499), fontSize = 14.sp, modifier = Modifier.padding(top = 7.dp, bottom = 30.dp))

                        Button(onClick = { inMenu = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = blue), shape = RoundedCornerShape(17.dp)) {
                            Text("Chơi tiếp  •  Level ${level + 1}", fontWeight = FontWeight.Black, modifier = Modifier.padding(7.dp))
                        }
                        Spacer(Modifier.height(11.dp))
                        Button(onClick = {
                            gameDifficulty = selectedDifficulty
                            level = 0
                            arrows.clear(); arrows.addAll(makeLevel(0, selectedDifficulty))
                            lives = if (selectedDifficulty == 1) 1 else startingLives(0) + 1
                            moves = 0; score = 0; won = false; catCelebrating = false; showGameOver = false; scoreSubmitted = false; usedHintThisLevel = false; queuedId = null; movingId = null
                            prefs.edit().putInt("level", 0).putInt("game_difficulty", selectedDifficulty).remove("remaining_ids").apply()
                            inMenu = false
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ink), shape = RoundedCornerShape(17.dp)) {
                            Text("Chơi mới", fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                        }
                        Spacer(Modifier.height(11.dp))
                        Text("CHỌN ĐỘ KHÓ CHO VÁN MỚI", color = Color(0xFF8A95AA), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Dễ", "Khó").forEachIndexed { index, label ->
                                Button(
                                    onClick = { selectedDifficulty = index; prefs.edit().putInt("selected_difficulty", index).apply() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedDifficulty == index) if (index == 0) Color(0xFF36C79A) else Color(0xFFFF5967) else Color(0xFFECEFF5),
                                        contentColor = if (selectedDifficulty == index) Color.White else Color(0xFF69758D)
                                    ), shape = RoundedCornerShape(15.dp)
                                ) { Text(label, fontWeight = FontWeight.Black, modifier = Modifier.padding(3.dp)) }
                            }
                        }
                        Spacer(Modifier.height(11.dp))
                        Button(onClick = { showAbout = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F2FA), contentColor = Color(0xFF53617E)), shape = RoundedCornerShape(17.dp)) {
                            Text("Giới thiệu", fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                        }
                        Text("💡 $hintCount gợi ý", color = Color(0xFF97A0B2), fontSize = 13.sp, modifier = Modifier.padding(top = 22.dp))
                    }
                }
            }

            AnimatedVisibility(showOptions && !showAbout, enter = fadeIn() + scaleIn(initialScale = .92f)) {
                Box(Modifier.fillMaxSize().background(Color(0x991D2747)).clickable { }, contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.fillMaxWidth(.86f).clip(RoundedCornerShape(28.dp)).background(Color.White).padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Tùy chọn", color = ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(24.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (musicVolume == 0f) "♪×" else "♪", color = blue, fontSize = 25.sp)
                            Slider(value = musicVolume, onValueChange = { musicVolume = it }, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                            Text("${(musicVolume * 100).toInt()}%", color = Color(0xFF7C8499), fontSize = 12.sp, modifier = Modifier.width(38.dp))
                        }
                        Text("Âm lượng nhạc nền", color = Color(0xFF7C8499), fontSize = 13.sp)
                        Spacer(Modifier.height(22.dp))
                        Button(onClick = { showAbout = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F7FD), contentColor = blue), shape = RoundedCornerShape(16.dp)) {
                            Text("Giới thiệu", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { showOptions = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = blue), shape = RoundedCornerShape(16.dp)) {
                            Text("Đóng", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(showAbout, enter = fadeIn() + scaleIn(initialScale = .92f)) {
                Box(Modifier.fillMaxSize().background(Color(0xAA1D2747)).clickable { }, contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.fillMaxWidth(.86f).clip(RoundedCornerShape(28.dp)).background(Color.White).padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(com.xspy8.tapaway.R.drawable.cat_center),
                            contentDescription = "Chú mèo Meow Tap Away 2D",
                            modifier = Modifier.size(78.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text("MEOW TAP AWAY 2D", color = ink, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Image(
                            painter = painterResource(com.xspy8.tapaway.R.drawable.asl_logo),
                            contentDescription = "ASLDEV.VN",
                            modifier = Modifier.fillMaxWidth(.62f).height(78.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Copyright: © 2026 ", color = Color(0xFF657087), fontSize = 14.sp)
                            Text(
                                "ASLDEV.VN", color = blue, fontSize = 14.sp, fontWeight = FontWeight.Black,
                                modifier = Modifier.clip(RoundedCornerShape(7.dp)).clickable {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://asldev.vn")))
                                }.padding(horizontal = 3.dp, vertical = 5.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { showAbout = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = blue), shape = RoundedCornerShape(16.dp)) {
                            Text("Quay lại", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatMascot(celebrating: Boolean, startled: Boolean) {
    val jump = remember { Animatable(0f) }
    val idle = rememberInfiniteTransition(label = "cat_idle")
    val breathing by idle.animateFloat(
        initialValue = .98f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "cat_breath"
    )
    val startledScale by animateFloatAsState(if (startled) .88f else 1f, tween(120), label = "cat_startled")
    LaunchedEffect(celebrating) {
        if (celebrating) {
            repeat(2) {
                jump.animateTo(1f, tween(190))
                jump.animateTo(0f, tween(230))
            }
        } else jump.snapTo(0f)
    }
    Box(Modifier.size(82.dp), contentAlignment = Alignment.Center) {
        if (celebrating) {
            Text("♥  ✦  ♥", color = Color(0xFFFF5967), fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopCenter))
        }
        Image(
            painter = painterResource(com.xspy8.tapaway.R.drawable.cat_center),
            contentDescription = "Mèo con đang chờ được giải cứu",
            modifier = Modifier
                .size(60.dp)
                .offset(y = (-jump.value * 34f).dp)
                .scale(breathing * startledScale * (1f + jump.value * .12f))
                .rotate(if (startled) -7f else jump.value * 9f)
        )
    }
}

@Composable
private fun ArrowBoard(arrows: List<ArrowLine>, movingId: Int?, queuedId: Int?, escaping: Boolean, motion: Float, hintId: Int?, ink: Color, blue: Color, onTap: (ArrowLine) -> Unit) {
    Canvas(
        Modifier.fillMaxWidth().height(490.dp).clip(RoundedCornerShape(10.dp))
            .pointerInput(arrows, movingId) {
                detectTapGestures { tap ->
                    // PointerInput exposes integer pixel dimensions. Keep this math
                    // floating-point so hit coordinates exactly match DrawScope;
                    // integer division used to drift by many pixels near the bottom.
                    val unit = min(size.width.toFloat() / GRID_W, size.height.toFloat() / GRID_H)
                    val boardLeft = (size.width.toFloat() - GRID_W * unit) / 2f
                    val boardTop = (size.height.toFloat() - GRID_H * unit) / 2f
                    fun distance(p: Offset, a: Offset, b: Offset): Float {
                        val dx = b.x - a.x; val dy = b.y - a.y
                        val length = dx * dx + dy * dy
                        val t = if (length == 0f) 0f else (((p.x-a.x)*dx+(p.y-a.y)*dy)/length).coerceIn(0f,1f)
                        return hypot(p.x - (a.x+t*dx), p.y - (a.y+t*dy))
                    }
                    arrows.minByOrNull { line ->
                        line.points.zipWithNext().minOf { (a,b) ->
                            distance(tap, Offset(boardLeft+(a.x+.5f)*unit, boardTop+(a.y+.5f)*unit), Offset(boardLeft+(b.x+.5f)*unit, boardTop+(b.y+.5f)*unit))
                        }
                    }?.let { candidate ->
                        val near = candidate.points.zipWithNext().minOf { (a,b) ->
                            distance(tap, Offset(boardLeft+(a.x+.5f)*unit, boardTop+(a.y+.5f)*unit), Offset(boardLeft+(b.x+.5f)*unit, boardTop+(b.y+.5f)*unit))
                        }
                        // The visible stroke stays slim, but its invisible touch target is
                        // almost two grid cells wide for reliable one-handed phone play.
                        if (near < unit * 2.05f) onTap(candidate)
                    }
                }
            }
    ) {
        val unit = min(size.width / GRID_W, size.height / GRID_H)
        val left = (size.width - GRID_W * unit) / 2f
        val top = (size.height - GRID_H * unit) / 2f
        val candyColors = listOf(
            Color(0xFF62D9B0), Color(0xFFFFC857), Color(0xFFFF826E),
            Color(0xFF75C7F0), Color(0xFFB99BEA), Color(0xFFFF9FC3)
        )
        arrows.forEach { line ->
            val direction = line.direction
            val isMoving = movingId == line.id
            // Escaping lines behave like a rope: the head advances, while the tail is
            // progressively trimmed through every old corner. Blocked lines stay rigid.
            val shift = if (isMoving && !escaping) Offset(direction.x * motion * size.width, direction.y * motion * size.width) else Offset.Zero
            val color = when {
                hintId == line.id -> Color(0xFFFF3B4E)
                isMoving && motion > 0f -> blue
                queuedId == line.id -> Color(0xFF36C79A)
                else -> candyColors[line.id % candyColors.size]
            }
            val endCell = line.points.last()
            val segmentLengths = line.points.zipWithNext().map { (a, b) -> (abs(b.x - a.x) + abs(b.y - a.y)).toFloat() }
            val ropeLength = segmentLengths.sum()
            val travel = if (isMoving && escaping) motion * (ropeLength + max(GRID_W, GRID_H) * 1.35f) else 0f
            val visiblePoints = mutableListOf<Offset>()
            if (isMoving && escaping) {
                if (travel < ropeLength) {
                    var remainingTrim = travel
                    var startSegment = 0
                    while (startSegment < segmentLengths.size && remainingTrim >= segmentLengths[startSegment]) {
                        remainingTrim -= segmentLengths[startSegment]
                        startSegment++
                    }
                    if (startSegment < segmentLengths.size) {
                        val a = line.points[startSegment]; val b = line.points[startSegment + 1]
                        val length = segmentLengths[startSegment]
                        val ratio = if (length == 0f) 0f else remainingTrim / length
                        visiblePoints += Offset(a.x + (b.x - a.x) * ratio, a.y + (b.y - a.y) * ratio)
                        for (i in startSegment + 1..line.points.lastIndex) visiblePoints += Offset(line.points[i].x.toFloat(), line.points[i].y.toFloat())
                    }
                    visiblePoints += Offset(endCell.x + direction.x * travel, endCell.y + direction.y * travel)
                } else {
                    val tailTravel = travel - ropeLength
                    visiblePoints += Offset(endCell.x + direction.x * tailTravel, endCell.y + direction.y * tailTravel)
                    visiblePoints += Offset(endCell.x + direction.x * travel, endCell.y + direction.y * travel)
                }
            } else {
                visiblePoints += line.points.map { Offset(it.x.toFloat(), it.y.toFloat()) }
            }
            val path = Path()
            visiblePoints.forEachIndexed { index, cell ->
                val p = Offset(left + (cell.x + .5f) * unit, top + (cell.y + .5f) * unit) + shift
                if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            // A tiny soft shadow and slimmer navy rim keep dense levels readable
            // without losing the rounded pastel candy identity.
            val shadowPath = Path()
            visiblePoints.forEachIndexed { index, cell ->
                val p = Offset(left + (cell.x + .5f) * unit + unit * .10f, top + (cell.y + .5f) * unit + unit * .12f) + shift
                if (index == 0) shadowPath.moveTo(p.x, p.y) else shadowPath.lineTo(p.x, p.y)
            }
            drawPath(shadowPath, ink.copy(alpha = .13f), style = Stroke(width = unit * .34f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, ink.copy(alpha = .84f), style = Stroke(width = unit * .30f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, color, style = Stroke(width = unit * .19f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, Color.White.copy(alpha = .38f), style = Stroke(width = unit * .048f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val animatedHead = visiblePoints.last()
            val end = Offset(left+(animatedHead.x+.5f)*unit, top+(animatedHead.y+.5f)*unit) + shift
            val back = Offset(-direction.x.toFloat(), -direction.y.toFloat())
            val side = Offset(-direction.y.toFloat(), direction.x.toFloat())
            // Two open chevrons stay legible after the board is made denser. The
            // dark under-stroke preserves the candy outline used by every rope.
            listOf(0f to .50f, .68f to .40f).forEach { (inset, wingLength) ->
                val tip = end + back * (unit * inset)
                val chevron = Path().apply {
                    moveTo(tip.x + (back.x + side.x * .72f) * unit * wingLength, tip.y + (back.y + side.y * .72f) * unit * wingLength)
                    lineTo(tip.x, tip.y)
                    lineTo(tip.x + (back.x - side.x * .72f) * unit * wingLength, tip.y + (back.y - side.y * .72f) * unit * wingLength)
                }
                drawPath(chevron, ink.copy(alpha = .88f), style = Stroke(width = unit * .30f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(chevron, color, style = Stroke(width = unit * .18f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable private fun CircleAction(label: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFF3F6FA)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Text(label, color = color, fontSize = 20.sp) }
}

@Composable private fun BottomAction(label: String, badge: String?, onClick: () -> Unit) {
    Box {
        Box(Modifier.size(62.dp).clip(CircleShape).background(Color.White).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Text(label, color = Color(0xFF2998EE), fontSize = if (label.length > 2) 12.sp else 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
        badge?.let { Box(Modifier.align(Alignment.TopEnd).size(21.dp).clip(CircleShape).background(Color(0xFF2998EE)), contentAlignment = Alignment.Center) { Text(it, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
    }
}
// AdMob Interstitial Ad Integration
private var interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd? = null

private fun loadInterstitialAd(context: android.content.Context) {
    val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
    com.google.android.gms.ads.interstitial.InterstitialAd.load(
        context,
        "ca-app-pub-7664427102906771/6690346986",
        adRequest,
        object : com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: com.google.android.gms.ads.interstitial.InterstitialAd) {
                interstitialAd = ad
            }
            override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {
                interstitialAd = null
            }
        }
    )
}

private fun showInterstitialAd(activity: android.app.Activity, onAdDismissed: () -> Unit) {
    if (interstitialAd != null) {
        interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onAdDismissed()
                loadInterstitialAd(activity)
            }
            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                onAdDismissed()
            }
        }
        interstitialAd?.show(activity)
    } else {
        onAdDismissed()
        loadInterstitialAd(activity)
    }
}
