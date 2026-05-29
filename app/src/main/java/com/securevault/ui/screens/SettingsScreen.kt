package com.securevault.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onResetDone: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var bio by remember { mutableStateOf(vm.isBiometricEnabled) }

    // Change password
    var showChangePwd by remember { mutableStateOf(false) }
    var curPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var cfmPwd by remember { mutableStateOf("") }
    var showPwds by remember { mutableStateOf(false) }

    // Export/import
    var exportUri by remember { mutableStateOf<Uri?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showExportDlg by remember { mutableStateOf(false) }
    var showImportDlg by remember { mutableStateOf(false) }
    var exportPwd by remember { mutableStateOf("") }
    var importPwd by remember { mutableStateOf("") }
    var showExpPwd by remember { mutableStateOf(false) }
    var showImpPwd by remember { mutableStateOf(false) }
    var showExportFormatDlg by remember { mutableStateOf(false) }
    var exportCsvUri by remember { mutableStateOf<Uri?>(null) }
    var exportTxtUri by remember { mutableStateOf<Uri?>(null) }

    // Mass rotate
    var showMassRotate by remember { mutableStateOf(false) }
    var rotateProfile by remember { mutableStateOf<String?>(null) }

    // Reset
    var showReset by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        when (val s = state) {
            is SettingsState.Success -> {
                if (s.msg == "RESET") { onResetDone(); return@LaunchedEffect }
                snackbar.showSnackbar(s.msg); vm.clearState()
                showChangePwd = false; showExportDlg = false; showImportDlg = false
                curPwd = ""; newPwd = ""; cfmPwd = ""; exportPwd = ""; importPwd = ""
            }
            is SettingsState.Error -> { snackbar.showSnackbar(s.msg); vm.clearState() }
            else -> {}
        }
    }

    // Launchers
    val encExportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { exportUri = it; showExportDlg = true }
    }
    val csvExportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { exportCsvUri = it; vm.exportCsv(it) }
    }
    val txtExportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { exportTxtUri = it; vm.exportTxt(it) }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importUri = it; showImportDlg = true }
    }

    // === Change Password Dialog ===
    if (showChangePwd) {
        AlertDialog(onDismissRequest = { showChangePwd = false },
            title = { Text("Изменить мастер-пароль") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = curPwd, onValueChange = { curPwd = it }, label = { Text("Текущий пароль") },
                        visualTransformation = if (showPwds) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = { IconButton({ showPwds = !showPwds }) {
                            Icon(if (showPwds) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = newPwd, onValueChange = { newPwd = it }, label = { Text("Новый пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = cfmPwd, onValueChange = { cfmPwd = it }, label = { Text("Подтвердите") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button({ vm.changePassword(curPwd, newPwd, cfmPwd) }, enabled = state !is SettingsState.Loading) {
                    if (state is SettingsState.Loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Изменить")
                }
            },
            dismissButton = { TextButton({ showChangePwd = false }) { Text("Отмена") } }
        )
    }

    // === Encrypted Export Dialog ===
    if (showExportDlg && exportUri != null) {
        AlertDialog(onDismissRequest = { showExportDlg = false; exportPwd = "" },
            title = { Text("Пароль для резервной копии") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Запомните этот пароль — без него файл не открыть!", fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                    OutlinedTextField(value = exportPwd, onValueChange = { exportPwd = it }, label = { Text("Пароль экспорта") },
                        visualTransformation = if (showExpPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = { IconButton({ showExpPwd = !showExpPwd }) {
                            Icon(if (showExpPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button({ exportUri?.let { vm.exportEncrypted(it, exportPwd) } }, enabled = state !is SettingsState.Loading) {
                    if (state is SettingsState.Loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Экспортировать")
                }
            },
            dismissButton = { TextButton({ showExportDlg = false; exportPwd = "" }) { Text("Отмена") } }
        )
    }

    // === Import Dialog ===
    if (showImportDlg && importUri != null) {
        AlertDialog(onDismissRequest = { showImportDlg = false; importPwd = "" },
            title = { Text("Пароль файла резервной копии") },
            text = {
                OutlinedTextField(value = importPwd, onValueChange = { importPwd = it }, label = { Text("Пароль файла") },
                    visualTransformation = if (showImpPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = { IconButton({ showImpPwd = !showImpPwd }) {
                        Icon(if (showImpPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
            },
            confirmButton = {
                Button({ importUri?.let { vm.import(it, importPwd) } }, enabled = state !is SettingsState.Loading) {
                    if (state is SettingsState.Loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Импортировать")
                }
            },
            dismissButton = { TextButton({ showImportDlg = false; importPwd = "" }) { Text("Отмена") } }
        )
    }

    // === Mass Rotate Dialog ===
    if (showMassRotate) {
        AlertDialog(onDismissRequest = { showMassRotate = false },
            icon = { Icon(Icons.Default.Autorenew, null, tint = MaterialTheme.colorScheme.tertiary) },
            title = { Text("Массовая смена паролей") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Генерирует новые пароли для выбранной группы. Старые пароли будут удалены!", color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                    Text("Профиль для смены:", fontSize = 13.sp)
                    val opts = listOf(null to "Все профили", "Личное" to "Личное", "Рабочее" to "Рабочее")
                    opts.forEach { (val_, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(rotateProfile == val_, { rotateProfile = val_ })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                Button({
                    vm.massRotatePasswords(rotateProfile)
                    showMassRotate = false
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Text("Обновить пароли")
                }
            },
            dismissButton = { TextButton({ showMassRotate = false }) { Text("Отмена") } }
        )
    }

    // === Reset Dialog ===
    if (showReset) {
        AlertDialog(onDismissRequest = { showReset = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Сбросить хранилище?") },
            text = { Text("Все пароли будут удалены без возможности восстановления!", color = MaterialTheme.colorScheme.error) },
            confirmButton = {
                Button({ showReset = false; vm.reset() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Удалить всё")
                }
            },
            dismissButton = { TextButton({ showReset = false }) { Text("Отмена") } }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Настройки", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            SectionLabel("Безопасность")
            SettingRow(Icons.Default.Fingerprint, "Биометрический вход", "Отпечаток пальца",
                trailing = { Switch(bio, { bio = it; vm.setBiometric(it) }) })
            SettingRow(Icons.Default.Lock, "Изменить мастер-пароль", "Смена пароля для входа",
                onClick = { showChangePwd = true })

            SectionLabel("Резервная копия — зашифрованная")
            SettingRow(Icons.Default.EnhancedEncryption, "Экспорт (.svault)", "AES-256-GCM + PBKDF2, только Secure Vault",
                onClick = { encExportPicker.launch("backup_${System.currentTimeMillis()}.svault") })
            SettingRow(Icons.Default.Download, "Импорт (.svault)", "Восстановить из зашифрованного файла",
                onClick = { importPicker.launch(arrayOf("*/*")) })

            SectionLabel("Резервная копия — открытая")
            SettingRow(Icons.Default.TableChart, "Экспорт CSV", "Для Excel/Google Sheets (незашифрованный!)",
                titleColor = MaterialTheme.colorScheme.tertiary,
                onClick = { csvExportPicker.launch("passwords_${System.currentTimeMillis()}.csv") })
            SettingRow(Icons.Default.TextSnippet, "Экспорт TXT", "Читаемый текстовый файл (незашифрованный!)",
                titleColor = MaterialTheme.colorScheme.tertiary,
                onClick = { txtExportPicker.launch("passwords_${System.currentTimeMillis()}.txt") })

            SectionLabel("Генерация паролей")
            SettingRow(Icons.Default.Autorenew, "Массовая смена паролей",
                "Регенерировать пароли для профиля или всех записей",
                titleColor = MaterialTheme.colorScheme.tertiary,
                onClick = { showMassRotate = true })

            SectionLabel("Данные")
            SettingRow(Icons.Default.DeleteForever, "Сбросить хранилище", "Удалить все пароли",
                titleColor = MaterialTheme.colorScheme.error, onClick = { showReset = true })

            SectionLabel("О приложении")
            SettingRow(Icons.Default.Shield, "Безопасность",
                "AES-256-GCM · Android Keystore · PBKDF2-SHA256 · Защита от брутфорса")
            SettingRow(Icons.Default.Info, "Версия", "Secure Vault 1.0")
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

@Composable
private fun SettingRow(
    icon: ImageVector, title: String, subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(onClick = { onClick?.invoke() }, enabled = onClick != null,
        color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = titleColor, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) trailing()
            else if (onClick != null) Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}
