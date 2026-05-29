package com.securevault.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryEditScreen(entryId: Long?, onBack: () -> Unit, vm: EntryViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val existing by vm.entry.collectAsState()
    val saved by vm.saved.collectAsState()

    var title by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf("Личное") }
    var cat by remember { mutableStateOf("Общее") }
    var user by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var hintText by remember { mutableStateOf("") }
    var hintKeywords by remember { mutableStateOf("") }
    var changeInterval by remember { mutableIntStateOf(0) }
    var fav by remember { mutableStateOf(false) }
    var showPwd by remember { mutableStateOf(false) }
    var showGen by remember { mutableStateOf(false) }
    // Generator settings per-entry
    var genLen by remember { mutableFloatStateOf(20f) }
    var genUpper by remember { mutableStateOf(true) }
    var genDigits by remember { mutableStateOf(true) }
    var genSymbols by remember { mutableStateOf(true) }

    val profiles = listOf("Личное", "Рабочее", "Другое")
    val cats = listOf("Общее","Соцсети","Банки","Работа","Почта","Другое")
    val intervals = listOf(0 to "Без напоминания", 30 to "30 дней", 60 to "60 дней",
        90 to "90 дней", 180 to "180 дней", 365 to "1 год")

    LaunchedEffect(entryId) { entryId?.let { vm.load(it) } }
    LaunchedEffect(existing) {
        existing?.let { e ->
            title = e.title; profile = e.profile; cat = e.category; user = e.username
            pwd = e.password; url = e.url; notes = e.notes; hintText = e.hintText
            hintKeywords = e.hintKeywords; changeInterval = e.changeIntervalDays; fav = e.isFavorite
            genLen = e.genLength.toFloat(); genUpper = e.genUpper; genDigits = e.genDigits; genSymbols = e.genSymbols
        }
    }
    LaunchedEffect(saved) { if (saved) onBack() }

    fun copy(text: String) = (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText("sv", text))

    // Генератор паролей
    if (showGen) {
        var previewPwd by remember { mutableStateOf(vm.generatePassword(genLen.toInt(), genUpper, genDigits, genSymbols)) }
        AlertDialog(onDismissRequest = { showGen = false },
            title = { Text("Генератор паролей") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(previewPwd, Modifier.weight(1f), fontSize = 13.sp)
                            IconButton({ previewPwd = vm.generatePassword(genLen.toInt(), genUpper, genDigits, genSymbols) }) {
                                Icon(Icons.Default.Refresh, null)
                            }
                        }
                    }
                    Text("Длина: ${genLen.toInt()}", fontSize = 13.sp)
                    Slider(genLen, { v -> genLen = v; previewPwd = vm.generatePassword(v.toInt(), genUpper, genDigits, genSymbols) }, valueRange = 8f..40f, steps = 31)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(genUpper, { genUpper = it; previewPwd = vm.generatePassword(genLen.toInt(), it, genDigits, genSymbols) })
                        Text("Заглавные (A-Z)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(genDigits, { genDigits = it; previewPwd = vm.generatePassword(genLen.toInt(), genUpper, it, genSymbols) })
                        Text("Цифры (0-9)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(genSymbols, { genSymbols = it; previewPwd = vm.generatePassword(genLen.toInt(), genUpper, genDigits, it) })
                        Text("Символы (!@#...)")
                    }
                    Text("Настройки сохраняются для этой записи.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { Button({ pwd = previewPwd; showGen = false }) { Text("Использовать") } },
            dismissButton = { TextButton({ showGen = false }) { Text("Отмена") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == null) "Новая запись" else "Редактировать") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton({ fav = !fav }) {
                        Icon(if (fav) Icons.Default.Star else Icons.Default.StarBorder, null,
                            tint = if (fav) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton({ vm.save(entryId ?: 0, title, profile, cat, user, pwd, url, notes,
                        hintText, hintKeywords, changeInterval, fav,
                        genLen.toInt(), genUpper, genDigits, genSymbols) }) {
                        Icon(Icons.Default.Check, "Сохранить")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // === Основные поля ===
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название *") },
                modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Label, null) }, singleLine = true)

            // Профиль
            var profExp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(profExp, { profExp = it }) {
                OutlinedTextField(value = profile, onValueChange = {}, readOnly = true, label = { Text("Профиль") },
                    leadingIcon = { Icon(if (profile == "Рабочее") Icons.Default.Work else Icons.Default.Person, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(profExp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(profExp, { profExp = false }) {
                    profiles.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { profile = p; profExp = false }) }
                }
            }

            // Категория
            var catExp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(catExp, { catExp = it }) {
                OutlinedTextField(value = cat, onValueChange = {}, readOnly = true, label = { Text("Категория") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(catExp, { catExp = false }) {
                    cats.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { cat = c; catExp = false }) }
                }
            }

            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Логин / Email") },
                modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, null) },
                trailingIcon = { if (user.isNotEmpty()) IconButton({ copy(user) }) { Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp)) } },
                singleLine = true)

            OutlinedTextField(value = pwd, onValueChange = { pwd = it }, label = { Text("Пароль *") },
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    Row {
                        if (pwd.isNotEmpty()) IconButton({ copy(pwd) }) { Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp)) }
                        IconButton({ showPwd = !showPwd }) { Icon(if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
                        IconButton({ showGen = true }) { Icon(Icons.Default.Casino, null) }
                    }
                }, singleLine = true)

            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL (необязательно)") },
                modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Language, null) }, singleLine = true)

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Заметки") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)

            // === Планировщик смены пароля ===
            HorizontalDivider()
            Text("Напоминание о смене пароля", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            var intExp by remember { mutableStateOf(false) }
            val intervalLabel = intervals.find { it.first == changeInterval }?.second ?: "Без напоминания"
            ExposedDropdownMenuBox(intExp, { intExp = it }) {
                OutlinedTextField(value = intervalLabel, onValueChange = {}, readOnly = true,
                    label = { Text("Менять каждые") },
                    leadingIcon = { Icon(Icons.Default.Schedule, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(intExp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(intExp, { intExp = false }) {
                    intervals.forEach { (val_, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { changeInterval = val_; intExp = false })
                    }
                }
            }

            // === Подсказка / Мнемоника ===
            HorizontalDivider()
            Text("Мнемонические подсказки", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("Помогут вспомнить пароль, не раскрывая его.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(value = hintText, onValueChange = { hintText = it },
                label = { Text("Текстовая подсказка") },
                placeholder = { Text("Например: «год рождения бабушки + кличка кота»") },
                leadingIcon = { Icon(Icons.Default.Lightbulb, null) },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3)

            OutlinedTextField(value = hintKeywords, onValueChange = { hintKeywords = it },
                label = { Text("Ключевые слова → эмодзи") },
                placeholder = { Text("кот, дом, 5, синий, ...") },
                leadingIcon = { Icon(Icons.Default.EmojiObjects, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            // Предпросмотр эмодзи
            val emojiPreview = HintVisualizer.toEmojis(hintKeywords)
            if (emojiPreview.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Визуальная подсказка:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(emojiPreview, fontSize = 36.sp)
                    }
                }
            }

            // Быстрые теги-подсказки
            Text("Быстрые теги:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HintVisualizer.suggestKeywords().forEach { kw ->
                    val emoji = HintVisualizer.toEmojis(kw)
                    SuggestionChip(onClick = {
                        hintKeywords = if (hintKeywords.isEmpty()) kw
                            else "${hintKeywords.trimEnd(',').trim()}, $kw"
                    }, label = { Text("$emoji $kw", fontSize = 12.sp) })
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
