package com.example.hapittracker.habitviewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.hapittracker.data.Habit
import com.example.hapittracker.data.HabitDatabase
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val habitDao = HabitDatabase.getDatabase(application).habitDao()
    val allHabits: LiveData<List<Habit>> = habitDao.getAllHabits()

    fun insert(habit: Habit, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val existingHabit = habitDao.getHabitByName(habit.name)
        if (existingHabit == null) {
            habitDao.insert(habit)
            onResult(true)
        } else {
            onResult(false)
        }
    }

    fun update(habit: Habit) = viewModelScope.launch {
        habitDao.update(habit)
    }

    fun delete(habit: Habit) = viewModelScope.launch {
        habitDao.delete(habit)
    }

    // Toggle: ضغطة تعلم، ضغطة تشيل
    fun toggleHabit(habit: Habit) = viewModelScope.launch {
        val today = Calendar.getInstance()
        val lastDate = Calendar.getInstance()
        lastDate.timeInMillis = habit.lastCompletedDate

        val isDoneToday = habit.lastCompletedDate!= 0L &&
                today.get(Calendar.YEAR) == lastDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == lastDate.get(Calendar.DAY_OF_YEAR)

        val updatedHabit = if (isDoneToday) {
            // نشيلها
            habit.copy(
                streak = if (habit.streak > 0) habit.streak - 1 else 0,
                lastCompletedDate = 0L
            )
        } else {
            // نعلمها
            val diffInMillis = today.timeInMillis - lastDate.timeInMillis
            val daysDifference = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

            val newStreak = if (daysDifference == 1) {
                habit.streak + 1 // كملتي
            } else {
                1 // ابدأي من جديد
            }

            habit.copy(
                streak = newStreak,
                lastCompletedDate = System.currentTimeMillis()
            )
        }
        habitDao.update(updatedHabit)
    }


    fun checkAndResetStreak(habit: Habit) = viewModelScope.launch {
        if (habit.lastCompletedDate == 0L) return@launch

        val today = Calendar.getInstance()
        val lastDate = Calendar.getInstance()
        lastDate.timeInMillis = habit.lastCompletedDate
        val diffInMillis = today.timeInMillis - lastDate.timeInMillis
        val daysDifference = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

        if (daysDifference > 1) {
            val resetHabit = habit.copy(streak = 0)
            habitDao.update(resetHabit)
        }
    }

}