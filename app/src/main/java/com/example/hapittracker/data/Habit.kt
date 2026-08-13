package com.example.hapittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Time

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val streak: Int = 0,
    var reminderTime: Long?=null,
    val days: String = "1,2,3,4,5,6,7", // ايام الاسبوع
    val lastCompletedDate: Long = 0L // عشان نحسب الستريك صح بعدين
)
