package com.equipo.atrapame.presentation.game

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.equipo.atrapame.R

object GameDialogs {
    
    fun showVictoryDialog(
        context: Context,
        moves: Int,
        time: String,
        onPlayAgain: () -> Unit,
        onMainMenu: () -> Unit
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(R.string.dialog_victory_title)
            .setMessage(
                context.getString(R.string.dialog_victory_message, moves, time)
            )
            .setPositiveButton(R.string.btn_play_again) { dialog, _ ->
                dialog.dismiss()
                onPlayAgain()
            }
            .setNegativeButton(R.string.btn_main_menu) { dialog, _ ->
                dialog.dismiss()
                onMainMenu()
            }
            .setCancelable(false)
            .create()
    }
    
    fun showDefeatDialog(
        context: Context,
        onPlayAgain: () -> Unit,
        onMainMenu: () -> Unit
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(R.string.dialog_defeat_title)
            .setMessage(R.string.dialog_defeat_message)
            .setPositiveButton(R.string.btn_try_again) { dialog, _ ->
                dialog.dismiss()
                onPlayAgain()
            }
            .setNegativeButton(R.string.btn_main_menu) { dialog, _ ->
                dialog.dismiss()
                onMainMenu()
            }
            .setCancelable(false)
            .create()
    }
}
