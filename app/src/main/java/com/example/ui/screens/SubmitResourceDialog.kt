package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ResourceType
import com.example.data.model.Subject
import com.example.ui.components.resourceTypeIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitResourceBottomSheet(
    subjects: List<Subject>,
    defaultSemester: Int,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        subjectId: String,
        subjectName: String,
        unit: Int,
        type: ResourceType,
        documentUrl: String,
        source: String,
        semester: Int,
        description: String
    ) -> Boolean,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }

    var selectedSemester by remember { mutableIntStateOf(defaultSemester.coerceIn(1, 6)) }
    val availableSubjects = subjects.filter { it.semester == selectedSemester }.ifEmpty { subjects }
    var selectedSubject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: subjects.first()) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    var selectedUnit by remember { mutableIntStateOf(1) }
    var selectedType by remember { mutableStateOf(ResourceType.NOTES) }

    var documentUrl by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }

    var source by remember { mutableStateOf("Govt Polytechnic Student") }
    var description by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Submit a Study Resource",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Share verified notes, practical records or PYQs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("submit_dialog_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Resource Title *",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.trim().length >= 4) titleError = null
                },
                placeholder = { Text("e.g., Theodolite Temporary Adjustments & Formula Sheet") },
                isError = titleError != null,
                supportingText = {
                    titleError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_input_title")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Semester",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items((1..6).toList()) { sem ->
                    val isSelected = sem == selectedSemester
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSemester = sem
                            val subForSem = subjects.filter { it.semester == sem }
                            if (subForSem.isNotEmpty()) {
                                selectedSubject = subForSem.first()
                            }
                        },
                        label = { Text("Sem $sem") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Subject *",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = subjectDropdownExpanded,
                onExpandedChange = { subjectDropdownExpanded = !subjectDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = "${selectedSubject.code} - ${selectedSubject.name}",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("submit_subject_dropdown"),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = subjectDropdownExpanded,
                    onDismissRequest = { subjectDropdownExpanded = false }
                ) {
                    availableSubjects.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text("${sub.code} - ${sub.name}") },
                            onClick = {
                                selectedSubject = sub
                                subjectDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Syllabus Unit (Unit 1 to ${selectedSubject.unitCount})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items((1..selectedSubject.unitCount.coerceAtLeast(4)).toList()) { unitNum ->
                    val isSelected = unitNum == selectedUnit
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedUnit = unitNum },
                        label = { Text("Unit $unitNum") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Resource Type *",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ResourceType.values()) { type ->
                    val isSelected = type == selectedType
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        leadingIcon = {
                            Icon(
                                imageVector = resourceTypeIcon(type),
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(type.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Document URL (PDF / Google Drive / Web Link) *",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = documentUrl,
                onValueChange = {
                    documentUrl = it
                    if (it.startsWith("http://") || it.startsWith("https://")) urlError = null
                },
                placeholder = { Text("https://...") },
                isError = urlError != null,
                supportingText = {
                    urlError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_input_url")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Contributor / College Source",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                placeholder = { Text("e.g., Govt Polytechnic Kashipur Faculty / Student") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Brief Summary / Key Topics (Optional)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("e.g., Covers derivations, formulas and IS code provisions.") },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    var isValid = true
                    if (title.trim().length < 4) {
                        titleError = "Title must be at least 4 characters long"
                        isValid = false
                    }
                    if (documentUrl.isBlank() || (!documentUrl.startsWith("http://") && !documentUrl.startsWith("https://"))) {
                        urlError = "Please enter a valid URL starting with https://"
                        isValid = false
                    }

                    if (isValid) {
                        onSubmit(
                            title,
                            selectedSubject.id,
                            selectedSubject.name,
                            selectedUnit,
                            selectedType,
                            documentUrl,
                            source,
                            selectedSemester,
                            description
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_form_confirm_btn")
            ) {
                Text(
                    text = "Submit to PolyNotes Library",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
