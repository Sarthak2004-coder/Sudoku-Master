package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPuzzleScreen(
    navController: NavController,
    gameViewModel: GameViewModel
) {
    var selectedSize by remember { mutableIntStateOf(9) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var gridState by remember { mutableStateOf(Array(9) { IntArray(9) { 0 } }) }
    
    val scope = rememberCoroutineScope()
    var isValidating by remember { mutableStateOf(false) }

    // Status states
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }

    // Re-initialize layout whenever sizing modes modify
    LaunchedEffect(selectedSize) {
        gridState = Array(selectedSize) { IntArray(selectedSize) { 0 } }
        selectedCell = null
        statusMessage = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Puzzle", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
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
                "Create Custom Puzzles",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center
            )

            // Select Grid Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(4, 6, 9).forEach { size ->
                    val isSel = selectedSize == size
                    Card(
                        onClick = { selectedSize = size },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "${size}x${size}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium)
                            )
                        }
                    }
                }
            }

            // The Editable Grid Space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(3.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            ) {
                val (boxRows, boxCols) = SudokuSolver.getBoxDimensions(selectedSize)
                val numBoxRows = selectedSize / boxRows
                val numBoxCols = selectedSize / boxCols

                Column(modifier = Modifier.fillMaxSize()) {
                    for (br in 0 until numBoxRows) {
                        Row(modifier = Modifier.weight(1f)) {
                            for (bc in 0 until numBoxCols) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(1.5.dp, MaterialTheme.colorScheme.onBackground)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        for (rInBox in 0 until boxRows) {
                                            Row(modifier = Modifier.weight(1f)) {
                                                for (cInBox in 0 until boxCols) {
                                                    val r = br * boxRows + rInBox
                                                    val c = bc * boxCols + cInBox
                                                    
                                                    val isSelected = selectedCell?.first == r && selectedCell?.second == c
                                                    val cellVal = if (r < gridState.size && c < gridState[r].size) gridState[r][c] else 0

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight()
                                                            .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                                                            .background(
                                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                            )
                                                            .clickable { selectedCell = Pair(r, c) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (cellVal != 0) {
                                                            Text(
                                                                text = cellVal.toString(),
                                                                style = MaterialTheme.typography.titleLarge.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    fontSize = if (selectedSize == 9) 18.sp else 22.sp
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (isValidating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Editable Digit Input pad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (digit in 1..selectedSize) {
                    Button(
                        onClick = {
                            val cell = selectedCell
                            if (cell != null) {
                                val (r, c) = cell
                                // Update grid safely
                                if (r < gridState.size && c < gridState[r].size) {
                                    val newGrid = Array(gridState.size) { rowIdx -> gridState[rowIdx].clone() }
                                    newGrid[r][c] = digit
                                    gridState = newGrid
                                    statusMessage = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            digit.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
                // Back/Erase option
                Button(
                    onClick = {
                        val cell = selectedCell
                        if (cell != null) {
                            val (r, c) = cell
                            if (r < gridState.size && c < gridState[r].size) {
                                val newGrid = Array(gridState.size) { rowIdx -> gridState[rowIdx].clone() }
                                newGrid[r][c] = 0
                                gridState = newGrid
                                statusMessage = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Del", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer))
                }
            }

            // Dynamic evaluation info card
            AnimatedVisibility(visible = statusMessage != null) {
                statusMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccessStatus) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSuccessStatus) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Game Control panel row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clear button
                OutlinedButton(
                    onClick = {
                        gridState = Array(selectedSize) { IntArray(selectedSize) { 0 } }
                        selectedCell = null
                        statusMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }

                // Validate button
                Button(
                    onClick = {
                        val currentGrid = gridState
                        val currentSize = selectedSize
                        
                        // Check for consistency before processing
                        if (currentGrid.size != currentSize) return@Button
                        
                        isValidating = true
                        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                            val hasConflicts = checkImmediateConflicts(currentGrid, currentSize)
                            if (hasConflicts) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    statusMessage = "Conflict Error: Duplicate numbers exist in some row, column, or sub-box."
                                    isSuccessStatus = false
                                    isValidating = false
                                }
                                return@launch
                            }

                            val cloneGrid = Array(currentSize) { r -> currentGrid[r].clone() }
                            val solsCount = SudokuSolver.countSolutions(cloneGrid, currentSize, limit = 2)
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (solsCount == 0) {
                                    statusMessage = "No Solution: No valid Sudoku configuration can solve this custom board."
                                    isSuccessStatus = false
                                } else if (solsCount == 1) {
                                    statusMessage = "Perfect! Puzzle is completely valid and holds exactly 1 unique solution."
                                    isSuccessStatus = true
                                } else {
                                    statusMessage = "Multiple Solutions Found: This configuration is valid, but has more than one final solved grid state."
                                    isSuccessStatus = true
                                }
                                isValidating = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    enabled = !isValidating
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Validate")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Solve board
                FilledTonalButton(
                    onClick = {
                        val currentGrid = gridState
                        val currentSize = selectedSize
                        if (currentGrid.size != currentSize) return@FilledTonalButton

                        isValidating = true
                        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                            val cloneGrid = Array(currentSize) { r -> currentGrid[r].clone() }
                            val solved = SudokuSolver.solve(cloneGrid, currentSize)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (solved) {
                                    gridState = cloneGrid
                                    statusMessage = "Backtracking solved successfully!"
                                    isSuccessStatus = true
                                } else {
                                    statusMessage = "Cannot solve. Check if constraints conflict!"
                                    isSuccessStatus = false
                                }
                                isValidating = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isValidating
                ) {
                    Text("Solve Grid")
                }

                // Play this puzzle
                Button(
                    onClick = {
                        val currentGrid = gridState
                        val currentSize = selectedSize
                        if (currentGrid.size != currentSize) return@Button

                        isValidating = true
                        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                            // We must first resolve the full solution to pass as solvedGrid reference
                            val cloneGrid = Array(currentSize) { r -> currentGrid[r].clone() }
                            val solved = SudokuSolver.solve(cloneGrid, currentSize)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (solved) {
                                    gameViewModel.startCustomPuzzle(currentSize, currentGrid, cloneGrid)
                                    navController.navigate(GameDestination)
                                } else {
                                    statusMessage = "Cannot play: Current puzzle layout must have at least one logical solution."
                                    isSuccessStatus = false
                                }
                                isValidating = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isValidating
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play Puzzle")
                }
            }
        }
    }
}

// Check if any numbers are already conflicting
private fun checkImmediateConflicts(grid: Array<IntArray>, size: Int): Boolean {
    val (boxRows, boxCols) = SudokuSolver.getBoxDimensions(size)

    for (r in 0 until size) {
        for (c in 0 until size) {
            val num = grid[r][c]
            if (num != 0) {
                // Check row
                for (otherC in 0 until size) {
                    if (otherC != c && grid[r][otherC] == num) return true
                }
                // Check col
                for (otherR in 0 until size) {
                    if (otherR != r && grid[otherR][c] == num) return true
                }
                // Check box
                val boxStartRow = (r / boxRows) * boxRows
                val boxStartCol = (c / boxCols) * boxCols
                for (br in 0 until boxRows) {
                    for (bc in 0 until boxCols) {
                        val or = boxStartRow + br
                        val oc = boxStartCol + bc
                        if ((or != r || oc != c) && grid[or][oc] == num) {
                            return true
                        }
                    }
                }
            }
        }
    }
    return false
}
