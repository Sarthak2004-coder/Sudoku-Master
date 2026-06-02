package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolutionExplanationScreen(
    navController: NavController,
    viewModel: GameViewModel
) {
    val gameState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentHint by remember { mutableStateOf<SudokuHint?>(null) }
    var hintChecked by remember { mutableStateOf(false) }

    // Run hint search on entry
    LaunchedEffect(gameState.grid) {
        val arrayGrid = Array(gameState.size) { r ->
            IntArray(gameState.size) { c ->
                gameState.grid[r][c].value
            }
        }
        currentHint = SudokuTechniques.findSimplestHint(arrayGrid, gameState.size)
        hintChecked = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hint Walkthrough", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Logical Solver Analyst",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center
            )

            Text(
                "We check all available candidates mathematically from simple single positions to advanced multidirectional loops.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Dynamic mini preview of the grid specifying targets
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                MiniGridRender(
                    grid = gameState.grid,
                    gridSize = gameState.size,
                    hint = currentHint
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!hintChecked) {
                CircularProgressIndicator()
            } else {
                val hint = currentHint
                if (hint != null) {
                    TechniqueExplanationCard(hint = hint, viewModel = viewModel, navController = navController)
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "No Direct Logic Technique Deducible",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "This configuration does not contain immediate transparent solutions. You might need trial-and-error (bifurcation) techniques to proceed.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TechniqueExplanationCard(hint: SudokuHint, viewModel: GameViewModel, navController: NavController) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Technique Name Badge
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = hint.title.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            // 2. Plain English "what this means"
            val whatThisMeans = when (hint) {
                is SudokuHint.NakedSingle -> "Only one possible number can fit in this specific cell."
                is SudokuHint.HiddenSingle -> "This number can only fit in one possible cell within its row, column, or box."
                is SudokuHint.NakedPair -> "Two cells in the same unit share the exact same two candidates, locking them in."
                is SudokuHint.PointingPairs -> "A number's only possible spots in a box are in one line, clearing that line elsewhere."
                else -> hint.description.split("—").first().trim()
            }
            Text(
                text = whatThisMeans,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            // 3. Numbered Step-by-Step
            val steps = when (hint) {
                is SudokuHint.NakedSingle -> listOf(
                    "Check row, column, and box constraints for R${hint.row + 1}C${hint.col + 1}.",
                    "Eliminate all numbers that already appear in these units.",
                    "Only ${hint.num} remains as a valid candidate."
                )
                is SudokuHint.HiddenSingle -> listOf(
                    "Look at the ${hint.unitType} containing R${hint.row + 1}C${hint.col + 1}.",
                    "Notice that ${hint.num} cannot go anywhere else in this ${hint.unitType}.",
                    "Place ${hint.num} in the only remaining available spot."
                )
                is SudokuHint.NakedPair -> listOf(
                    "Identify two cells containing only the pair ${hint.nums.joinToString(", ")}.",
                    "Since these two cells must contain these two numbers, no other cell in the ${hint.unitType} can.",
                    "Eliminate ${hint.nums.joinToString(" and ")} from all other cells in the ${hint.unitType}."
                )
                is SudokuHint.PointingPairs -> listOf(
                    "In box ${hint.boxIndex + 1}, the number ${hint.num} only appears in ${hint.rowOrCol} ${hint.unitIndex + 1}.",
                    "Therefore, ${hint.num} must be in one of those cells within the box.",
                    "Remove ${hint.num} from all other cells in ${hint.rowOrCol} ${hint.unitIndex + 1} outside this box."
                )
                else -> listOf(hint.description)
            }

            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (hint.targetCells.isNotEmpty()) {
                        viewModel.selectCell(hint.targetCells.first().first, hint.targetCells.first().second)
                    }
                    viewModel.hintRequested()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply & Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MiniGridRender(
    grid: List<List<SudokuCell>>,
    gridSize: Int,
    hint: SudokuHint?
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val amber = Color(0xFFFFBF00)
    val mutedRed = Color(0xFFE57373).copy(alpha = 0.6f)

    val colorScheme = MaterialTheme.colorScheme

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellSize = size.width / gridSize
        val (boxRows, boxCols) = SudokuSolver.getBoxDimensions(gridSize)

        // 1. Draw Highlights based on Technique
        if (hint != null) {
            when (hint) {
                is SudokuHint.NakedSingle -> {
                    // Row, Col, Box sweeps
                    drawRect(primary.copy(alpha = 0.1f), Offset(0f, hint.row * cellSize), androidx.compose.ui.geometry.Size(size.width, cellSize))
                    drawRect(primary.copy(alpha = 0.1f), Offset(hint.col * cellSize, 0f), androidx.compose.ui.geometry.Size(cellSize, size.height))
                    val boxR = (hint.row / boxRows) * boxRows
                    val boxC = (hint.col / boxCols) * boxCols
                    drawRect(primary.copy(alpha = 0.1f), Offset(boxC * cellSize, boxR * cellSize), androidx.compose.ui.geometry.Size(boxCols * cellSize, boxRows * cellSize))
                    // Target
                    drawRect(amber, Offset(hint.col * cellSize, hint.row * cellSize), androidx.compose.ui.geometry.Size(cellSize, cellSize))
                }
                is SudokuHint.HiddenSingle -> {
                    // Highlight Unit
                    val boxR = (hint.row / boxRows) * boxRows
                    val boxC = (hint.col / boxCols) * boxCols
                    
                    when (hint.unitType) {
                        "row" -> drawRect(primary.copy(alpha = 0.15f), Offset(0f, hint.row * cellSize), androidx.compose.ui.geometry.Size(size.width, cellSize))
                        "column" -> drawRect(primary.copy(alpha = 0.15f), Offset(hint.col * cellSize, 0f), androidx.compose.ui.geometry.Size(cellSize, size.height))
                        "box" -> drawRect(primary.copy(alpha = 0.15f), Offset(boxC * cellSize, boxR * cellSize), androidx.compose.ui.geometry.Size(boxCols * cellSize, boxRows * cellSize))
                    }
                    
                    // Gray out cells that already have the number in the same unit
                    for (r in 0 until gridSize) {
                        for (c in 0 until gridSize) {
                            val inRow = r == hint.row && hint.unitType == "row"
                            val inCol = c == hint.col && hint.unitType == "column"
                            val inBox = r >= boxR && r < boxR + boxRows && c >= boxC && c < boxC + boxCols && hint.unitType == "box"
                            
                            if ((inRow || inCol || inBox) && grid[r][c].value != 0 && grid[r][c].value != hint.num) {
                                drawRect(Color.Gray.copy(alpha = 0.3f), Offset(c * cellSize, r * cellSize), androidx.compose.ui.geometry.Size(cellSize, cellSize))
                            }
                        }
                    }

                    // Target Cell Glowing
                    drawRect(amber, Offset(hint.col * cellSize, hint.row * cellSize), androidx.compose.ui.geometry.Size(cellSize, cellSize))
                }
                is SudokuHint.NakedPair -> {
                    // Highlight the two cells
                    drawRect(primary.copy(alpha = 0.2f), Offset(hint.cell1.second * cellSize, hint.cell1.first * cellSize), androidx.compose.ui.geometry.Size(cellSize, cellSize))
                    drawRect(primary.copy(alpha = 0.2f), Offset(hint.cell2.second * cellSize, hint.cell2.first * cellSize), androidx.compose.ui.geometry.Size(cellSize, cellSize))
                    
                    // Elimination markers
                    hint.eliminations.forEach { (cell, _) ->
                        drawRect(mutedRed, Offset(cell.second * cellSize, cell.first * cellSize), androidx.compose.ui.geometry.Size(cellSize, cellSize))
                        // Strikethrough
                        drawLine(Color.Red, Offset(cell.second * cellSize + 4, cell.first * cellSize + 4), Offset((cell.second + 1) * cellSize - 4, (cell.first + 1) * cellSize - 4), 2f)
                    }
                }
                is SudokuHint.PointingPairs -> {
                    // Box highlight
                    val startRow = (hint.boxIndex / (9/boxRows)) * boxRows
                    val startCol = (hint.boxIndex % (9/boxRows)) * boxCols
                    drawRect(primary.copy(alpha = 0.1f), Offset(startCol * cellSize, startRow * cellSize), androidx.compose.ui.geometry.Size(boxCols * cellSize, boxRows * cellSize))
                    
                    // Arrow extending
                    val targetCellsInBox = grid.flatten().filter { it.row / boxRows == startRow / boxRows && it.col / boxCols == startCol / boxCols && it.pencilMarks.contains(hint.num) }
                    if (targetCellsInBox.isNotEmpty()) {
                        val centerR = (targetCellsInBox.map { it.row }.average() + 0.5f).toFloat() * cellSize
                        val centerC = (targetCellsInBox.map { it.col }.average() + 0.5f).toFloat() * cellSize
                        
                        val targetTo = Offset(
                            if (hint.rowOrCol == "column") centerC else if (startCol == 0) size.width else 0f, 
                            if (hint.rowOrCol == "row") centerR else if (startRow == 0) size.height else 0f
                        )
                        drawArrow(Offset(centerC, centerR), targetTo, primary)
                    }
                }
                else -> { /* Other techniques */ }
            }
        }

        // 2. Draw Grid Lines
        for (i in 0..gridSize) {
            val (bR, bC) = SudokuSolver.getBoxDimensions(gridSize)
            
            // Vertical
            val vIsBoundary = i % bC == 0
            drawLine(
                color = if (vIsBoundary) colorScheme.outline else colorScheme.outlineVariant,
                start = Offset(i * cellSize, 0f),
                end = Offset(i * cellSize, size.height),
                strokeWidth = (if (vIsBoundary) 2.dp else 0.5.dp).toPx()
            )
            // Horizontal
            val hIsBoundary = i % bR == 0
            drawLine(
                color = if (hIsBoundary) colorScheme.outline else colorScheme.outlineVariant,
                start = Offset(0f, i * cellSize),
                end = Offset(size.width, i * cellSize),
                strokeWidth = (if (hIsBoundary) 2.dp else 0.5.dp).toPx()
            )
        }

        // 3. Draw Numbers
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val cell = grid[r][c]
                if (cell.value != 0) {
                    val text = cell.value.toString()
                    val textLayoutResult = textMeasurer.measure(
                        text = text,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hint?.targetCells?.contains(Pair(r, c)) == true) Color.Black else onSurface.copy(alpha = 0.4f)
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            c * cellSize + (cellSize - textLayoutResult.size.width) / 2,
                            r * cellSize + (cellSize - textLayoutResult.size.height) / 2
                        )
                    )
                }
            }
        }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(from: Offset, to: Offset, color: Color) {
    val strokeWidth = 3.dp.toPx()
    drawLine(color, from, to, strokeWidth)
    
    val angle = atan2(to.y - from.y, to.x - from.x)
    val arrowSize = 12.dp.toPx()
    
    val path = Path().apply {
        moveTo(to.x, to.y)
        lineTo(
            to.x - arrowSize * cos(angle - Math.PI / 6).toFloat(),
            to.y - arrowSize * sin(angle - Math.PI / 6).toFloat()
        )
        lineTo(
            to.x - arrowSize * cos(angle + Math.PI / 6).toFloat(),
            to.y - arrowSize * sin(angle + Math.PI / 6).toFloat()
        )
        close()
    }
    drawPath(path, color)
}
