package com.example

import kotlin.random.Random

object SudokuGenerator {

    enum class Difficulty {
        EASY, INTERMEDIATE, HARD, EXTREME
    }

    data class SudokuPuzzle(
        val size: Int,
        val originalGrid: Array<IntArray>, // The complete solved grid
        val puzzleGrid: Array<IntArray>,   // The grid with empty cells
        val difficulty: Difficulty
    )

    /**
     * Helper to get target cells to remove for different sizes and difficulties
     */
    fun getRemovalCountRange(size: Int, difficulty: Difficulty): IntRange {
        return when (size) {
            4 -> when (difficulty) {
                Difficulty.EASY -> 4..5
                Difficulty.INTERMEDIATE -> 6..7
                Difficulty.HARD -> 8..9
                Difficulty.EXTREME -> 10..11
            }
            6 -> when (difficulty) {
                Difficulty.EASY -> 12..14
                Difficulty.INTERMEDIATE -> 15..17
                Difficulty.HARD -> 18..20
                Difficulty.EXTREME -> 21..24
            }
            9 -> when (difficulty) {
                Difficulty.EASY -> 30..35
                Difficulty.INTERMEDIATE -> 40..45
                Difficulty.HARD -> 50..54
                Difficulty.EXTREME -> 55..58
            }
            else -> 30..35
        }
    }

    /**
     * Generates a new Sudoku puzzle using backtracking, ensuring uniqueness of the solution.
     */
    fun generate(size: Int, difficulty: Difficulty, random: Random = Random): SudokuPuzzle {
        // Step 1: Start with an empty grid
        val solvedGrid = Array(size) { IntArray(size) { 0 } }
        
        // Step 2-5: Generate complete valid grid
        generateFullGrid(solvedGrid, size, random)

        // Make copies
        val puzzleGrid = Array(size) { r -> solvedGrid[r].clone() }

        // Get how many cells we target to remove
        val range = getRemovalCountRange(size, difficulty)
        val targetRemovalCount = random.nextInt(range.first, range.last + 1)

        // Step 6: Remove cells while maintaining a unique solution
        removeCellsForPuzzle(puzzleGrid, solvedGrid, size, targetRemovalCount, random)

        return SudokuPuzzle(
            size = size,
            originalGrid = solvedGrid,
            puzzleGrid = puzzleGrid,
            difficulty = difficulty
        )
    }

    private fun generateFullGrid(grid: Array<IntArray>, size: Int, random: Random): Boolean {
        val emptyCell = SudokuSolver.findEmptyCell(grid, size) ?: return true
        val (row, col) = emptyCell

        // Shuffle candidates list randomly (for variety) using the provided Random instance
        val candidates = (1..size).toList().shuffled(random)

        for (num in candidates) {
            if (SudokuSolver.isValid(grid, row, col, num, size)) {
                grid[row][col] = num
                if (generateFullGrid(grid, size, random)) {
                    return true
                }
                grid[row][col] = 0 // Backtrack
            }
        }
        return false
    }

    private fun removeCellsForPuzzle(
        puzzleGrid: Array<IntArray>,
        solvedGrid: Array<IntArray>,
        size: Int,
        targetRemoval: Int,
        random: Random
    ) {
        // Create a list of all coordinates
        val coords = ArrayList<Pair<Int, Int>>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                coords.add(Pair(r, c))
            }
        }
        // Shuffle coordinates to remove in random order
        coords.shuffle(random)

        var removedCount = 0
        for (coord in coords) {
            if (removedCount >= targetRemoval) break

            val (r, col) = coord
            val originalVal = puzzleGrid[r][col]
            if (originalVal == 0) continue

            // Try removing it
            puzzleGrid[r][col] = 0

            // If uniqueness is broken, skip this cell and try another
            val solutions = SudokuSolver.countSolutions(puzzleGrid, size, limit = 2)
            if (solutions == 1) {
                removedCount++
            } else {
                // Restore original value
                puzzleGrid[r][col] = originalVal
            }
        }
    }
}
