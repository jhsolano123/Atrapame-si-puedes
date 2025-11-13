package com.equipo.atrapame.presentation.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equipo.atrapame.data.models.Direction
import com.equipo.atrapame.data.models.GameState
import com.equipo.atrapame.data.models.Position
import com.equipo.atrapame.data.models.Score
import com.equipo.atrapame.data.repository.ConfigRepository
import com.equipo.atrapame.data.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameResult(val moves: Int, val timeElapsed: Long)

class GameViewModel(
    private val gameRepository: GameRepository = GameRepository(),
    private val configRepository: ConfigRepository? = null
) : ViewModel() {

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _showVictoryDialog = MutableLiveData<GameResult?>()
    val showVictoryDialog: LiveData<GameResult?> = _showVictoryDialog

    private val _showDefeatDialog = MutableLiveData<Boolean>()
    val showDefeatDialog: LiveData<Boolean> = _showDefeatDialog

    private var enemyMovementJob: Job? = null
    private var timerJob: Job? = null
    private var gameStartTime: Long = 0L
    private var pauseStartTime: Long = 0L
    private var totalPausedTime: Long = 0L
    private var isPaused: Boolean = false

    init {
        initializeGame()
    }

    fun initializeGame(rows: Int = GameState.DEFAULT_ROWS, cols: Int = GameState.DEFAULT_COLS) {
        // Cancelar jobs anteriores
        timerJob?.cancel()
        enemyMovementJob?.cancel()
        
        val obstacles = createDefaultObstacles(rows, cols)
        _gameState.value = GameState.createInitialState(rows, cols, obstacles)
        gameStartTime = System.currentTimeMillis()
        totalPausedTime = 0L
        isPaused = false
        
        startTimerLoop()
        startEnemyMovementLoop()
    }

    fun onDirectionInput(direction: Direction?) {
        if (direction == null || isPaused) return
        val currentState = _gameState.value ?: return
        val newState = currentState.movePlayer(direction)
        if (newState != currentState) {
            _gameState.value = newState
        }
    }
    
    fun pauseGame() {
        isPaused = true
        pauseStartTime = System.currentTimeMillis()
    }
    
    fun resumeGame() {
        if (isPaused) {
            totalPausedTime += System.currentTimeMillis() - pauseStartTime
            isPaused = false
        }
    }

    private fun startTimerLoop() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                val currentState = _gameState.value
                if (currentState != null && !currentState.isGameWon && !currentState.isGameLost && !isPaused) {
                    val elapsed = System.currentTimeMillis() - gameStartTime - totalPausedTime
                    _gameState.value = currentState.copy(timeElapsed = elapsed)
                }
                delay(100) // Actualizar cada 100ms para suavidad
            }
        }
    }

    private fun startEnemyMovementLoop() {
        // Obtener velocidad del enemigo según dificultad
        val enemySpeed = configRepository?.getPlayerConfig()?.difficulty?.enemySpeed?.toLong()
            ?: DEFAULT_ENEMY_MOVE_INTERVAL_MS
        
        enemyMovementJob = viewModelScope.launch {
            while (isActive) {
                val shouldContinue = stepEnemy()
                if (!shouldContinue) break
                delay(enemySpeed)
            }
        }
    }

    private fun stepEnemy(): Boolean {
        val currentState = _gameState.value ?: return true
        if (currentState.isGameWon || currentState.isGameLost || isPaused) {
            return !isPaused // Continuar el loop si está pausado, pero no hacer nada
        }

        val updatedState = currentState.advanceEnemy()
        if (updatedState != currentState) {
            _gameState.value = updatedState
        }

        return !(updatedState.isGameWon || updatedState.isGameLost)
    }


    private fun createDefaultObstacles(rows: Int, cols: Int): List<Position> {
        if (rows < 3 || cols < 3) return emptyList()

        val positions = mutableSetOf<Position>()
        for (row in 1 until rows - 1 step 2) {
            val startCol = (row / 2) % cols
            positions.add(Position(row, startCol))
            val secondaryCol = startCol + 2
            if (secondaryCol < cols) {
                positions.add(Position(row, secondaryCol))
            }
        }

        positions.remove(Position(0, 0))
        positions.remove(Position(rows - 1, cols - 1))

        return positions.toList()
    }

    fun onGameWon() {
        val state = _gameState.value ?: return
        _showVictoryDialog.value = GameResult(state.moves, state.timeElapsed)
    }

    fun onGameLost() {
        _showDefeatDialog.value = true
    }

    suspend fun saveCurrentScore(): Result<String> {
        val state = _gameState.value ?: return Result.failure(Exception("No game state"))
        if (configRepository == null) {
            return Result.failure(Exception("ConfigRepository not available"))
        }
        
        val config = configRepository.getPlayerConfig()
        val score = Score(
            playerName = config.name,
            moves = state.moves,
            timeElapsed = state.timeElapsed,
            difficulty = config.difficulty
        )
        return gameRepository.saveScore(score)
    }

    fun resetDialogEvents() {
        _showVictoryDialog.value = null
        _showDefeatDialog.value = false
    }

    override fun onCleared() {
        enemyMovementJob?.cancel()
        timerJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_ENEMY_MOVE_INTERVAL_MS = 750L
    }
}