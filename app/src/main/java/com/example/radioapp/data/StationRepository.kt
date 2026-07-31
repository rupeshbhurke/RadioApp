package com.example.radioapp.data

import android.content.Context
import android.util.Log
import com.example.radioapp.data.local.AppDatabase
import com.example.radioapp.data.local.AppPreferences
import com.example.radioapp.data.local.toEntity
import com.example.radioapp.data.local.toStation
import com.example.radioapp.model.Station
import com.example.radioapp.network.RadioBrowserApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StationRepository(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val stationDao = database.stationDao()
    val preferences = AppPreferences(context)
    
    private val api = RadioBrowserApi.create()

    private var cachedStations: List<Station>? = null

    fun getStationsFlow(): Flow<List<Station>> {
        return stationDao.getAllStations().map { entities ->
            entities.map { it.toStation() }
        }
    }

    suspend fun loadInitialStationsIfNeeded() {
        val jsonString = context.assets.open("stations.json")
            .bufferedReader()
            .use { it.readText() }
        val staticStations = Json.decodeFromString<List<Station>>(jsonString)
        
        val existing = stationDao.getAllStationsStatic().map { it.streamUrl }.toSet()
        
        staticStations.forEach { staticStation ->
            if (!existing.contains(staticStation.streamUrl)) {
                stationDao.insertStation(staticStation.toEntity())
            }
        }
    }

    suspend fun syncStations(): Pair<Int, String?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiStations = api.searchStations(country = "India", limit = 100)
            val existing = stationDao.getAllStationsStatic().map { it.streamUrl }.toSet()
            
            var addedCount = 0
            apiStations.forEach { apiStation ->
                if (!existing.contains(apiStation.urlResolved)) {
                    val newStation = Station(
                        id = apiStation.id ?: "unknown_id",
                        name = apiStation.name ?: "Unknown",
                        streamUrl = apiStation.urlResolved ?: "",
                        city = apiStation.state ?: "India",
                        language = apiStation.language ?: "Unknown",
                        logoUrl = apiStation.favicon ?: ""
                    )
                    stationDao.insertStation(newStation.toEntity())
                    addedCount++
                }
            }
            Pair(addedCount, null)
        } catch (e: Throwable) {
            Log.e("StationRepository", "Failed to sync stations", e)
            Pair(-1, e.message ?: e.javaClass.simpleName)
        }
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
