package com.example.radioapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.radioapp.data.StationRepository
import com.example.radioapp.model.Station
import com.example.radioapp.player.RadioController
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(repository: StationRepository, radioController: RadioController) {
    val recentStations by repository.getRecentStations().collectAsState(initial = emptyList())
    // For home screen, show recents, or all stations if recents is empty
    val stations = if (recentStations.isNotEmpty()) recentStations else repository.getStations().take(10)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (recentStations.isNotEmpty()) "Recently Played" else "Suggested Stations",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        StationList(stations = stations, repository = repository, radioController = radioController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(repository: StationRepository, radioController: RadioController) {
    var searchQuery by remember { mutableStateOf("") }
    val allStations = repository.getStations()
    val filteredStations = if (searchQuery.isEmpty()) {
        allStations
    } else {
        allStations.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.city.contains(searchQuery, ignoreCase = true) 
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { },
            active = false,
            onActiveChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search stations...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
        ) {}

        StationList(stations = filteredStations, repository = repository, radioController = radioController)
    }
}

@Composable
fun FavouritesScreen(repository: StationRepository, radioController: RadioController) {
    val favouriteStations by repository.getFavouriteStations().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Your Favourites",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        if (favouriteStations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No favourites yet.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            StationList(stations = favouriteStations, repository = repository, radioController = radioController)
        }
    }
}

@Composable
fun SettingsScreen(repository: StationRepository) {
    val darkMode by repository.preferences.darkModeFlow.collectAsState(initial = false)
    val autoPlay by repository.preferences.autoPlayFlow.collectAsState(initial = true)
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dark Mode")
            Switch(
                checked = darkMode,
                onCheckedChange = { coroutineScope.launch { repository.preferences.setDarkMode(it) } }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Auto-Play on Select")
            Switch(
                checked = autoPlay,
                onCheckedChange = { coroutineScope.launch { repository.preferences.setAutoPlay(it) } }
            )
        }
    }
}

@Composable
fun StationList(stations: List<Station>, repository: StationRepository, radioController: RadioController) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(stations, key = { it.id }) { station ->
            StationItem(
                station = station,
                repository = repository,
                onClick = {
                    radioController.play(station)
                }
            )
        }
    }
}

@Composable
fun StationItem(station: Station, repository: StationRepository, onClick: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isFavourite by remember { mutableStateOf(false) }

    LaunchedEffect(station.id) {
        isFavourite = repository.isFavourite(station.id)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { 
                coroutineScope.launch { repository.recordPlayed(station) }
                onClick() 
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = station.name, style = MaterialTheme.typography.titleLarge)
                Text(text = "${station.city} • ${station.language}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = {
                coroutineScope.launch {
                    repository.toggleFavourite(station)
                    isFavourite = !isFavourite
                }
            }) {
                Icon(
                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favourite",
                    tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
