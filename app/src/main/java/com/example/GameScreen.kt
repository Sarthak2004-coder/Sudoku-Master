package com.example

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    viewModel: GameViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDropdownMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Format time (seconds to MM:SS)
    val timeFormatted = remember(state.timerSeconds) {
        val minutes = state.timerSeconds / 60
        val remainingSecs = state.timerSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSecs)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.size}x${state.size} - ${state.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePause() }) {
                        Icon(
                            imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (state.isPaused) "Resume" else "Pause"
                        )
                    }
                    IconButton(onClick = { showDropdownMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("How to solve next step") },
                            onClick = {
                                showDropdownMenu = false
                                navController.navigate(SolutionExplanationDestination)
                            },
                            leadingIcon = { Icon(Icons.Outlined.Lightbulb, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Restart Game") },
                            onClick = {
                                showDropdownMenu = false
                                viewModel.startNewGame(state.size, state.difficulty)
                            },
                            leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo action
                    IconButton(
                        onClick = { viewModel.undo() },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo")
                            Text("Undo", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }

                    // Pencil action
                    IconButton(
                        onClick = { viewModel.togglePencilMode() },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (state.isPencilMode) Icons.Default.EditCalendar else Icons.Outlined.Edit,
                                contentDescription = "Pencil mode",
                                tint = if (state.isPencilMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Pencil",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (state.isPencilMode) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (state.isPencilMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    }

                    // Hint action
                    val hintsRemaining = 3 - state.hintsUsed
                    val hintEnabled = state.isCustomMode || hintsRemaining > 0
                    IconButton(
                        onClick = { viewModel.hintRequested() },
                        enabled = hintEnabled,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Get Hint",
                                tint = if (hintEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                            val hintLabel = if (state.isCustomMode) "Hint" else "Hint ($hintsRemaining)"
                            Text(hintLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }

                    // Erase action
                    IconButton(
                        onClick = { viewModel.eraseSelectedCell() },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Erase cell")
                            Text("Erase", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val sheetState = rememberModalBottomSheetState()
            
            if (state.lastHint != null) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.dismissHint() },
                    sheetState = sheetState
                ) {
                    val hintResult = state.lastHint as HintResult.Success
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = hintResult.techniqueName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        
                        Text(
                            text = hintResult.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        
                        // Mini 3x3 sub-grid showing just the box context
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            val (boxRows, boxCols) = SudokuSolver.getBoxDimensions(state.size)
                            val targetR = hintResult.targetCell.first
                            val targetC = hintResult.targetCell.second
                            val boxStartRow = (targetR / boxRows) * boxRows
                            val boxStartCol = (targetC / boxCols) * boxCols
                            
                            Column(modifier = Modifier.fillMaxSize()) {
                                for (r in 0 until boxRows) {
                                    Row(modifier = Modifier.weight(1f)) {
                                        for (c in 0 until boxCols) {
                                            val cellR = boxStartRow + r
                                            val cellC = boxStartCol + c
                                            val isTarget = cellR == targetR && cellC == targetC
                                            val cellValue = state.grid[cellR][cellC].value
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                                    .background(if (isTarget) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (cellValue != 0) {
                                                    Text(
                                                        text = cellValue.toString(),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Button(
                            onClick = { viewModel.dismissHint() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(60.dp)) // Reserve space for hint overlay if it appears

                // ==========================================
                // THE SUDOKU GRID
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(3.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("sudoku_grid")
                ) {
                    if (state.isPaused) {
                        // Game Paused overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.togglePause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Resume",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Game Paused",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Tap anywhere to resume",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    } else {
                        SudokuRenderGrid(
                            grid = state.grid,
                            size = state.size,
                            selectedCell = state.selectedCell,
                            onCellSelect = { r, c -> viewModel.selectCell(r, c) }
                        )
                    }
                }

                // ==========================================
                // DIGIT KEY PAD
                // ==========================================
                DigitPad(
                    placedCount = state.placedCount,
                    maxSize = state.size,
                    onDigitClick = { num -> viewModel.enterNumber(num) }
                )
            }

            // Active hint snackbar overlay (Sticky at top)
            AnimatedVisibility(
                visible = state.activeHintMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut(),
                modifier = Modifier.padding(16.dp).align(Alignment.TopCenter)
            ) {
                state.activeHintMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (state.activeHint != null) {
                                    viewModel.applyActiveHint()
                                }
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Hint Help Available",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                )
                                Text(msg, style = MaterialTheme.typography.bodySmall)
                            }
                            if (state.activeHint != null) {
                                TextButton(onClick = { viewModel.applyActiveHint() }) {
                                    Text("Apply", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            IconButton(onClick = { viewModel.selectCell(-1, -1) }) { // Hack to clear hint
                                Icon(Icons.Default.Close, contentDescription = "Close hint", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // ==========================================
            // COMPLETED / CONGRATULATIONS DIALOG
            // ==========================================
            if (state.isCompleted) {
                AlertDialog(
                    onDismissRequest = { /* Don't dismiss without choosing */ },
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophy icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Puzzle Completed!",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Excellent job! You successfully completed this Sudoku challenge.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )

                            // Star rating
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(3) { i ->
                                    Icon(
                                        imageVector = if (i < state.ratingStars) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Rating $i",
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Difficulty:", fontWeight = FontWeight.Bold)
                                Text(state.difficulty.name)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Time Taken:", fontWeight = FontWeight.Bold)
                                Text(timeFormatted)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Hints Used:", fontWeight = FontWeight.Bold)
                                Text("${state.hintsUsed}/3")
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.startNewGame(state.size, state.difficulty)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Play Again", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                val text = "Sudoku ${state.difficulty.name} ⭐".repeat(state.ratingStars) + " | Time: $timeFormatted | Hints: ${state.hintsUsed} #SudokuAndroid"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Result"))
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SudokuRenderGrid(
    grid: List<List<SudokuCell>>,
    size: Int,
    selectedCell: Pair<Int, Int>?,
    onCellSelect: (row: Int, col: Int) -> Unit
) {
    val gridSize = size
    val (boxRows, boxCols) = SudokuSolver.getBoxDimensions(gridSize)
    val colorScheme = MaterialTheme.colorScheme

    fun getCellBackground(row: Int, col: Int): Color {
        val cell = grid[row][col]
        val isSelected = selectedCell?.first == row && selectedCell?.second == col
        
        var isSoftHighlight = false
        var isMediumHighlight = false
        if (selectedCell != null && selectedCell.first >= 0) {
            val selR = selectedCell.first
            val selC = selectedCell.second
            val selVal = grid[selR][selC].value
            
            val inRow = row == selR
            val inCol = col == selC
            val inBox = row / boxRows == selR / boxRows && col / boxCols == selC / boxCols
            if ((inRow || inCol || inBox) && !isSelected) isSoftHighlight = true
            if (selVal != 0 && cell.value == selVal && !isSelected) isMediumHighlight = true
        }

        return when {
            isSelected -> colorScheme.primaryContainer
            cell.isError -> colorScheme.errorContainer
            isMediumHighlight -> colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            isSoftHighlight -> colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else -> colorScheme.surface
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(horizontal = 8.dp)
            .border(2.dp, colorScheme.outline, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until gridSize) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0 until gridSize) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    .background(getCellBackground(row, col))
                                    .clickable { onCellSelect(row, col) },
                                contentAlignment = Alignment.Center
                            ) {
                                val cell = grid[row][col]
                                if (cell.value != 0) {
                                    Text(
                                        text = cell.value.toString(),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = if (cell.isGiven) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = if (size == 4) 28.sp else if (size == 6) 24.sp else 20.sp,
                                            color = when {
                                                cell.isError -> colorScheme.error
                                                cell.isGiven -> colorScheme.onSurface
                                                else -> colorScheme.primary
                                            }
                                        )
                                    )
                                } else if (cell.pencilMarks.isNotEmpty()) {
                                    PencilGrid(marks = cell.pencilMarks, size = gridSize)
                                }
                            }
                        }
                    }
                }
            }

            val outlineColor = colorScheme.outline
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val strokeWidth = 2.dp.toPx()

                // Draw vertical box dividers
                val numBoxCols = gridSize / boxCols
                for (i in 1 until numBoxCols) {
                    val x = (w / numBoxCols) * i
                    drawLine(
                        color = outlineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = strokeWidth
                    )
                }

                // Draw horizontal box dividers
                val numBoxRows = gridSize / boxRows
                for (i in 1 until numBoxRows) {
                    val y = (h / numBoxRows) * i
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    }
}

@Composable
fun PencilGrid(marks: Set<Int>, size: Int) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fontSize = (this.maxWidth.value / 4.5f).coerceAtLeast(7f).sp

        for (num in 1..9) {
            if (num <= size && marks.contains(num)) {
                val row = (num - 1) / 3
                val col = (num - 1) % 3
                Box(
                    modifier = Modifier
                        .size(this.maxWidth / 3, this.maxHeight / 3)
                        .offset(x = (this.maxWidth / 3) * col, y = (this.maxHeight / 3) * row),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = num.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = fontSize,
                            color = MaterialTheme.colorScheme.tertiary,
                            lineHeight = fontSize
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DigitPad(
    placedCount: Map<Int, Int>,
    maxSize: Int,
    onDigitClick: (num: Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (digit in 1..maxSize) {
            val isComplete = (placedCount[digit] ?: 0) == maxSize
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = if (isComplete) 0.3f else 1f))
                    .clickable(enabled = !isComplete) { onDigitClick(digit) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = digit.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant.copy(alpha = if (isComplete) 0.3f else 1f)
                )
            }
        }
    }
}
