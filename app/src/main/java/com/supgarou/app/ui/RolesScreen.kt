package com.supgarou.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supgarou.app.model.CustomWake
import com.supgarou.app.model.Lang
import com.supgarou.app.model.RoleDef
import com.supgarou.app.model.Team
import com.supgarou.app.model.teamName
import com.supgarou.app.model.tr
import java.util.UUID

private fun newCustomRole() = RoleDef(
    id = "custom_" + UUID.randomUUID().toString().take(8),
    name = "",
    icon = "⭐",
    team = Team.VILLAGE,
    custom = true,
    customWake = CustomWake.EVERY_NIGHT,
    customTargets = 1,
)

@Composable
fun RolesScreen(vm: AppViewModel) {
    var editing by remember { mutableStateOf<RoleDef?>(null) }
    ScreenScaffold(
        tr("Roles", "Rôles"),
        onBack = { vm.back() },
        actions = { IconButton(onClick = { editing = newCustomRole() }) { Icon(Icons.Filled.Add, contentDescription = tr("Add a role", "Ajouter un rôle")) } },
    ) {
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    tr("Tap a role to edit its name, icon, team and description. Changes apply to new games.", "Touche un rôle pour modifier son nom, son icône, son camp et sa description. Les changements s'appliquent aux nouvelles parties."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(vm.roles, key = { it.id }) { role ->
                Card(
                    onClick = { editing = role },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        TeamStripe(teamColor(role.team), Modifier.height(64.dp))
                        Text(role.icon, fontSize = 30.sp, modifier = Modifier.padding(horizontal = 10.dp).width(44.dp))
                        Column(Modifier.weight(1f)) {
                            Text(role.name.ifBlank { "?" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                teamName(role.team) + " · " + role.wakes + if (role.custom) " · " + tr("custom", "personnalisé") else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = teamColor(role.team),
                            )
                            Text(role.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { editing = newCustomRole() }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ " + tr("Add a custom role", "Ajouter un rôle personnalisé"))
                }
            }
        }
    }
    editing?.let { r ->
        val exists = vm.roles.any { it.id == r.id }
        RoleEditDialog(
            r,
            onSave = { vm.saveRole(it); editing = null },
            onDelete = if (r.custom && exists) ({ vm.deleteRole(r.id); editing = null }) else null,
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun RoleEditDialog(role: RoleDef, onSave: (RoleDef) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(role.name) }
    var icon by remember { mutableStateOf(role.icon) }
    var team by remember { mutableStateOf(role.team) }
    var wakes by remember { mutableStateOf(role.wakes) }
    var desc by remember { mutableStateOf(role.description) }
    var wake by remember { mutableStateOf(role.customWake) }
    var targets by remember { mutableStateOf(role.customTargets) }
    var badge by remember { mutableStateOf(role.customBadge) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (role.name.isBlank()) tr("New role", "Nouveau rôle") else role.name) },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = icon, onValueChange = { icon = it.take(8) }, label = { Text(tr("Icon", "Icône")) }, singleLine = true, modifier = Modifier.width(90.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Name", "Nom")) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text(tr("Team", "Camp"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Team.entries.forEach { t -> FilterChip(selected = team == t, onClick = { team = t }, label = { Text(teamName(t)) }) }
                }
                OutlinedTextField(value = wakes, onValueChange = { wakes = it }, label = { Text(tr("Wakes", "Réveil")) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(tr("Ability", "Pouvoir")) }, minLines = 3, modifier = Modifier.fillMaxWidth())
                if (role.custom) {
                    Text(tr("When the app calls it at night", "Quand l'app l'appelle la nuit"), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            CustomWake.NEVER to tr("Never", "Jamais"),
                            CustomWake.NIGHT_1 to tr("Night 1", "Nuit 1"),
                            CustomWake.EVERY_NIGHT to tr("Every night", "Chaque nuit"),
                            CustomWake.FROM_NIGHT_2 to tr("From night 2", "Dès la nuit 2"),
                        ).forEach { (w, l) -> FilterChip(selected = wake == w, onClick = { wake = w }, label = { Text(l) }) }
                    }
                    StepperRow(tr("Players it targets", "Joueurs ciblés"), targets, 0..4) { targets = it }
                    OutlinedTextField(value = badge, onValueChange = { badge = it.take(8) }, label = { Text(tr("Badge on its targets", "Badge sur ses cibles")) }, singleLine = true)
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(tr("Delete this role", "Supprimer ce rôle"), color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val fr = Lang.french
                    onSave(
                        role.copy(
                            name = name.trim(),
                            icon = icon.ifBlank { "⭐" },
                            team = team,
                            wakesEn = if (fr) role.wakesEn else wakes,
                            wakesFr = if (fr) wakes else role.wakesFr,
                            descEn = if (fr) role.descEn else desc,
                            descFr = if (fr) desc else role.descFr,
                            customWake = wake,
                            customTargets = targets,
                            customBadge = badge.ifBlank { "⭐" },
                        ),
                    )
                },
            ) { Text(tr("Save", "Enregistrer")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "Annuler")) } },
    )
}
