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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import coil.compose.AsyncImage
import com.example.radioapp.data.StationRepository
import com.example.radioapp.model.Station
import com.example.radioapp.player.RadioController
import kotlinx.coroutines.launch

@Composable
fun getScaledFontSize(baseSize: TextUnit, scale: Float): TextUnit {
    return baseSize * scale
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(repository: StationRepository, radioController: RadioController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val allStations by repository.getStationsFlow().collectAsState(initial = emptyList())
    
    val categories = remember(allStations) {
        allStations.flatMap { it.language.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    
    val filteredStations = remember(allStations, searchQuery, selectedCategory) {
        allStations.filter { station ->
            val matchesSearch = if (searchQuery.isEmpty()) true else {
                station.name.contains(searchQuery, ignoreCase = true) || 
                station.city.contains(searchQuery, ignoreCase = true)
            }
            val currentCategory = selectedCategory
            val matchesCategory = if (currentCategory == null) true else {
                station.language.split(",").map { it.trim() }.any { it.equals(currentCategory, ignoreCase = true) }
            }
            matchesSearch && matchesCategory
        }.sortedBy { it.name }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search stations...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
        ) {}
        
        if (categories.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { 
                            if (selectedCategory == category) selectedCategory = null else selectedCategory = category 
                        },
                        label = { Text(category) }
                    )
                }
            }
        }
        
        Text(
            text = "${filteredStations.size} Stations Available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
        )

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
fun SettingsScreen(repository: StationRepository, radioController: RadioController) {
    val themeMode by repository.preferences.themeModeFlow.collectAsState(initial = 0)
    val autoPlay by repository.preferences.autoPlayFlow.collectAsState(initial = true)
    val gridColumns by repository.preferences.gridColumnsFlow.collectAsState(initial = 3)
    val sleepTimerRemaining by radioController.sleepTimerRemaining.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    var isSyncing by remember { mutableStateOf(false) }
    var syncResult by remember { mutableStateOf<String?>(null) }
    
    var viewModeExpanded by remember { mutableStateOf(false) }
    var themeModeExpanded by remember { mutableStateOf(false) }
    var sleepTimerExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Sleep Timer")
            Box {
                OutlinedButton(onClick = { sleepTimerExpanded = true }) {
                    val text = if (sleepTimerRemaining != null) {
                        val totalSeconds = sleepTimerRemaining!! / 1000
                        val h = totalSeconds / 3600
                        val m = (totalSeconds % 3600) / 60
                        val s = totalSeconds % 60
                        if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
                    } else "Off"
                    Text(text)
                }
                
                if (sleepTimerExpanded) {
                    var hours by remember { mutableStateOf(0) }
                    var minutes by remember { mutableStateOf(15) }
                    
                    AlertDialog(
                        onDismissRequest = { sleepTimerExpanded = false },
                        title = { Text("Set Sleep Timer") },
                        text = {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Hours")
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.material3.IconButton(onClick = { if (hours > 0) hours-- }) {
                                                Text("-", style = MaterialTheme.typography.titleLarge)
                                            }
                                            Text(String.format("%02d", hours), style = MaterialTheme.typography.titleLarge)
                                            androidx.compose.material3.IconButton(onClick = { if (hours < 24) hours++ }) {
                                                Text("+", style = MaterialTheme.typography.titleLarge)
                                            }
                                        }
                                    }
                                    Text(":", style = MaterialTheme.typography.titleLarge)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Minutes")
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.material3.IconButton(onClick = { if (minutes > 0) minutes-- else if (hours > 0) { hours--; minutes = 59 } }) {
                                                Text("-", style = MaterialTheme.typography.titleLarge)
                                            }
                                            Text(String.format("%02d", minutes), style = MaterialTheme.typography.titleLarge)
                                            androidx.compose.material3.IconButton(onClick = { if (minutes < 59) minutes++ else if (hours < 24) { hours++; minutes = 0 } }) {
                                                Text("+", style = MaterialTheme.typography.titleLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                val totalMinutes = (hours * 60) + minutes
                                // Max 24 hours (1440 mins), min 1 minute unless 0 (Off)
                                val finalMinutes = totalMinutes.coerceIn(0, 1440)
                                radioController.setSleepTimer(finalMinutes)
                                sleepTimerExpanded = false
                            }) {
                                Text("Set Timer")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.OutlinedButton(onClick = {
                                radioController.setSleepTimer(0)
                                sleepTimerExpanded = false
                            }) {
                                Text("Turn Off")
                            }
                        }
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Theme Mode")
            Box {
                OutlinedButton(onClick = { themeModeExpanded = true }) {
                    val text = when(themeMode) { 1 -> "Light"; 2 -> "Dark"; else -> "System" }
                    Text(text)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = themeModeExpanded, onDismissRequest = { themeModeExpanded = false }) {
                    DropdownMenuItem(text = { Text("System Default") }, onClick = { coroutineScope.launch { repository.preferences.setThemeMode(0) }; themeModeExpanded = false })
                    DropdownMenuItem(text = { Text("Light") }, onClick = { coroutineScope.launch { repository.preferences.setThemeMode(1) }; themeModeExpanded = false })
                    DropdownMenuItem(text = { Text("Dark") }, onClick = { coroutineScope.launch { repository.preferences.setThemeMode(2) }; themeModeExpanded = false })
                }
            }
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
            Text("Grid Columns")
            Box {
                OutlinedButton(onClick = { viewModeExpanded = true }) {
                    Text("$gridColumns")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = viewModeExpanded, onDismissRequest = { viewModeExpanded = false }) {
                    (1..5).forEach { cols ->
                        DropdownMenuItem(
                            text = { Text("$cols") }, 
                            onClick = { 
                                coroutineScope.launch { repository.preferences.setGridColumns(cols) }
                                viewModeExpanded = false 
                            }
                        )
                    }
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
    val gridColumns by repository.preferences.gridColumnsFlow.collectAsState(initial = 3)
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(stations, key = { it.id }) { station ->
            StationTile(
                station = station,
                repository = repository,
                gridColumns = gridColumns,
                onClick = { radioController.play(station) }
            )
        }
    }
}

@Composable
fun StationTile(station: Station, repository: StationRepository, gridColumns: Int, onClick: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isFavourite by remember { mutableStateOf(false) }
    val fontScale = (1.5f - (gridColumns * 0.25f)).coerceAtLeast(0.6f)

    LaunchedEffect(station.id) {
        isFavourite = repository.isFavourite(station.id)
    }

    ElevatedCard(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .let { 
                if (gridColumns == 1) it.height(100.dp) else it.aspectRatio(1f) 
            }
            .clickable { 
                coroutineScope.launch { repository.recordPlayed(station) }
                onClick() 
            },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (gridColumns == 1) {
            // 1-Column Banner Layout
            Row(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = station.logoUrl.ifEmpty { null },
                    contentDescription = "Station Logo",
                    modifier = Modifier.size(80.dp).padding(end = 16.dp),
                    error = rememberVectorPainter(Icons.Default.PlayArrow),
                    fallback = rememberVectorPainter(Icons.Default.PlayArrow)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name, 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = getScaledFontSize(MaterialTheme.typography.titleLarge.fontSize, fontScale)
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "${station.city} • ${station.language}", 
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = getScaledFontSize(MaterialTheme.typography.bodyMedium.fontSize, fontScale)
                        ),
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.toggleFavourite(station)
                            isFavourite = !isFavourite
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favourite",
                        tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Multi-Column Square Tile Layout
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AsyncImage(
                        model = station.logoUrl.ifEmpty { null },
                        contentDescription = "Station Logo",
                        modifier = Modifier.fillMaxSize().align(Alignment.Center),
                        error = rememberVectorPainter(Icons.Default.PlayArrow),
                        fallback = rememberVectorPainter(Icons.Default.PlayArrow)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(if (gridColumns >= 4) 16.dp else 28.dp)
                            .clickable {
                                coroutineScope.launch {
                                    repository.toggleFavourite(station)
                                    isFavourite = !isFavourite
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle Favourite",
                            tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = station.name, 
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = getScaledFontSize(MaterialTheme.typography.bodyMedium.fontSize, fontScale)
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "${station.city} • ${station.language}", 
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = getScaledFontSize(MaterialTheme.typography.labelSmall.fontSize, fontScale)
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}


