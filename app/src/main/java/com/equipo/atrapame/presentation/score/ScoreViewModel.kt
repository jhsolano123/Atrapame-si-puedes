package com.equipo.atrapame.presentation.score

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equipo.atrapame.data.models.Score
import com.equipo.atrapame.data.repository.GameRepository
import kotlinx.coroutines.launch

class ScoreViewModel(
    private val repository: GameRepository = GameRepository()
) : ViewModel() {

    private val _scores = MutableLiveData<List<Score>>()
    val scores: LiveData<List<Score>> = _scores

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadTopScores()
    }

    fun loadTopScores(limit: Int = 10) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val result = repository.getTopScores(limit)
            result.fold(
                onSuccess = { scoreList ->
                    _scores.value = scoreList
                    _loading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Error desconocido"
                    _scores.value = emptyList()
                    _loading.value = false
                }
            )
        }
    }

    fun loadPlayerScores(playerName: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val result = repository.getPlayerScores(playerName)
            result.fold(
                onSuccess = { scoreList ->
                    _scores.value = scoreList
                    _loading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Error desconocido"
                    _scores.value = emptyList()
                    _loading.value = false
                }
            )
        }
    }

    fun retry() {
        loadTopScores()
    }
}
