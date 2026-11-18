package com.mediadock.app.feature.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mediadock.app.core.designsystem.components.BrandButton
import com.mediadock.app.core.designsystem.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs

// Game state has no dependency on downloads, networking, or UI frame rate.
data class SnakeCell(val x: Int, val y: Int)
enum class SnakeDirection(val x: Int, val y: Int) { UP(0,-1), DOWN(0,1), LEFT(-1,0), RIGHT(1,0) }
data class SnakeState(
    val body: List<SnakeCell> = listOf(SnakeCell(6,7), SnakeCell(5,7), SnakeCell(4,7)),
    val direction: SnakeDirection = SnakeDirection.RIGHT,
    val food: SnakeCell = SnakeCell(10,7), val score: Int = 0,
    val over: Boolean = false, val won: Boolean = false
)
object SnakeRules {
    const val SIZE = 14
    fun turn(current: SnakeDirection, requested: SnakeDirection) = if (current.x + requested.x == 0 && current.y + requested.y == 0) current else requested
    fun step(state: SnakeState, requested: SnakeDirection = state.direction, choose: (List<SnakeCell>) -> SnakeCell = { it.random() }): SnakeState {
        if (state.over) return state
        val direction = turn(state.direction, requested)
        val head = SnakeCell(state.body.first().x + direction.x, state.body.first().y + direction.y)
        val grows = head == state.food
        val obstacle = if (grows) state.body else state.body.dropLast(1)
        if (head.x !in 0 until SIZE || head.y !in 0 until SIZE || head in obstacle) return state.copy(direction = direction, over = true)
        val body = listOf(head) + if (grows) state.body else state.body.dropLast(1)
        val free = if (grows) (0 until SIZE).flatMap { y -> (0 until SIZE).map { x -> SnakeCell(x,y) } }.filterNot { it in body } else emptyList()
        return state.copy(body = body, direction = direction, score = state.score + if (grows) 1 else 0,
            food = if (grows && free.isNotEmpty()) choose(free) else state.food,
            over = grows && free.isEmpty(), won = grows && free.isEmpty())
    }
}

@Composable fun SnakeGame(modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(SnakeState()) }
    var requested by remember { mutableStateOf(SnakeDirection.RIGHT) }
    var running by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("mediadock_snake", android.content.Context.MODE_PRIVATE) }
    var best by remember { mutableStateOf(preferences.getInt("best", 0)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) running = false }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(running) {
        while (running) {
            delay((210L - state.score * 4).coerceAtLeast(100L))
            state = SnakeRules.step(state, requested)
            requested = state.direction
            if (state.score > best) { best = state.score; preferences.edit().putInt("best", best).apply() }
            if (state.over) running = false
        }
    }
    fun steer(direction: SnakeDirection) { requested = SnakeRules.turn(state.direction, direction) }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Score ${state.score} · Best $best", style = MaterialTheme.typography.labelLarge)
        Canvas(Modifier.widthIn(max = 224.dp).fillMaxWidth().aspectRatio(1f)
            .semantics { contentDescription = "Snake game board" }
            .pointerInput(state.direction) {
                var drag = Offset.Zero
                detectDragGestures(onDragStart = { drag = Offset.Zero }, onDragEnd = {
                    if (abs(drag.x) + abs(drag.y) > 20f) steer(if (abs(drag.x) > abs(drag.y)) { if (drag.x > 0) SnakeDirection.RIGHT else SnakeDirection.LEFT } else { if (drag.y > 0) SnakeDirection.DOWN else SnakeDirection.UP })
                }) { change, amount -> change.consume(); drag += amount }
            }) {
            drawRect(BrandDarkSurface)
            val cell = size.width / SnakeRules.SIZE
            repeat(SnakeRules.SIZE + 1) { index ->
                drawLine(BrandWhite.copy(alpha = .08f), Offset(index * cell,0f),Offset(index * cell,size.height))
                drawLine(BrandWhite.copy(alpha = .08f), Offset(0f,index * cell),Offset(size.width,index * cell))
            }
            state.body.forEachIndexed { index, point -> drawRoundRect(if (index == 0) BrandWhite else BrandCoralOrange, Offset(point.x * cell + 1,point.y * cell + 1), Size(cell - 2,cell - 2), androidx.compose.ui.geometry.CornerRadius(3f,3f)) }
            if (!state.won) drawCircle(BrandCoralOrangeLight, cell * .35f, Offset((state.food.x + .5f) * cell, (state.food.y + .5f) * cell))
        }
        Text(if (state.over) { if (state.won) "Perfect run!" else "Game over — try again" } else if (running) "Swipe the board or use the arrows" else "Ready when you are. Downloads keep running.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { steer(SnakeDirection.LEFT) }, enabled = running, contentPadding = PaddingValues(10.dp)) { Text("←", Modifier.semantics { contentDescription = "Snake left" }) }
            Column { OutlinedButton(onClick = { steer(SnakeDirection.UP) }, enabled = running, contentPadding = PaddingValues(10.dp)) { Text("↑", Modifier.semantics { contentDescription = "Snake up" }) }; OutlinedButton(onClick = { steer(SnakeDirection.DOWN) }, enabled = running, contentPadding = PaddingValues(10.dp)) { Text("↓", Modifier.semantics { contentDescription = "Snake down" }) } }
            OutlinedButton(onClick = { steer(SnakeDirection.RIGHT) }, enabled = running, contentPadding = PaddingValues(10.dp)) { Text("→", Modifier.semantics { contentDescription = "Snake right" }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandButton(onClick = { if (state.over) { state = SnakeState(); requested = SnakeDirection.RIGHT }; running = !running }) { Text(if (state.over) "Play again" else if (running) "Pause Snake" else "Start Snake") }
            TextButton(onClick = { running = false; state = SnakeState(); requested = SnakeDirection.RIGHT }) { Text("Restart") }
        }
    }
}
