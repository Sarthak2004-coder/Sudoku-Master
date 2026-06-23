<div align="center">

# 🧩 Sudoku Master

### A feature-rich Android Sudoku app that doesn't just let you play — it teaches you *how* to think.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09-4285F4?style=flat&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room DB](https://img.shields.io/badge/Room%20Database-2.7-green?style=flat)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat)](LICENSE)

</div>

---

## 📌 Overview

**Sudoku Master** is a fully offline Android application built with Kotlin and Jetpack Compose. It goes beyond a standard puzzle app by incorporating a **rule-based hint engine** that actively teaches human solving techniques — explaining *why* a move works, not just *what* to do. Puzzles are algorithmically generated using **recursive backtracking with uniqueness guarantees**, ensuring every board has exactly one valid solution.

---

## ✨ Key Features

### 🎮 User-Facing
- **4 difficulty levels** across **3 grid sizes** (4×4, 6×6, 9×9) — 12 distinct challenge configurations
- **Daily Challenge mode** — seeded daily puzzle with persistent completion tracking via Room DB
- **Custom Puzzle Import** — enter any Sudoku (e.g. from a newspaper) manually with real-time validation, then play it in-app
- **Pencil Mark system** — candidate note-taking just like a real Sudoku solver
- **Unlimited Undo** — full state stack with snapshot-based rollback
- **Live timer** with pause/resume and a star-rating system on completion
- **Stats screen** — tracks solve history, hints used, and time per difficulty

### ⚙️ Technical Highlights
- **Uniqueness-guaranteed puzzle generation** — cells are only removed if the resulting board still has exactly one solution (verified via a bounded `countSolutions()` call)
- **6-tier hint engine** — escalates from Naked Singles → Hidden Singles → Naked Pairs → Hidden Pairs → Pointing Pairs → X-Wing
- **MVVM architecture** with `StateFlow`-powered reactive UI — zero direct View manipulation
- **Repository pattern** with Room DAOs and Kotlin Coroutines for all DB operations
- **Sealed class hint system** — each technique is a typed `SudokuHint` subclass with its own `applyHint()` logic

---

## 🛠️ Tech Stack & Architecture

| Layer | Technology | Why |
|---|---|---|
| **Language** | Kotlin 2.2 | Concise, null-safe, coroutine-native |
| **UI** | Jetpack Compose + Material 3 | Declarative UI, no XML layouts |
| **Architecture** | MVVM + Repository | Separation of concerns, testable ViewModels |
| **State** | `StateFlow` + `viewModelScope` | Reactive, lifecycle-aware state management |
| **Persistence** | Room DB 2.7 (2 entities, 1 DAO) | Offline-first game records and daily challenges |
| **Navigation** | Navigation Compose 2.8 | Type-safe, single-activity navigation |
| **Async** | Kotlin Coroutines | Non-blocking puzzle generation and DB writes |
| **Build** | Gradle Version Catalog (`libs.versions.toml`) | Centralised dependency management |

---

## 🧠 The Algorithm: How Sudoku Generation Works

This is the most technically interesting part of the project. Puzzle generation happens in **three stages**:

### Stage 1 — Generate a Complete Valid Grid
```
generateFullGrid(grid, size, random)
```
A recursive backtracking function fills an empty grid cell by cell. At each empty cell, it:
1. Shuffles the candidate numbers `(1..size)` randomly for variety
2. Tries each candidate — validating against its **row**, **column**, and **sub-box**
3. Recurses to the next cell; if no candidate fits, it backtracks and tries the next number

This guarantees a fully-filled, valid Sudoku board every time.

### Stage 2 — Remove Cells (With Uniqueness Enforcement)
```
removeCellsForPuzzle(puzzleGrid, solvedGrid, size, targetRemoval, random)
```
Cells are removed in randomised order. Before each removal is accepted, the solver runs `countSolutions(limit = 2)` on the modified board. If it finds more than 1 solution, the cell is **restored** — ensuring the final puzzle always has **exactly one valid solution**.

| Difficulty | Cells Removed (9×9) |
|---|---|
| Easy | 30–35 |
| Intermediate | 40–45 |
| Hard | 50–54 |
| Extreme | 55–58 |

### Stage 3 — Hint Engine (Rule-Based Solver)
```
SudokuTechniques.findSimplestHint(grid, size, pencilMarks)
```
Rather than back-solving with brute force, the hint engine applies **human logic techniques in escalating order**:

| Priority | Technique | What It Does |
|---|---|---|
| 1 | **Naked Single** | Only 1 candidate fits a cell |
| 2 | **Hidden Single** | A number can only go in one cell within a row/col/box |
| 3 | **Naked Pair** | Two cells share the same 2 candidates → eliminate from rest of unit |
| 4 | **Hidden Pair** | Two numbers only appear in two cells → eliminate other candidates from those cells |
| 5 | **Pointing Pair** | Candidates in a box align on one row/col → eliminate from that row/col outside the box |
| 6 | **X-Wing** | Candidate appears in only 2 cells across 2 rows/cols → cross-elimination |

Each technique returns a typed `SudokuHint` sealed class instance with a human-readable description — so the app explains *why* the hint works, not just which cell to fill.

---

## 📸 UI / UX Showcase

> 📷 **Screenshots coming soon** — add your own below after capturing from device

| Home Screen | Game Board | Hint Explanation | Stats Screen |
|---|---|---|---|
| `screenshots/home.png` | `screenshots/game.png` | `screenshots/hint.png` | `screenshots/stats.png` |

*To add screenshots: capture from your device/emulator, place `.png` files in a `/screenshots` folder in the repo root, and update the paths above.*

---

## 🚀 Installation & Setup

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 26+
- Kotlin 2.2+

### Clone & Run
```bash
# Clone the repository
git clone https://github.com/Sarthak2004-coder/Sudoku-Master.git

# Open in Android Studio
# File → Open → Select the cloned folder

# Sync Gradle, then Run on emulator or physical device
# Run → Run 'app' (Shift + F10)
```

No API keys or external services required — the app is fully offline.

---

## 🗺️ Future Roadmap

- [ ] **Difficulty auto-tuning** — analyse which techniques are required to solve a generated puzzle and assign difficulty based on the highest technique needed (rather than cell count alone)
- [ ] **Solution Explanation Walkthrough** — step through the full solution with each technique highlighted and explained, as a learning mode
- [ ] **Cloud Sync & Leaderboard** — Firebase integration to sync stats across devices and compare daily challenge solve times globally

---

## 👤 About the Developer

Built by **Sarthak Jain** — B.E. graduate in AI & Data Science, Android Developer, and ML enthusiast.

| | |
|---|---|
| 🔗 LinkedIn | [linkedin.com/in/sarthak-jain-b96544312](https://linkedin.com/in/sarthak-jain-b96544312) |
| 💻 GitHub | [github.com/Sarthak2004-coder](https://github.com/Sarthak2004-coder) |
| 📧 Email | sarthakjain1968@gmail.com |

---

<div align="center">
  <sub>Built with ❤️ using Kotlin & Jetpack Compose</sub>
</div>
