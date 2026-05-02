package com.playground.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class EditableField(
    val label: String,
    val initialValue: String,
    val placeholder: String? = null
)

@Composable
fun EditableFieldGroup(
    fields: List<EditableField>,
    onSave: (List<String>) -> Unit,
    saveLabel: String = "Save"
) {
    // Single remember call — number of remember calls must be stable across recompositions
    val states = remember(fields) { fields.map { mutableStateOf(it.initialValue) } }

    val hasChanges = states.zip(fields).any { (state, field) ->
        state.value != field.initialValue
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        states.forEachIndexed { index, state ->
            val field = fields[index]
            OutlinedTextField(
                value = state.value,
                onValueChange = { state.value = it },
                label = { Text(field.label) },
                placeholder = field.placeholder?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        if (hasChanges) {
            Button(
                onClick = { onSave(states.map { it.value.trim() }) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(saveLabel) }
        }
    }
}
