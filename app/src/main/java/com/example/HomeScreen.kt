package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    gameViewModel: GameViewModel
) {
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
    var showDifficultyBS by remember { mutableStateOf(false) }
    var selectedGridSize by remember { mutableIntStateOf(9) }
    var selectedDiff by remember { mutableStateOf(SudokuGenerator.Difficulty.EASY) }

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text(
                    text = "$greeting,",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ready to play?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logo Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                // Minimalistic Sudoku Geometric Logo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(12.dp)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 4f
                        val color = Color.Gray.copy(alpha = 0.3f)
                        
                        // draw blocks
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset.Zero,
                            size = size,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeW * 3)
                        )
                        
                        // draw inner lines
                        val thirdW = size.width / 3
                        val thirdH = size.height / 3

                        drawLine(color, Offset(thirdW, 0f), Offset(thirdW, size.height), strokeW)
                        drawLine(color, Offset(thirdW * 2, 0f), Offset(thirdW * 2, size.height), strokeW)
                        drawLine(color, Offset(0f, thirdH), Offset(size.width, thirdH), strokeW)
                        drawLine(color, Offset(0f, thirdH * 2), Offset(size.width, thirdH * 2), strokeW)
                    }
                    
                    Text("9", modifier = Modifier.align(Alignment.TopStart).padding(4.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    Text("5", modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary))
                    Text("2", modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sudoku",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.testTag("app_title")
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val actions = listOf(
                    MenuAction("New Game", Icons.Outlined.PlayArrow, { showDifficultyBS = true }),
                    MenuAction("Daily Challenge", Icons.Outlined.CalendarToday, { 
                        gameViewModel.startDailyChallenge()
                        navController.navigate(GameDestination)
                    }),
                    MenuAction("Custom Puzzle", Icons.Outlined.Edit, { navController.navigate(CustomPuzzleDestination) }),
                    MenuAction("Statistics", Icons.Outlined.BarChart, { navController.navigate(StatsDestination) })
                )

                actions.forEachIndexed { index, action ->
                    AnimatedVisibility(
                        visible = animateTrigger,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        ) + fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = index * 60))
                    ) {
                        HomeMenuCard(action)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ==========================================
        // LAUNCHER / DIFFICULTY BOTTOM SHEET
        // ==========================================
        if (showDifficultyBS) {
            ModalBottomSheet(
                onDismissRequest = { showDifficultyBS = false },
                dragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Configure Puzzle",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Grid Size segment selector
                    Text(
                        "Grid Size Mode",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(4, 6, 9).forEach { size ->
                            val isSelected = selectedGridSize == size
                            val label = when (size) {
                                4 -> "4x4 (Micro)"
                                6 -> "6x6 (Mini)"
                                9 -> "9x9 (Classic)"
                                else -> "9x9"
                            }
                            Card(
                                onClick = { selectedGridSize = size },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Difficulty grid cards
                    Text(
                        "Clue Difficulty",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SudokuGenerator.Difficulty.values().forEach { difficulty ->
                            val isSelected = selectedDiff == difficulty
                            val label = difficulty.name.replaceFirstChar { it.uppercase() }
                            val cluesText = when (selectedGridSize) {
                                4 -> when (difficulty) {
                                    SudokuGenerator.Difficulty.EASY -> "~11 clues"
                                    SudokuGenerator.Difficulty.INTERMEDIATE -> "~9 clues"
                                    SudokuGenerator.Difficulty.HARD -> "~7 clues"
                                    SudokuGenerator.Difficulty.EXTREME -> "~5 clues"
                                }
                                6 -> when (difficulty) {
                                    SudokuGenerator.Difficulty.EASY -> "~23 clues"
                                    SudokuGenerator.Difficulty.INTERMEDIATE -> "~20 clues"
                                    SudokuGenerator.Difficulty.HARD -> "~17 clues"
                                    SudokuGenerator.Difficulty.EXTREME -> "~13 clues"
                                }
                                9 -> when (difficulty) {
                                    SudokuGenerator.Difficulty.EASY -> "~48 clues"
                                    SudokuGenerator.Difficulty.INTERMEDIATE -> "~38 clues"
                                    SudokuGenerator.Difficulty.HARD -> "~29 clues"
                                    SudokuGenerator.Difficulty.EXTREME -> "~25 clues"
                                }
                                else -> ""
                            }
                            val estTime = when (difficulty) {
                                SudokuGenerator.Difficulty.EASY -> "2 - 5 min"
                                SudokuGenerator.Difficulty.INTERMEDIATE -> "5 - 12 min"
                                SudokuGenerator.Difficulty.HARD -> "12 - 25 min"
                                SudokuGenerator.Difficulty.EXTREME -> "25 + min"
                            }

                            Card(
                                onClick = { selectedDiff = difficulty },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            cluesText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        estTime,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showDifficultyBS = false
                            gameViewModel.startNewGame(selectedGridSize, selectedDiff)
                            navController.navigate(GameDestination)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_difficulty_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Start Generation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

data class MenuAction(val title: String, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMenuCard(action: MenuAction) {
    Card(
        onClick = action.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
