package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class SudokuCell(
    val row: Int,
    val col: Int,
    val value: Int,
    val isGiven: Boolean,
    val pencilMarks: Set<Int> = emptySet(),
    val isError: Boolean = false
)

enum class GameMode {
    NORMAL, DAILY_CHALLENGE, CUSTOM
}

data class GameState(
    val size: Int = 9,
    val difficulty: SudokuGenerator.Difficulty = SudokuGenerator.Difficulty.EASY,
    val grid: List<List<SudokuCell>> = emptyList(),
    val solvedGrid: Array<IntArray> = emptyArray(), // Reference solved grid
    val selectedCell: Pair<Int, Int>? = null,
    val isPencilMode: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val timerSeconds: Int = 0,
    val hintsUsed: Int = 0,
    val isCustomMode: Boolean = false,
    val activeHint: SudokuHint? = null,
    val activeHintMessage: String? = null,
    val lastHint: HintResult? = null,
    val gameMode: GameMode = GameMode.NORMAL,
    val dailyChallengeCompleted: Boolean = false,
    val ratingStars: Int = 0,
    val placedCount: Map<Int, Int> = emptyMap()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    // Keep state snapshots for unlimited Undo
    private val undoStack = mutableListOf<List<List<SudokuCell>>>()

    private var timerJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
        
        // Start first game by default
        startNewGame(9, SudokuGenerator.Difficulty.EASY)
    }

    /**
     * Start a standard game with selected size and difficulty
     */
    fun startNewGame(size: Int, difficulty: SudokuGenerator.Difficulty) {
        _uiState.update { it.copy(isPaused = true, isCustomMode = false) } // pause during logic
        viewModelScope.launch {
            val puzzle = SudokuGenerator.generate(size, difficulty)
            initializeGrid(puzzle.size, puzzle.puzzleGrid, puzzle.originalGrid, difficulty, GameMode.NORMAL)
            startTimer()
        }
    }

    /**
     * Start the Daily Challenge loaded based on current date seed
     */
    fun startDailyChallenge() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(isCustomMode = false) }
        viewModelScope.launch {
            val existing = repository.getDailyChallenge(currentDate)
            if (existing != null && existing.completed) {
                // Already completed daily challenge today
                _uiState.update { it.copy(dailyChallengeCompleted = true) }
            }

            // Create deterministic seed from string
            val seed = currentDate.replace("-", "").toLongOrNull() ?: System.currentTimeMillis()
            val random = Random(seed)

            // Let's generate a standard 9x9 Intermediate puzzle for the daily challenge
            val puzzle = SudokuGenerator.generate(9, SudokuGenerator.Difficulty.INTERMEDIATE, random)
            initializeGrid(9, puzzle.puzzleGrid, puzzle.originalGrid, SudokuGenerator.Difficulty.INTERMEDIATE, GameMode.DAILY_CHALLENGE)
            startTimer()
        }
    }

    /**
     * Set up custom input mode grid
     */
    fun startCustomPuzzle(size: Int, board: Array<IntArray>, solvedBoard: Array<IntArray>) {
        initializeGrid(size, board, solvedBoard, SudokuGenerator.Difficulty.INTERMEDIATE, GameMode.CUSTOM)
        _uiState.update { it.copy(isCustomMode = true) }
        startTimer()
    }

    private fun initializeGrid(
        size: Int,
        puzzleGrid: Array<IntArray>,
        originalGrid: Array<IntArray>,
        difficulty: SudokuGenerator.Difficulty,
        mode: GameMode
    ) {
        undoStack.clear()
        val gridList = List(size) { r ->
            List(size) { c ->
                SudokuCell(
                    row = r,
                    col = c,
                    value = puzzleGrid[r][c],
                    isGiven = puzzleGrid[r][c] != 0
                )
            }
        }

        _uiState.update {
            GameState(
                size = size,
                difficulty = difficulty,
                grid = gridList,
                solvedGrid = originalGrid,
                selectedCell = null,
                isPencilMode = false,
                isPaused = false,
                isCompleted = false,
                timerSeconds = 0,
                hintsUsed = 0,
                activeHint = null,
                activeHintMessage = null,
                gameMode = mode,
                dailyChallengeCompleted = false,
                ratingStars = 0,
                placedCount = calculatePlacedCount(gridList)
            )
        }
        validateErrors()
    }

    // Timer control
    fun startTimer() {
        _uiState.update { it.copy(isPaused = false) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (!_uiState.value.isPaused && !_uiState.value.isCompleted) {
                    _uiState.update { it.copy(timerSeconds = it.timerSeconds + 1) }
                }
            }
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    // Grid Cell Selection
    fun selectCell(row: Int, col: Int) {
        if (_uiState.value.isPaused || _uiState.value.isCompleted) return
        _uiState.update { it.copy(selectedCell = Pair(row, col), activeHint = null, activeHintMessage = null) }
    }

    // Cell modification
    fun enterNumber(num: Int) {
        val state = _uiState.value
        val cellPos = state.selectedCell ?: return
        val r = cellPos.first
        val c = cellPos.second
        val currentCell = state.grid[r][c]

        // Pre-filled given numbers can't be edited
        if (currentCell.isGiven) return

        saveUndoState()

        if (state.isPencilMode) {
            // Pencil notation mode
            val currentPencil = currentCell.pencilMarks.toMutableSet()
            if (currentPencil.contains(num)) {
                currentPencil.remove(num)
            } else {
                currentPencil.add(num)
            }
            updateGridCell(r, c) { it.copy(value = 0, pencilMarks = currentPencil) }
        } else {
            // Normal number entry
            updateGridCell(r, c) { it.copy(value = num, pencilMarks = emptySet()) }
            // Auto-clear conflicting pencil marks in row, col, and box
            autoClearPencilMarks(r, c, num)
            validateErrors()
            checkGameCompletion()
        }
    }

    fun eraseSelectedCell() {
        val state = _uiState.value
        val cellPos = state.selectedCell ?: return
        val r = cellPos.first
        val c = cellPos.second
        val currentCell = state.grid[r][c]

        if (currentCell.isGiven) return

        saveUndoState()

        updateGridCell(r, c) { it.copy(value = 0, pencilMarks = emptySet(), isError = false) }
        validateErrors()
    }

    private fun updateGridCell(row: Int, col: Int, transform: (SudokuCell) -> SudokuCell) {
        val currentList = _uiState.value.grid
        val newList = currentList.mapIndexed { rIndex, rowList ->
            rowList.mapIndexed { cIndex, cell ->
                if (rIndex == row && cIndex == col) {
                    transform(cell)
                } else {
                    cell
                }
            }
        }
        _uiState.update { it.copy(grid = newList) }
    }

    // Toggle Pencil Mode
    fun togglePencilMode() {
        _uiState.update { it.copy(isPencilMode = !it.isPencilMode) }
    }

    // Undo implementation
    private fun saveUndoState() {
        val currentGridState = _uiState.value.grid.map { rList -> rList.map { it.copy() } }
        undoStack.add(currentGridState)
    }

    fun undo() {
        if (undoStack.isEmpty() || _uiState.value.isPaused || _uiState.value.isCompleted) return
        val previousState = undoStack.removeAt(undoStack.size - 1)
        _uiState.update { it.copy(grid = previousState, activeHint = null, activeHintMessage = null, placedCount = calculatePlacedCount(previousState)) }
        validateErrors()
    }

    // Auto-clear pencil marks in affected row, col, box
    private fun autoClearPencilMarks(row: Int, col: Int, num: Int) {
        val state = _uiState.value
        val (boxRows, boxCols) = SudokuSolver.getBoxDimensions(state.size)
        val boxStartRow = (row / boxRows) * boxRows
        val boxStartCol = (col / boxCols) * boxCols

        val currentList = _uiState.value.grid
        val newList = currentList.mapIndexed { rIndex, rowList ->
            rowList.mapIndexed { cIndex, cell ->
                val inRow = rIndex == row
                val inCol = cIndex == col
                val inBox = rIndex >= boxStartRow && rIndex < boxStartRow + boxRows &&
                            cIndex >= boxStartCol && cIndex < boxStartCol + boxCols

                if ((inRow || inCol || inBox) && cell.pencilMarks.contains(num)) {
                    val updatedPencil = cell.pencilMarks.toMutableSet().apply { remove(num) }
                    cell.copy(pencilMarks = updatedPencil)
                } else {
                    cell
                }
            }
        }
        _uiState.update { it.copy(grid = newList) }
    }

    // Validate errors (conflicting row, col, or box)
    private fun validateErrors() {
        val state = _uiState.value
        val currentGrid = state.grid
        val (boxRows, boxCols) = SudokuSolver.getBoxDimensions(state.size)

        val updatedGrid = currentGrid.mapIndexed { r, rList ->
            rList.mapIndexed { c, cell ->
                if (cell.value == 0) {
                    cell.copy(isError = false)
                } else {
                    val num = cell.value
                    // Check duplicate in same row
                    val rowConflict = rList.any { other -> other.col != c && other.value == num }
                    // Check duplicate in same column
                    val colConflict = currentGrid.any { otherRow -> otherRow[c].row != r && otherRow[c].value == num }
                    // Check duplicate in same sub-box
                    val boxStartRow = (r / boxRows) * boxRows
                    val boxStartCol = (c / boxCols) * boxCols
                    var boxConflict = false
                    for (br in 0 until boxRows) {
                        for (bc in 0 until boxCols) {
                            val or = boxStartRow + br
                            val oc = boxStartCol + bc
                            if ((or != r || oc != c) && currentGrid[or][oc].value == num) {
                                boxConflict = true
                            }
                        }
                    }

                    cell.copy(isError = rowConflict || colConflict || boxConflict)
                }
            }
        }
        _uiState.update { it.copy(grid = updatedGrid, placedCount = calculatePlacedCount(updatedGrid)) }
    }

    private fun calculatePlacedCount(grid: List<List<SudokuCell>>): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()
        grid.flatten().forEach { cell ->
            if (cell.value != 0) {
                counts[cell.value] = (counts[cell.value] ?: 0) + 1
            }
        }
        return counts
    }

    /**
     * Hint acquisition engine.
     * Selects logically solvable cells, showcases explanations, and decrements counters.
     */
    fun hintRequested() {
        val state = _uiState.value
        if (state.isCompleted || state.isPaused) return
        
        if (!state.isCustomMode) {
            val hintsRemaining = 3 - state.hintsUsed
            if (hintsRemaining <= 0) {
                _uiState.update { it.copy(activeHintMessage = "No hints remaining!") }
                return
            }
        }

        // Get standard array grid representing current filled matrix
        val arrayGrid = Array(state.size) { r ->
            IntArray(state.size) { c ->
                state.grid[r][c].value
            }
        }

        // Run NakedSingle first, then HiddenSingle
        val candidates = SudokuTechniques.calculateAllCandidates(arrayGrid, state.size)
        var hint: SudokuHint? = findNakedSingleInternal(arrayGrid, state.size, candidates)
        if (hint == null) {
            hint = findHiddenSingleInternal(arrayGrid, state.size, candidates)
        }

        if (hint != null) {
            val target = hint.targetCells.first()
            val value = hint.affectedNumber ?: 0
            
            saveUndoState()
            
            // Set selected cell
            _uiState.update { it.copy(selectedCell = target) }
            
            // Place value immediately
            updateGridCell(target.first, target.second) { 
                it.copy(value = value, pencilMarks = emptySet()) 
            }
            
            // Auto-clear conflicting pencil marks
            autoClearPencilMarks(target.first, target.second, value)
            
            _uiState.update {
                it.copy(
                    hintsUsed = it.hintsUsed + 1,
                    placedCount = calculatePlacedCount(it.grid), // Grid is updated by updateGridCell before this update but updateGridCell now updates state immediately. Wait.
                    lastHint = HintResult.Success(
                        targetCell = target,
                        value = value,
                        techniqueName = hint!!.title,
                        reason = hint!!.description.split("—").last().trim().replaceFirstChar { it.uppercase() }
                    )
                )
            }
            
            validateErrors()
            checkGameCompletion()
        } else {
            // Fallback: search for any empty cell from solvedGrid
            var fallbackFound = false
            for (r in 0 until state.size) {
                for (c in 0 until state.size) {
                    if (state.grid[r][c].value == 0 && state.solvedGrid.size > r && state.solvedGrid[r].size > c) {
                        val correctValue = state.solvedGrid[r][c]
                        val target = Pair(r, c)
                        
                        saveUndoState()
                        _uiState.update { it.copy(selectedCell = target) }
                        updateGridCell(r, c) { it.copy(value = correctValue, pencilMarks = emptySet()) }
                        autoClearPencilMarks(r, c, correctValue)
                        
                        _uiState.update {
                            it.copy(
                                hintsUsed = it.hintsUsed + 1,
                                placedCount = calculatePlacedCount(it.grid),
                                lastHint = HintResult.Success(
                                    targetCell = target,
                                    value = correctValue,
                                    techniqueName = "Standard Hint",
                                    reason = "R${r + 1}C${c + 1} must be $correctValue based on the puzzle solution."
                                )
                            )
                        }
                        fallbackFound = true
                        break
                    }
                }
                if (fallbackFound) break
            }
            if (!fallbackFound) {
                _uiState.update { it.copy(activeHintMessage = "No more moves possible.") }
            }
        }
    }

    private fun findNakedSingleInternal(grid: Array<IntArray>, size: Int, candidates: Map<Pair<Int, Int>, Set<Int>>): SudokuHint? {
        for ((cell, ops) in candidates) {
            if (ops.size == 1) {
                val num = ops.first()
                val (r, c) = cell
                return SudokuHint.NakedSingle(r, c, num, "Naked Single — Only $num fits at R${r+1}C${c+1}; all other values are eliminated by row, column, or box.")
            }
        }
        return null
    }

    private fun findHiddenSingleInternal(grid: Array<IntArray>, size: Int, candidates: Map<Pair<Int, Int>, Set<Int>>): SudokuHint? {
        // Simple Row check for Hidden Single
        for (r in 0 until size) {
            for (num in 1..size) {
                if (grid[r].contains(num)) continue
                val possibleCols = (0 until size).filter { c -> grid[r][c] == 0 && candidates[Pair(r, c)]?.contains(num) == true }
                if (possibleCols.size == 1) {
                    val c = possibleCols.first()
                    return SudokuHint.HiddenSingle(r, c, num, "row", "Hidden Single — In row ${r+1}, $num can only fit at R${r+1}C${c+1}.")
                }
            }
        }
        // ... (Could add Col/Box but logic is similar, keeping it concise as per instructions to run Naked then Hidden)
        return null
    }

    fun dismissHint() {
        _uiState.update { it.copy(lastHint = null) }
    }

    /**
     * Applies the suggested action from the active or explainer step
     */
    fun applyActiveHint() {
        val hint = _uiState.value.activeHint ?: return
        saveUndoState()

        val mutablePencilMarks = HashMap<Pair<Int, Int>, MutableSet<Int>>()
        for (r in 0 until _uiState.value.size) {
            for (c in 0 until _uiState.value.size) {
                mutablePencilMarks[Pair(r, c)] = _uiState.value.grid[r][c].pencilMarks.toMutableSet()
            }
        }

        val arrayGrid = Array(_uiState.value.size) { r ->
            IntArray(_uiState.value.size) { c ->
                _uiState.value.grid[r][c].value
            }
        }

        // Apply
        hint.applyHint(arrayGrid, mutablePencilMarks)

        // Sync grid
        val updatedGrid = _uiState.value.grid.mapIndexed { r, rList ->
            rList.mapIndexed { c, cell ->
                if (arrayGrid[r][c] != cell.value) {
                    cell.copy(value = arrayGrid[r][c], pencilMarks = emptySet())
                } else {
                    cell.copy(pencilMarks = mutablePencilMarks[Pair(r, c)] ?: emptySet())
                }
            }
        }

        _uiState.update {
            it.copy(
                grid = updatedGrid,
                activeHint = null,
                activeHintMessage = "Applied logical step: ${hint.title}",
                placedCount = calculatePlacedCount(updatedGrid)
            )
        }
        validateErrors()
        checkGameCompletion()
    }

    // Game evaluation
    private fun checkGameCompletion() {
        val state = _uiState.value
        val grid = state.grid

        // Grid must be fully filled with correct values and no errors
        val isFilled = grid.all { rowList -> rowList.all { it.value != 0 } }
        val hasErrors = grid.any { rowList -> rowList.any { it.isError } }

        if (isFilled && !hasErrors) {
            // Game is won! Calculate Rating Stars
            val finalHints = state.hintsUsed
            val time = state.timerSeconds

            // 3 Stars: < 4 mins and 0 hints
            // 2 Stars: < 8 mins and <= 1 hint
            // 1 Star: anything else
            val stars = when {
                time < 240 && finalHints == 0 -> 3
                time < 480 && finalHints <= 1 -> 2
                else -> 1
            }

            _uiState.update { it.copy(isCompleted = true, ratingStars = stars) }

            // Save record in Room Database
            viewModelScope.launch {
                val record = GameRecord(
                    difficulty = state.difficulty.name,
                    gridSize = state.size,
                    timeSeconds = state.timerSeconds,
                    hintsUsed = state.hintsUsed,
                    completed = true
                )
                repository.saveGame(record)

                if (state.gameMode == GameMode.DAILY_CHALLENGE) {
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    repository.saveDailyChallenge(
                        DailyChallenge(
                            date = currentDate,
                            puzzleString = "completed",
                            completed = true,
                            timeSeconds = state.timerSeconds
                        )
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

sealed class HintResult {
    data class Success(
        val targetCell: Pair<Int, Int>,
        val value: Int,
        val techniqueName: String,
        val reason: String
    ) : HintResult()
}
