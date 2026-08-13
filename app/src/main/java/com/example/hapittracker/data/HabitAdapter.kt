package com.example.hapittracker.data

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.hapittracker.R
import com.example.hapittracker.ReminderReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class HabitAdapter(
    private var habits: MutableList<Habit>,
    private val onItemClick: (Habit) -> Unit,
    private val onDeleteClick: (Habit) -> Unit,
    private val onUpdateHabit: (Habit) -> Unit,
    private val onEditClick: (Habit) -> Unit, // جديد: تعديل الاسم
    private val context: Context
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHabitName: TextView = itemView.findViewById(R.id.tvHabitName)
        private val tvStreak: TextView = itemView.findViewById(R.id.tvStreak)
        private val tvLastDate: TextView = itemView.findViewById(R.id.tvLastDate)
        private val cardHabit: CardView = itemView.findViewById(R.id.cardHabit)
        private val tvCheck: TextView = itemView.findViewById(R.id.tvCheck)
        private val ivReminder: ImageView = itemView.findViewById(R.id.ivReminder)

        fun bind(habit: Habit) {
            tvHabitName.text = habit.name
            tvStreak.text = "🔥 ${habit.streak} days"

            val today = Calendar.getInstance()
            val lastDate = Calendar.getInstance()
            lastDate.timeInMillis = habit.lastCompletedDate

            val isDoneToday = habit.lastCompletedDate!= 0L &&
                    today.get(Calendar.YEAR) == lastDate.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == lastDate.get(Calendar.DAY_OF_YEAR)

            tvLastDate.text = if (habit.lastCompletedDate == 0L) {
                "Not done yet"
            } else {
                val diffInMillis = today.timeInMillis - lastDate.timeInMillis
                val daysAgo = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
                when (daysAgo) {
                    0 -> "Last done: Today"
                    1 -> "Last done: Yesterday"
                    else -> "Last done: $daysAgo days ago - ${dateFormat.format(lastDate.time)}"
                }
            }

            // لون الجرس
            if (habit.reminderTime!= null) {
                ivReminder.setColorFilter(Color.parseColor("#6200EE")) // بنفسجي
            } else {
                ivReminder.setColorFilter(Color.parseColor("#757575")) // رمادي
            }

            if (isDoneToday) {
                cardHabit.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                tvCheck.visibility = View.VISIBLE
                tvCheck.text = "✓"
                tvCheck.setTextColor(Color.parseColor("#4CAF50"))
                tvCheck.textSize = 18f
            } else {
                tvCheck.visibility = View.GONE
                when {
                    habit.streak >= 30 -> {
                        cardHabit.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.gold))
                        tvStreak.text = "👑 ${habit.streak} days"
                    }
                    habit.streak >= 7 -> {
                        cardHabit.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.green_light))
                    }
                    habit.streak >= 3 -> {
                        cardHabit.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.orange_light))
                    }
                    else -> {
                        cardHabit.setCardBackgroundColor(Color.WHITE)
                    }
                }
            }

            // ضغطة عادية على الاسم = تعديل
            tvHabitName.setOnClickListener {
                onEditClick(habit)
            }

            // ضغطة عادية على الجرس = تعيين منبه
            ivReminder.setOnClickListener {
                showTimePicker(habit, adapterPosition)
            }

            // ضغطة طويلة على الجرس = الغاء منبه
            ivReminder.setOnLongClickListener {
                cancelReminder(habit, adapterPosition)
                true
            }

            // ضغطة على الكارد = تعليم اليوم
            cardHabit.setOnClickListener {
                val animation = AnimationUtils.loadAnimation(itemView.context, R.anim.scale_up)
                itemView.startAnimation(animation)
                onItemClick(habit)
            }

            // ضغطة طويلة على الكارد = حذف
            cardHabit.setOnLongClickListener {
                showDeleteDialog(habit)
                true
            }
        }
    }

    private fun showDeleteDialog(habit: Habit) {
        AlertDialog.Builder(context)
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete \"${habit.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                cancelAlarm(habit)
                onDeleteClick(habit)
                Toast.makeText(context, "\"${habit.name}\" deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun cancelAlarm(habit: Habit) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, habit.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun showTimePicker(habit: Habit, position: Int) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(context, { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            habits[position].reminderTime = calendar.timeInMillis
            setAlarm(habits[position])
            onUpdateHabit(habits[position])
            notifyItemChanged(position)

            val time = String.format("%02d:%02d", hour, minute)
            Toast.makeText(context, "Reminder set for ${habit.name} at $time", Toast.LENGTH_SHORT).show()
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun cancelReminder(habit: Habit, position: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, habit.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        habits[position].reminderTime = null
        onUpdateHabit(habits[position])
        notifyItemChanged(position)

        Toast.makeText(context, "Reminder cancelled ❌", Toast.LENGTH_SHORT).show()
    }

    private fun setAlarm(habit: Habit) {
        val intent = Intent(context, ReminderReceiver::class.java)
        intent.putExtra("habitName", habit.name)
        intent.putExtra("habitId", habit.id)

        val pendingIntent = PendingIntent.getBroadcast(
            context, habit.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        habit.reminderTime?.let {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                it,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(habits[position])
    }

    override fun getItemCount(): Int = habits.size

    fun setData(newHabits: MutableList<Habit>) {
        habits = newHabits
        notifyDataSetChanged()
    }
}