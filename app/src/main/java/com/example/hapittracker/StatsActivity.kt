package com.example.hapittracker

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.hapittracker.data.Habit
import com.example.hapittracker.habitviewmodel.HabitViewModel
import java.util.Calendar

class StatsActivity : AppCompatActivity() {

    private lateinit var habitViewModel: HabitViewModel
    private lateinit var tvTotalHabits: TextView
    private lateinit var tvLongestStreak: TextView
    private lateinit var tvDoneToday: TextView
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        supportActionBar?.hide() // ده المهم: بيخفي الشريط الاسود

        btnBack = findViewById(R.id.btnBack)
        tvTotalHabits = findViewById(R.id.tvTotalHabits)
        tvLongestStreak = findViewById(R.id.tvLongestStreak)
        tvDoneToday = findViewById(R.id.tvDoneToday)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        habitViewModel = ViewModelProvider(this, factory)[HabitViewModel::class.java]

        habitViewModel.allHabits.observe(this) { habits ->
            updateStats(habits)
        }
    }

    private fun updateStats(habits: List<Habit>) {
        tvTotalHabits.text = habits.size.toString()
        val longestStreak = habits.maxOfOrNull { it.streak } ?: 0
        tvLongestStreak.text = "$longestStreak day" + if(longestStreak != 1) "s" else "" // صلحت days

        val today = Calendar.getInstance()
        val doneToday = habits.count { habit ->
            if (habit.lastCompletedDate == 0L) false
            else {
                val lastDate = Calendar.getInstance()
                lastDate.timeInMillis = habit.lastCompletedDate
                today.get(Calendar.YEAR) == lastDate.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == lastDate.get(Calendar.DAY_OF_YEAR)
            }
        }
        tvDoneToday.text = doneToday.toString()
    }
}