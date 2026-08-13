package com.example.hapittracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ReminderReceiver : BroadcastReceiver() {
    private val CHANNEL_ID = "habit_channel"

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)
        val habitName = intent.getStringExtra("habitName")?: "Your Habit"
        val habitId = intent.getIntExtra("habitId", 0)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("⏰ Your Time To: $habitName")
                .setContentText("Don't be lazy do your habit now!🔥")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(habitId, builder.build())
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Habit Reminders", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}