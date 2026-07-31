package com.example.radioapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import com.example.radioapp.data.StationRepository
import com.example.radioapp.model.Station
import com.example.radioapp.player.RadioController
import kotlinx.coroutines.launch

@Composable
fun getScaledFontSize(baseSize: TextUnit, scale: Float): TextUnit {
    return baseSize * scale
}

@Composable
fun getFontScale(sizePref: String): Float {
    return when (sizePref) {
        "Small" -> 0.85f
        "Large" -> 1.15f
        else -> 1.0f
    }
}

@Composable
fun HomeScreen(repository: StationRepository, radioController: RadioController) {
    val recentStations by repository.getRecentStations().collectAsState(initial = emptyList())
    val allStations by repository.getStationsFlow().collectAsState(initial = emptyList())
    
    // Show a mix of recent and all stations, or just all if there are no recents yet
    val stations = if (recentStations.isNotEmpty()) {
        (recentStations + allStations).distinctBy { it.id }.take(15)
    } else {
        allStations.take(15)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (recentStations.isNotEmpty()) "Recently Played & Suggested" else "Suggested Stations",
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
    val allStations by repository.getStationsFlow().collectAsState(initial = emptyList())
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
    val viewMode by repository.preferences.viewModeFlow.collectAsState(initial = "List")
    val fontSize by repository.preferences.fontSizeFlow.collectAsState(initial = "Default")
    val coroutineScope = rememberCoroutineScope()
    
    var isSyncing by remember { mutableStateOf(false) }
    var syncResult by remember { mutableStateOf<String?>(null) }
    
    var viewModeExpanded by remember { mutableStateOf(false) }
    var fontSizeExpanded by remember { mutableStateOf(false) }

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
        
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text(text = "Appearance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Layout Mode")
            Box {
                OutlinedButton(onClick = { viewModeExpanded = true }) {
                    Text(viewMode)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = viewModeExpanded, onDismissRequest = { viewModeExpanded = false }) {
                    DropdownMenuItem(text = { Text("List") }, onClick = { coroutineScope.launch { repository.preferences.setViewMode("List") }; viewModeExpanded = false })
                    DropdownMenuItem(text = { Text("Tile") }, onClick = { coroutineScope.launch { repository.preferences.setViewMode("Tile") }; viewModeExpanded = false })
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Font Size")
            Box {
                OutlinedButton(onClick = { fontSizeExpanded = true }) {
                    Text(fontSize)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = fontSizeExpanded, onDismissRequest = { fontSizeExpanded = false }) {
                    DropdownMenuItem(text = { Text("Small") }, onClick = { coroutineScope.launch { repository.preferences.setFontSize("Small") }; fontSizeExpanded = false })
                    DropdownMenuItem(text = { Text("Default") }, onClick = { coroutineScope.launch { repository.preferences.setFontSize("Default") }; fontSizeExpanded = false })
                    DropdownMenuItem(text = { Text("Large") }, onClick = { coroutineScope.launch { repository.preferences.setFontSize("Large") }; fontSizeExpanded = false })
                }
            }
        }
        
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        
        Text(text = "Data", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        
        Button(
            onClick = {
                if (!isSyncing) {
                    isSyncing = true
                    syncResult = null
                    coroutineScope.launch {
                        val (added, error) = repository.syncStations()
                        isSyncing = false
                        if (added >= 0) {
                            syncResult = "Sync complete! Added $added new stations."
                        } else {
                            syncResult = "Sync failed: $error"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Syncing with Radio Browser...")
            } else {
                Text("Sync & Re-index Stations")
            }
        }
        
        if (syncResult != null) {
            Text(
                text = syncResult!!,
                style = MaterialTheme.typography.bodyMedium,
                color = if (syncResult!!.contains("failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun StationList(stations: List<Station>, repository: StationRepository, radioController: RadioController) {
    val viewMode by repository.preferences.viewModeFlow.collectAsState(initial = "List")
    
    if (viewMode == "Tile") {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(stations, key = { it.id }) { station ->
                StationTile(
                    station = station,
                    repository = repository,
                    onClick = { radioController.play(station) }
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(stations, key = { it.id }) { station ->
                StationItem(
                    station = station,
                    repository = repository,
                    onClick = { radioController.play(station) }
                )
            }
        }
    }
}

@Composable
fun StationTile(station: Station, repository: StationRepository, onClick: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isFavourite by remember { mutableStateOf(false) }
    val fontPref by repository.preferences.fontSizeFlow.collectAsState(initial = "Default")
    val fontScale = getFontScale(fontPref)

    LaunchedEffect(station.id) {
        isFavourite = repository.isFavourite(station.id)
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { 
                coroutineScope.launch { repository.recordPlayed(station) }
                onClick() 
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        repository.toggleFavourite(station)
                        isFavourite = !isFavourite
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favourite",
                    tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = station.name, 
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = getScaledFontSize(MaterialTheme.typography.titleMedium.fontSize, fontScale)
                    ),
                    maxLines = 2
                )
                Text(
                    text = "${station.city} • ${station.language}", 
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = getScaledFontSize(MaterialTheme.typography.bodySmall.fontSize, fontScale)
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun StationItem(station: Station, repository: StationRepository, onClick: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isFavourite by remember { mutableStateOf(false) }
    val fontPref by repository.preferences.fontSizeFlow.collectAsState(initial = "Default")
    val fontScale = getFontScale(fontPref)

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
                Text(
                    text = station.name, 
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = getScaledFontSize(MaterialTheme.typography.titleLarge.fontSize, fontScale)
                    )
                )
                Text(
                    text = "${station.city} • ${station.language}", 
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = getScaledFontSize(MaterialTheme.typography.bodyMedium.fontSize, fontScale)
                    )
                )
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
