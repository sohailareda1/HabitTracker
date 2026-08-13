package com.example.hapittracker

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hapittracker.data.Habit
import com.example.hapittracker.data.HabitAdapter
import com.example.hapittracker.habitviewmodel.HabitViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var habitViewModel: HabitViewModel
    private lateinit var adapter: HabitAdapter
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)!= PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val btnStats = findViewById<Button>(R.id.btnStats)
        fabAdd = findViewById(R.id.fabAdd)

        btnStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        habitViewModel = ViewModelProvider(this, factory)[HabitViewModel::class.java]

        val rvHabits = findViewById<RecyclerView>(R.id.rvHabits)

        adapter = HabitAdapter(
            mutableListOf(),
            onItemClick = { habit -> habitViewModel.toggleHabit(habit) },
            onDeleteClick = { habit -> habitViewModel.delete(habit) },
            onUpdateHabit = { habit -> habitViewModel.update(habit) },
            onEditClick = { habit -> showEditHabitDialog(habit) }, // جديد
            context = this
        )
        rvHabits.adapter = adapter
        rvHabits.layoutManager = LinearLayoutManager(this)

        habitViewModel.allHabits.observe(this) { habits ->
            habits?.let {
                adapter.setData(it.toMutableList())
                rescheduleAllAlarms(it)
            }
        }

        fabAdd.setOnClickListener { showAddHabitDialog() }
    }

    private fun rescheduleAllAlarms(habits: List<Habit>) {
        habits.forEach { habit ->
            habit.reminderTime?.let { time ->
                if (time > System.currentTimeMillis()) {
                    val intent = Intent(this, ReminderReceiver::class.java)
                    intent.putExtra("habitName", habit.name)
                    intent.putExtra("habitId", habit.id)

                    val pendingIntent = PendingIntent.getBroadcast(
                        this, habit.id, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        time,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                }
            }
        }
    }

    // جديد: Dialog تعديل الاسم
    private fun showEditHabitDialog(habit: Habit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Habit")

        val input = EditText(this)
        input.setText(habit.name)
        input.setSelection(habit.name.length)
        input.setPadding(32, 16, 32, 16)
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotBlank() && newName!= habit.name) {
                val updatedHabit = habit.copy(name = newName)
                habitViewModel.update(updatedHabit)
                Toast.makeText(this, "Habit updated ✏️", Toast.LENGTH_SHORT).show()
            } else if (newName.isBlank()) {
                Toast.makeText(this, "Name can't be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showAddHabitDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New Habit")
        val input = EditText(this)
        input.setPadding(32, 16, 32, 16)
        builder.setView(input)
        builder.setPositiveButton("Add") { _, _ ->
            val habitName = input.text.toString().trim()
            if (habitName.isNotBlank()) {
                val habit = Habit(name = habitName)
                habitViewModel.insert(habit) { success ->
                    Toast.makeText(this, if(success) "Added ✅" else "Already Exist", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter habit name", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}