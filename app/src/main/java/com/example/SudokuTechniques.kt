package com.example

sealed class SudokuHint {
    abstract val title: String
    abstract val description: String
    abstract val targetCells: List<Pair<Int, Int>>
    abstract val affectedNumber: Int? // A specific number if applicable (e.g. naked single val, hidden single, pointee)
    abstract fun applyHint(grid: Array<IntArray>, pencilMarks: HashMap<Pair<Int, Int>, MutableSet<Int>>)

    data class NakedSingle(
        val row: Int,
        val col: Int,
        val num: Int,
        override val description: String
    ) : SudokuHint() {
        override val title = "Naked Single"
        override val targetCells = listOf(Pair(row, col))
        override val affectedNumber = num
        override fun applyHint(grid: Array<IntArray>, pencilMarks: HashMap<Pair<Int, Int>, MutableSet<Int>>) {
            grid[row][col] = num
            pencilMarks.remove(Pair(row, col))
        }
    }

    data class HiddenSingle(
        val row: Int,
        val col: Int,
        val num: Int,
        val unitType: String,
        override val description: String
    ) : SudokuHint() {
        override val title = "Hidden Single"
        override val targetCells = listOf(Pair(row, col))
        override val affectedNumber = num
        override fun applyHint(grid: Array<IntArray>, pencilMarks: HashMap<Pair<Int, Int>, MutableSet<Int>>) {
            grid[row][col] = num
            pencilMarks.remove(Pair(row, col))
        }
    }

    data class NakedPair(
        val cell1: Pair<Int, Int>,
        val cell2: Pair<Int, Int>,
        val nums: Set<Int>,
        val unitType: String,
        override val description: String,
        val eliminations: List<Pair<Pair<Int, Int>, Int>> // List of Pair(cell, candidateToEliminate)
    ) : SudokuHint() {
        override val title = "Naked Pair"
        override val targetCells = listOf(cell1, cell2)
        override val affectedNumber = null
        override fun applyHint(grid: Array<IntArray>, pencilMarks: HashMap<Pair<Int, Int>, MutableSet<Int>>) {
            for (elim in eliminations) {
                pencilMarks[elim.first]?.remove(elim.second)
            }
        }
    }

    data class HiddenPair(
        val cell1: Pair<Int, Int>,
        val cell2: Pair<Int, Int>,
        val nums: Set<Int>,
        val unitType: String,
        override val description: String,
        val eliminations: List<Pair<Pair<Int, Int>, Int>> // List of Pair(cell, candidateToEliminate)
    ) : SudokuHint() {
        override val title = "Hidden Pair"
        override val targetCells = listOf(cell1, cell2)
        override val affectedNumber = null
        override fun applyHint(grid: Array<IntArray>, pencilMarks: HashMap<Pair<Int, Int>, MutableSet<Int>>) {
            for (elim in eliminations) {
                pencilMarks[elim.first]?.remove(elim.second)
            }
        }
    }

    data class PointingPairs(
        val boxIndex: Int,
        val rowOrCol: String,
        val unitIndex: Int,
        val num: Int,
        override val description: String,
        val eliminations: List<Pair<Pair<Int, Int>, Int>>
    ) : SudokuHint() {
        override val title = "Pointing Pair"
        override val targetCells = emptyList<Pair<Int, Int>>() // Calculated dynamically in UI
        override val affectedNumber = num
        override fun applyHint(grid: Array<IntArray>, pencilMarks: HashMap<Pair<Int, Int>, MutableSet<Int>>) {
            for (elim in eliminations) {
                pencilMarks[elim.first]?.remove(elim.second)
            }
        }
    }

    data class XWing(
        val lines: Pair<Int, Int>, // rows or cols
        val crossLines: Pair<Int, Int>, // cols if rows, or rows if cols
        val num: Int,
        val isRowBased: Boolean,
        override val description: String,
        val eliminations: List<Pair<Pair<Int, Int>, Int>>
    ) : SudokuHint() {
        override val title = "X-Wing"
        override val targetCells = listOf(
            Pair(if (isRowBased) lines.first else crossLines.first, if (isRowBased) crossLines.first else lines.first),
            Pair(if (isRowBased) lines.first else crossLines.second, if (isRowBased) crossLines.second else lines.first),
            Pair(if (isRowBased) lines.second else crossLines.first, if (isRowBased) crossLines.first else lines.second),
            Pair(if (isRowBased) lines.second else crossLines.second, if (isRowBased) crossLines.second else lines.second),
        )
        override val affectedNumber = num
        override fun applyHint(grid: Array<IntArray>, pencilMarks: HashMap<Pair<Int, Int>, MutableSet<Int>>) {
            for (elim in eliminations) {
                pencilMarks[elim.first]?.remove(elim.second)
            }
        }
    }
}

object SudokuTechniques {

    /**
     * Finds the simplest logical technique applicable to the current grid state.
     * Returns null if no structured human-solving technique can be logically deduced, or if empty.
     */
    fun findSimplestHint(grid: Array<IntArray>, size: Int, playerPencilMarks: Map<Pair<Int, Int>, Set<Int>>? = null): SudokuHint? {
        val candidates = calculateAllCandidates(grid, size)

        // 1. Naked Singles
        findNakedSingle(grid, size, candidates)?.let { return it }

        // 2. Hidden Singles
        findHiddenSingle(grid, size, candidates)?.let { return it }

        // Only search for advanced pencil-based techniques in standard 9x9 games
        if (size == 9) {
            // 3. Naked Pairs
            findNakedPair(grid, candidates)?.let { return it }

            // 4. Hidden Pairs
            findHiddenPair(grid, candidates)?.let { return it }

            // 5. Pointing Pairs
            findPointingPairs(grid, candidates)?.let { return it }

            // 6. X-Wing
            findXWing(grid, candidates)?.let { return it }
        }

        return null
    }

    /**
     * Calculates the set of possible numbers for each empty cell based on row, column, and box constraints.
     */
    fun calculateAllCandidates(grid: Array<IntArray>, size: Int): Map<Pair<Int, Int>, Set<Int>> {
        val candidatesMap = HashMap<Pair<Int, Int>, Set<Int>>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c] == 0) {
                    val possible = HashSet<Int>()
                    for (num in 1..size) {
                        if (SudokuSolver.isValid(grid, r, c, num, size)) {
                            possible.add(num)
                        }
                    }
                    candidatesMap[Pair(r, c)] = possible
                }
            }
        }
        return candidatesMap
    }

    private fun findNakedSingle(grid: Array<IntArray>, size: Int, candidates: Map<Pair<Int, Int>, Set<Int>>): SudokuHint? {
        for ((cell, ops) in candidates) {
            if (ops.size == 1) {
                val num = ops.first()
                val (r, c) = cell
                return SudokuHint.NakedSingle(
                    row = r,
                    col = c,
                    num = num,
                    description = "Naked Single at R${r + 1}C${c + 1} — only $num fits here because other values are occupied in the same row, column, or sub-box."
                )
            }
        }
        return null
    }

    private fun findHiddenSingle(grid: Array<IntArray>, size: Int, candidates: Map<Pair<Int, Int>, Set<Int>>): SudokuHint? {
        // Rows
        for (r in 0 until size) {
            for (num in 1..size) {
                if (grid[r].contains(num)) continue
                val possibleCols = (0 until size).filter { c -> grid[r][c] == 0 && candidates[Pair(r, c)]?.contains(num) == true }
                if (possibleCols.size == 1) {
                    val c = possibleCols.first()
                    return SudokuHint.HiddenSingle(
                        row = r, col = c, num = num, unitType = "row",
                        description = "Hidden Single in Row ${r + 1} — the number $num can only go in R${r + 1}C${c + 1} in this row."
                    )
                }
            }
        }

        // Columns
        for (c in 0 until size) {
            for (num in 1..size) {
                val alreadyInCol = (0 until size).any { r -> grid[r][c] == num }
                if (alreadyInCol) continue
                val possibleRows = (0 until size).filter { r -> grid[r][c] == 0 && candidates[Pair(r, c)]?.contains(num) == true }
                if (possibleRows.size == 1) {
                    val r = possibleRows.first()
                    return SudokuHint.HiddenSingle(
                        row = r, col = c, num = num, unitType = "column",
                        description = "Hidden Single in Column ${c + 1} — the number $num can only go in R${r + 1}C${c + 1} in this column."
                    )
                }
            }
        }

        // Boxes
        val (boxRows, boxCols) = SudokuSolver.getBoxDimensions(size)
        val numBoxes = size
        for (b in 0 until numBoxes) {
            val startRow = (b / (size / boxRows)) * boxRows
            val startCol = (b % (size / boxRows)) * boxCols
            for (num in 1..size) {
                // Check if already in box
                var existsInBox = false
                innerLoop@ for (r in 0 until boxRows) {
                    for (c in 0 until boxCols) {
                        if (grid[startRow + r][startCol + c] == num) {
                            existsInBox = true
                            break@innerLoop
                        }
                    }
                }
                if (existsInBox) continue

                val possibleCells = ArrayList<Pair<Int, Int>>()
                for (r in 0 until boxRows) {
                    for (c in 0 until boxCols) {
                        val cell = Pair(startRow + r, startCol + c)
                        if (grid[cell.first][cell.second] == 0 && candidates[cell]?.contains(num) == true) {
                            possibleCells.add(cell)
                        }
                    }
                }

                if (possibleCells.size == 1) {
                    val cell = possibleCells.first()
                    return SudokuHint.HiddenSingle(
                        row = cell.first, col = cell.second, num = num, unitType = "box",
                        description = "Hidden Single in Sub-box ${b + 1} — the number $num can only go in R${cell.first + 1}C${cell.second + 1} inside this sub-box."
                    )
                }
            }
        }

        return null
    }

    private fun findNakedPair(grid: Array<IntArray>, candidates: Map<Pair<Int, Int>, Set<Int>>): SudokuHint? {
        // Rows
        for (r in 0..8) {
            val emptyCellsInRow = (0..8).map { Pair(r, it) }.filter { grid[it.first][it.second] == 0 }
            for (i in emptyCellsInRow.indices) {
                for (j in i + 1 until emptyCellsInRow.size) {
                    val cell1 = emptyCellsInRow[i]
                    val cell2 = emptyCellsInRow[j]
                    val cand1 = candidates[cell1] ?: emptySet()
                    val cand2 = candidates[cell2] ?: emptySet()
                    if (cand1.size == 2 && cand1 == cand2) {
                        // Found Naked Pair in Row r!
                        // Check if we can eliminate candidates from any OTHER empty cell in this row
                        val eliminations = ArrayList<Pair<Pair<Int, Int>, Int>>()
                        for (otherCell in emptyCellsInRow) {
                            if (otherCell != cell1 && otherCell != cell2) {
                                val otherCand = candidates[otherCell] ?: emptySet()
                                for (num in cand1) {
                                    if (otherCand.contains(num)) {
                                        eliminations.add(Pair(otherCell, num))
                                    }
                                }
                            }
                        }
                        if (eliminations.isNotEmpty()) {
                            return SudokuHint.NakedPair(
                                cell1 = cell1, cell2 = cell2, nums = cand1, unitType = "row",
                                description = "Naked Pair in Row ${r + 1} — Cells R${cell1.first + 1}C${cell1.second + 1} and R${cell2.first + 1}C${cell2.second + 1} both have only candidate options ${cand1}. Therefore, these two numbers can be removed as candidates from all other cells in Row ${r + 1}.",
                                eliminations = eliminations
                            )
                        }
                    }
                }
            }
        }

        // Columns
        for (c in 0..8) {
            val emptyCellsInCol = (0..8).map { Pair(it, c) }.filter { grid[it.first][it.second] == 0 }
            for (i in emptyCellsInCol.indices) {
                for (j in i + 1 until emptyCellsInCol.size) {
                    val cell1 = emptyCellsInCol[i]
                    val cell2 = emptyCellsInCol[j]
                    val cand1 = candidates[cell1] ?: emptySet()
                    val cand2 = candidates[cell2] ?: emptySet()
                    if (cand1.size == 2 && cand1 == cand2) {
                        val eliminations = ArrayList<Pair<Pair<Int, Int>, Int>>()
                        for (otherCell in emptyCellsInCol) {
                            if (otherCell != cell1 && otherCell != cell2) {
                                val otherCand = candidates[otherCell] ?: emptySet()
                                for (num in cand1) {
                                    if (otherCand.contains(num)) {
                                        eliminations.add(Pair(otherCell, num))
                                    }
                                }
                            }
                        }
                        if (eliminations.isNotEmpty()) {
                            return SudokuHint.NakedPair(
                                cell1 = cell1, cell2 = cell2, nums = cand1, unitType = "column",
                                description = "Naked Pair in Column ${c + 1} — Cells R${cell1.first + 1}C${cell1.second + 1} and R${cell2.first + 1}C${cell2.second + 1} both have only candidate options ${cand1}. Therefore, these two numbers can be removed as candidates from all other cells in Column ${c + 1}.",
                                eliminations = eliminations
                            )
                        }
                    }
                }
            }
        }

        return null
    }

    private fun findHiddenPair(grid: Array<IntArray>, candidates: Map<Pair<Int, Int>, Set<Int>>): SudokuHint? {
        // Rows
        for (r in 0..8) {
            val emptyCellsInRow = (0..8).map { Pair(r, it) }.filter { grid[it.first][it.second] == 0 }
            val numbersCandidates = HashMap<Int, List<Pair<Int, Int>>>()
            for (num in 1..9) {
                val cellsWithNum = emptyCellsInRow.filter { candidates[it]?.contains(num) == true }
                if (cellsWithNum.size == 2) {
                    numbersCandidates[num] = cellsWithNum
                }
            }
            val numKeys = numbersCandidates.keys.toList()
            for (i in numKeys.indices) {
                for (j in i + 1 until numKeys.size) {
                    val num1 = numKeys[i]
                    val num2 = numKeys[j]
                    val cells1 = numbersCandidates[num1]!!
                    val cells2 = numbersCandidates[num2]!!
                    if (cells1 == cells2) {
                        // Found Hidden Pair of num1, num2 in these two cells!
                        val cell1 = cells1[0]
                        val cell2 = cells1[1]

                        // Check if these two cells have other candidates that can be eliminated
                        val eliminations = ArrayList<Pair<Pair<Int, Int>, Int>>()
                        val rawCand1 = candidates[cell1] ?: emptySet()
                        val rawCand2 = candidates[cell2] ?: emptySet()
                        val targetNums = setOf(num1, num2)

                        for (n in rawCand1) {
                            if (!targetNums.contains(n)) eliminations.add(Pair(cell1, n))
                        }
                        for (n in rawCand2) {
                            if (!targetNums.contains(n)) eliminations.add(Pair(cell2, n))
                        }

                        if (eliminations.isNotEmpty()) {
                            return SudokuHint.HiddenPair(
                                cell1 = cell1, cell2 = cell2, nums = targetNums, unitType = "row",
                                description = "Hidden Pair in Row ${r + 1} — The numbers $num1 and $num2 only occur as candidates in R${cell1.first + 1}C${cell1.second + 1} and R${cell2.first + 1}C${cell2.second + 1} inside this row. All other candidates can be eliminated from these two cells.",
                                eliminations = eliminations
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    private fun findPointingPairs(grid: Array<IntArray>, candidates: Map<Pair<Int, Int>, Set<Int>>): SudokuHint? {
        for (b in 0..8) {
            val startRow = (b / 3) * 3
            val startCol = (b % 3) * 3

            for (num in 1..9) {
                // Find all cells inside sub-box that can contain num
                val possibleCellsInBox = ArrayList<Pair<Int, Int>>()
                for (r in 0..2) {
                    for (c in 0..2) {
                        val cell = Pair(startRow + r, startCol + c)
                        if (grid[cell.first][cell.second] == 0 && candidates[cell]?.contains(num) == true) {
                            possibleCellsInBox.add(cell)
                        }
                    }
                }

                if (possibleCellsInBox.size >= 2 && possibleCellsInBox.size <= 3) {
                    val uniqueRow = possibleCellsInBox.map { it.first }.distinct()
                    if (uniqueRow.size == 1) {
                        val targetRow = uniqueRow[0]
                        // We have Pointing Pair horizontally!
                        // Can we eliminate `num` from cells on the same row outside this box?
                        val eliminations = ArrayList<Pair<Pair<Int, Int>, Int>>()
                        for (c in 0..8) {
                            val inBox = c >= startCol && c < startCol + 3
                            if (!inBox) {
                                val cell = Pair(targetRow, c)
                                if (grid[cell.first][cell.second] == 0 && candidates[cell]?.contains(num) == true) {
                                    eliminations.add(Pair(cell, num))
                                }
                            }
                        }
                        if (eliminations.isNotEmpty()) {
                            return SudokuHint.PointingPairs(
                                boxIndex = b, rowOrCol = "row", unitIndex = targetRow, num = num,
                                description = "Pointing Pair in Sub-box ${b + 1} — In this box, the candidate $num only appears inside Row ${targetRow + 1}. Thus, $num must go in one of these cells, which eliminates $num as an option from all other cells in Row ${targetRow + 1}.",
                                eliminations = eliminations
                            )
                        }
                    }

                    val uniqueCol = possibleCellsInBox.map { it.second }.distinct()
                    if (uniqueCol.size == 1) {
                        val targetCol = uniqueCol[0]
                        // Pointing Pair vertically!
                        val eliminations = ArrayList<Pair<Pair<Int, Int>, Int>>()
                        for (r in 0..8) {
                            val inBox = r >= startRow && r < startRow + 3
                            if (!inBox) {
                                val cell = Pair(r, targetCol)
                                if (grid[cell.first][cell.second] == 0 && candidates[cell]?.contains(num) == true) {
                                    eliminations.add(Pair(cell, num))
                                }
                            }
                        }
                        if (eliminations.isNotEmpty()) {
                            return SudokuHint.PointingPairs(
                                boxIndex = b, rowOrCol = "column", unitIndex = targetCol, num = num,
                                description = "Pointing Pair in Sub-box ${b + 1} — In this box, the candidate $num only appears inside Column ${targetCol + 1}. Thus, $num must go in one of these cells, which eliminates $num from all other cells in Column ${targetCol + 1}.",
                                eliminations = eliminations
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    private fun findXWing(grid: Array<IntArray>, candidates: Map<Pair<Int, Int>, Set<Int>>): SudokuHint? {
        // Row-based X-Wing
        for (num in 1..9) {
            val rowsHoldingTwoCandidateCols = HashMap<Int, Pair<Int, Int>>()
            for (r in 0..8) {
                val candidateCols = (0..8).filter { c -> grid[r][c] == 0 && candidates[Pair(r, c)]?.contains(num) == true }
                if (candidateCols.size == 2) {
                    rowsHoldingTwoCandidateCols[r] = Pair(candidateCols[0], candidateCols[1])
                }
            }

            val rowKeys = rowsHoldingTwoCandidateCols.keys.toList()
            for (i in rowKeys.indices) {
                for (j in i + 1 until rowKeys.size) {
                    val r1 = rowKeys[i]
                    val r2 = rowKeys[j]
                    val cols1 = rowsHoldingTwoCandidateCols[r1]!!
                    val cols2 = rowsHoldingTwoCandidateCols[r2]!!
                    if (cols1 == cols2) {
                        // Found a possible Row-based X-Wing on columns cols1.first and cols1.second
                        val c1 = cols1.first
                        val c2 = cols1.second

                        // Check if we can eliminate num in these two columns for other rows
                        val eliminations = ArrayList<Pair<Pair<Int, Int>, Int>>()
                        for (r in 0..8) {
                            if (r != r1 && r != r2) {
                                val cell1 = Pair(r, c1)
                                val cell2 = Pair(r, c2)
                                if (grid[cell1.first][cell1.second] == 0 && candidates[cell1]?.contains(num) == true) {
                                    eliminations.add(Pair(cell1, num))
                                }
                                if (grid[cell2.first][cell2.second] == 0 && candidates[cell2]?.contains(num) == true) {
                                    eliminations.add(Pair(cell2, num))
                                }
                            }
                        }

                        if (eliminations.isNotEmpty()) {
                            return SudokuHint.XWing(
                                lines = Pair(r1, r2), crossLines = Pair(c1, c2), num = num, isRowBased = true,
                                description = "X-Wing on $num — Rows ${r1 + 1} and ${r2 + 1} can only place the number $num in columns ${c1 + 1} and ${c2 + 1}. This creates a perfect loop containing the solution for both rows, allowing us to safely eliminate candidate $num from all other rows in columns ${c1 + 1} and ${c2 + 1}.",
                                eliminations = eliminations
                            )
                        }
                    }
                }
            }
        }
        return null
    }
}
