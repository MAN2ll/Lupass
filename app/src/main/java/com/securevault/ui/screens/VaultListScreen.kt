package com.securevault.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.securevault.data.repository.Entry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onLock: () -> Unit,
    favOnly: Boolean = false,
    vm: VaultViewModel = hiltViewModel()
) {
    val entries by (if (favOnly) vm.favorites else vm.entries).collectAsState()
    val query by vm.query.collectAsState()
    val categories by vm.categories.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val selectedCat by vm.category.collectAsState()
    val selectedProfile by vm.profile.collectAsState()
    val count by vm.count.collectAsState()
    val expiredCount by vm.expiredCount.collectAsState()
    val showExpired by vm.showExpiredOnly.collectAsState()
    var toDelete by remember { mutableStateOf<Entry?>(null) }

    toDelete?.let { e ->
        AlertDialog(onDismissRequest = { toDelete = null },
            title = { Text("Удалить?") },
            text = { Text("«${e.title}» будет удалена безвозвратно.") },
            confirmButton = { TextButton({ vm.delete(e.id); toDelete = null }) {
                Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ toDelete = null }) { Text("Отмена") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (favOnly) "Избранное" else "Пароли") },
                actions = {
                    // Значок просроченных паролей
                    if (!favOnly && expiredCount > 0) {
                        BadgedBox(badge = {
                            Badge { Text("$expiredCount") }
                        }) {
                            IconButton({ vm.setShowExpiredOnly(!showExpired) }) {
                                Icon(Icons.Default.Warning, "Просроченные пароли",
                                    tint = if (showExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    Text("$count", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp).align(Alignment.CenterVertically))
                    IconButton({ vm.lock(); onLock() }) { Icon(Icons.Default.Lock, "Заблокировать") }
                }
            )
        },
        floatingActionButton = {
            if (!favOnly) FloatingActionButton(onAdd) { Icon(Icons.Default.Add, null) }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (!favOnly) {
                // Поиск
                OutlinedTextField(
                    value = query, onValueChange = vm::setQuery,
                    placeholder = { Text("Поиск...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton({ vm.setQuery("") }) { Icon(Icons.Default.Clear, null) } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true
                )

                // Фильтр профилей (Личное/Рабочее)
                if (profiles.size > 1 || selectedProfile != null) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selectedProfile == null, { vm.setProfile(null) }, label = { Text("Все") },
                            leadingIcon = { Icon(Icons.Default.People, null, Modifier.size(16.dp)) })
                        val profileIcons = mapOf("Личное" to Icons.Default.Person, "Рабочее" to Icons.Default.Work)
                        profiles.forEach { p ->
                            FilterChip(selectedProfile == p, { vm.setProfile(if (selectedProfile == p) null else p) },
                                label = { Text(p) },
                                leadingIcon = { Icon(profileIcons[p] ?: Icons.Default.Label, null, Modifier.size(16.dp)) })
                        }
                    }
                }

                // Фильтр категорий
                if (categories.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selectedCat == null, { vm.setCategory(null) }, label = { Text("Категории: все") })
                        categories.forEach { c ->
                            FilterChip(selectedCat == c, { vm.setCategory(if (selectedCat == c) null else c) }, label = { Text(c) })
                        }
                    }
                }

                // Баннер просроченных паролей
                if (showExpired && expiredCount > 0) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Показаны пароли, требующие замены: $expiredCount",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                            TextButton({ vm.setShowExpiredOnly(false) }) { Text("Все") }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (favOnly) Icons.Default.StarBorder else Icons.Default.VpnKey,
                            null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(when {
                            favOnly -> "Нет избранных записей"
                            query.isNotEmpty() -> "Ничего не найдено"
                            showExpired -> "Просроченных паролей нет"
                            else -> "Нет паролей. Нажмите + чтобы добавить"
                        }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries, key = { it.id }) { e ->
                        EntryCard(e, { onEdit(e.id) }, { vm.toggleFavorite(e) }, { toDelete = e })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryCard(e: Entry, onClick: () -> Unit, onFav: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val isExpired = e.isPasswordExpired
    val daysLeft = e.daysUntilExpiry

    Card(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { menu = true }),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isExpired) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) else null
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Аватар с буквой
            Surface(shape = MaterialTheme.shapes.medium,
                color = if (isExpired) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(e.title.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(e.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    // Значок профиля
                    if (e.profile == "Рабочее")
                        Icon(Icons.Default.Work, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                }
                if (e.username.isNotEmpty())
                    Text(e.username, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(e.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    if (isExpired) {
                        Surface(color = MaterialTheme.colorScheme.error, shape = MaterialTheme.shapes.extraSmall) {
                            Text("Смените пароль!", fontSize = 10.sp, color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    } else if (daysLeft in 1..14) {
                        Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), shape = MaterialTheme.shapes.extraSmall) {
                            Text("${daysLeft}д.", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                // Эмодзи подсказка
                val emojis = HintVisualizer.toEmojis(e.hintKeywords)
                if (emojis.isNotEmpty())
                    Text(emojis, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onFav, Modifier.size(32.dp)) {
                    Icon(if (e.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null,
                        tint = if (e.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Удалить") }, onClick = { menu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
}
