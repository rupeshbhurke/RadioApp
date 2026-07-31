package com.example.radioapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations WHERE isFavourite = 1")
    fun getFavouriteStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations ORDER BY lastPlayedTime DESC LIMIT 10")
    fun getRecentStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE id = :stationId")
    suspend fun getStationById(stationId: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: StationEntity)

    @Query("UPDATE stations SET isFavourite = :isFavourite WHERE id = :stationId")
    suspend fun updateFavouriteStatus(stationId: String, isFavourite: Boolean)
    
    @Query("UPDATE stations SET lastPlayedTime = :time WHERE id = :stationId")
    suspend fun updateLastPlayedTime(stationId: String, time: Long)
}
