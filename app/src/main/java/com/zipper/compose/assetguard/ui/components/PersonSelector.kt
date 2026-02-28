package com.zipper.compose.assetguard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.data.local.entity.PersonEntity
import com.zipper.compose.assetguard.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonSelector(
    selectedPerson: PersonEntity?,
    newPersonName: String,
    query: String,
    suggestions: List<PersonEntity>,
    isLocked: Boolean,
    error: Int?,
    onQueryChanged: (String) -> Unit,
    onPersonSelected: (PersonEntity) -> Unit,
    onNewPersonSelected: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.loan_form_label_person),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(MaterialTheme.spacing.xs))

        when {
            // Locked mode (from PersonDetail)
            isLocked && selectedPerson != null -> {
                LockedPersonChip(person = selectedPerson)
            }
            // Selected existing person
            selectedPerson != null -> {
                SelectedPersonChip(
                    name = selectedPerson.name,
                    subtitle = selectedPerson.phone,
                    onClear = onClear
                )
            }
            // Selected new person name
            newPersonName.isNotBlank() -> {
                SelectedPersonChip(
                    name = newPersonName,
                    subtitle = stringResource(R.string.loan_form_new_person_tag),
                    onClear = onClear
                )
            }
            // Search mode
            else -> {
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded && (suggestions.isNotEmpty() || query.isNotBlank()),
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { text ->
                            onQueryChanged(text)
                            expanded = true
                        },
                        placeholder = { Text(stringResource(R.string.loan_form_person_hint)) },
                        isError = error != null,
                        supportingText = error?.let { resId -> { Text(stringResource(resId)) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )

                    ExposedDropdownMenu(
                        expanded = expanded && (suggestions.isNotEmpty() || query.isNotBlank()),
                        onDismissRequest = { expanded = false }
                    ) {
                        suggestions.forEach { person ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarView(name = person.name, size = 32.dp)
                                        Spacer(Modifier.width(MaterialTheme.spacing.sm))
                                        Column {
                                            Text(
                                                text = person.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            person.phone?.let {
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    onPersonSelected(person)
                                    expanded = false
                                }
                            )
                        }
                        if (query.isNotBlank()) {
                            if (suggestions.isNotEmpty()) {
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(MaterialTheme.spacing.sm))
                                        Text(
                                            text = stringResource(R.string.loan_form_create_person, query),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                onClick = {
                                    onNewPersonSelected(query)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedPersonChip(person: PersonEntity) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.spacing.md)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(name = person.name, size = 36.dp)
            Spacer(Modifier.width(MaterialTheme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                person.phone?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectedPersonChip(
    name: String,
    subtitle: String?,
    onClear: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(start = MaterialTheme.spacing.md, top = MaterialTheme.spacing.sm, bottom = MaterialTheme.spacing.sm)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(name = name, size = 36.dp)
            Spacer(Modifier.width(MaterialTheme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
