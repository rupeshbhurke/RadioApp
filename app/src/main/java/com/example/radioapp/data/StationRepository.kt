package com.example.radioapp.data

import android.content.Context
import com.example.radioapp.data.local.AppDatabase
import com.example.radioapp.data.local.AppPreferences
import com.example.radioapp.data.local.toEntity
import com.example.radioapp.data.local.toStation
import com.example.radioapp.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class StationRepository(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val stationDao = database.stationDao()
    val preferences = AppPreferences(context)

    private var cachedStations: List<Station>? = null

    fun getStations(): List<Station> {
        cachedStations?.let { return it }
        
        val jsonString = context.assets.open("stations.json")
            .bufferedReader()
            .use { it.readText() }
            
        val stations = Json.decodeFromString<List<Station>>(jsonString)
        cachedStations = stations
        return stations
    }

    fun getFavouriteStations(): Flow<List<Station>> {
        return stationDao.getFavouriteStations().map { entities ->
            entities.map { it.toStation() }
        }
    }

    fun getRecentStations(): Flow<List<Station>> {
        return stationDao.getRecentStations().map { entities ->
            entities.map { it.toStation() }
        }
    }

    suspend fun isFavourite(stationId: String): Boolean {
        val entity = stationDao.getStationById(stationId)
        return entity?.isFavourite == true
    }

    suspend fun toggleFavourite(station: Station) {
        val entity = stationDao.getStationById(station.id)
        if (entity != null) {
            stationDao.updateFavouriteStatus(station.id, !entity.isFavourite)
        } else {
            stationDao.insertStation(station.toEntity(isFavourite = true))
        }
    }

    suspend fun recordPlayed(station: Station) {
        val entity = stationDao.getStationById(station.id)
        val currentTime = System.currentTimeMillis()
        if (entity != null) {
            stationDao.updateLastPlayedTime(station.id, currentTime)
        } else {
            stationDao.insertStation(station.toEntity(lastPlayedTime = currentTime))
        }
    }
}
