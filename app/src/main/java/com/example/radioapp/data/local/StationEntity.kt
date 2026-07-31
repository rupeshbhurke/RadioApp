package com.example.radioapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.radioapp.model.Station

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val city: String,
    val language: String,
    val logoUrl: String,
    val isFavourite: Boolean = false,
    val lastPlayedTime: Long = 0L
)

fun StationEntity.toStation() = Station(
    id = id,
    name = name,
    streamUrl = streamUrl,
    city = city,
    language = language,
    logoUrl = logoUrl
)

fun Station.toEntity(isFavourite: Boolean = false, lastPlayedTime: Long = 0L) = StationEntity(
    id = id,
    name = name,
    streamUrl = streamUrl,
    city = city,
    language = language,
    logoUrl = logoUrl,
    isFavourite = isFavourite,
    lastPlayedTime = lastPlayedTime
)
