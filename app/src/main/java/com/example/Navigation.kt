package com.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

// ==========================================
// TYPE-SAFE DESTINATIONS
// ==========================================

@Serializable
object HomeDestination

@Serializable
object GameDestination

@Serializable
object SolutionExplanationDestination

@Serializable
object CustomPuzzleDestination

@Serializable
object StatsDestination

// ==========================================
// CENTRAL NAV HOST
// ==========================================

@Composable
fun SudokuNavHost(
    navController: NavHostController,
    gameViewModel: GameViewModel,
    statsViewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination,
        modifier = modifier
    ) {
        composable<HomeDestination> {
            HomeScreen(
                navController = navController,
                gameViewModel = gameViewModel
            )
        }
        composable<GameDestination> {
            GameScreen(
                navController = navController,
                viewModel = gameViewModel
            )
        }
        composable<SolutionExplanationDestination> {
            SolutionExplanationScreen(
                navController = navController,
                viewModel = gameViewModel
            )
        }
        composable<CustomPuzzleDestination> {
            CustomPuzzleScreen(
                navController = navController,
                gameViewModel = gameViewModel
            )
        }
        composable<StatsDestination> {
            StatsScreen(
                navController = navController,
                statsViewModel = statsViewModel
            )
        }
    }
}
