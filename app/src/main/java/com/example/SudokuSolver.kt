package com.example

import kotlin.random.Random

object SudokuSolver {

    /**
     * Get the dimensions of the sub-box for a given grid size.
     * Returns Pair(rows, cols) representing the height and width of the sub-box.
     */
    fun getBoxDimensions(size: Int): Pair<Int, Int> {
        return when (size) {
            4 -> Pair(2, 2)
            6 -> Pair(2, 3)
            9 -> Pair(3, 3)
            else -> Pair(3, 3)
        }
    }

    /**
     * Validates if placing 'num' at (row, col) in a grid of 'size' is valid.
     */
    fun isValid(grid: Array<IntArray>, row: Int, col: Int, num: Int, size: Int): Boolean {
        // Check row
        for (c in 0 until size) {
            if (grid[row][c] == num) return false
        }

        // Check column
        for (r in 0 until size) {
            if (grid[r][col] == num) return false
        }

        // Check sub-box
        val (boxRows, boxCols) = getBoxDimensions(size)
        val startRow = (row / boxRows) * boxRows
        val startCol = (col / boxCols) * boxCols
        for (r in 0 until boxRows) {
            for (c in 0 until boxCols) {
                if (grid[startRow + r][startCol + c] == num) return false
            }
        }

        return true
    }

    /**
     * Finds the next empty cell in the grid.
     * Returns Pair(row, col) or null if grid is full.
     */
    fun findEmptyCell(grid: Array<IntArray>, size: Int): Pair<Int, Int>? {
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c] == 0) {
                    return Pair(r, c)
                }
            }
        }
        return null
    }

    /**
     * Backtracking solver that finds one solution. Modifies grid in place.
     * Returns true if a solution is found, false otherwise.
     */
    fun solve(grid: Array<IntArray>, size: Int, shuffle: Boolean = false): Boolean {
        val emptyCell = findEmptyCell(grid, size) ?: return true
        val (row, col) = emptyCell

        val candidates = (1..size).toList()
        val orderedCandidates = if (shuffle) candidates.shuffled() else candidates

        for (num in orderedCandidates) {
            if (isValid(grid, row, col, num, size)) {
                grid[row][col] = num
                if (solve(grid, size, shuffle)) {
                    return true
                }
                grid[row][col] = 0 // Backtrack
            }
        }
        return false
    }

    /**
     * Counts the number of solutions for a grid, up to a specified limit (e.g., 2 for uniqueness checks).
     * Does not destroy or permanently alter the grid passed in.
     */
    fun countSolutions(grid: Array<IntArray>, size: Int, limit: Int = 2): Int {
        val gridCopy = Array(size) { r -> grid[r].clone() }
        var solutionsCount = 0

        fun solveAndCount(): Boolean {
            val emptyCell = findEmptyCell(gridCopy, size)
            if (emptyCell == null) {
                solutionsCount++
                return solutionsCount >= limit
            }
            val (row, col) = emptyCell

            for (num in 1..size) {
                if (isValid(gridCopy, row, col, num, size)) {
                    gridCopy[row][col] = num
                    if (solveAndCount()) {
                        return true // Exceeded or hit limit
                    }
                    gridCopy[row][col] = 0 // Backtrack
                }
            }
            return false
        }

        solveAndCount()
        return solutionsCount
    }
}
