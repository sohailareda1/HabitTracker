package com.example.hapittracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id DESC")
    fun getAllHabits(): LiveData<List<Habit>>

    @Query("SELECT * FROM habits WHERE name = :name LIMIT 1")
    suspend fun getHabitByName(name: String): Habit?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(habit: Habit): Long // خليتها ترجع Long عشان ناخد الـ id

    @Update
    suspend fun update(habit: Habit) // <-- دي اللي ناقصة عندك

    @Delete
    suspend fun delete(habit: Habit)
}