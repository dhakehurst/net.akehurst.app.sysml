/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.app.sysml.gui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val monospace = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun ErrorText(error: String?) {
    if (null != error) {
        Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp))
    }
}

/** pick one of a few options */
@Composable
fun <T> ChoiceList(options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    Column {
        options.forEach { o ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.selectable(selected = o == selected, onClick = { onSelect(o) }).padding(2.dp)
            ) {
                RadioButton(selected = o == selected, onClick = { onSelect(o) })
                Text(label(o))
            }
        }
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), singleLine: Boolean = true, minLines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun CodeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), minLines: Int = 10) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        textStyle = monospace,
        minLines = minLines,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

/** a list on the left that selects what is shown on the right */
@Composable
fun <T> MasterDetail(
    items: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    actions: @Composable ColumnScope.() -> Unit = {},
    detail: @Composable ColumnScope.() -> Unit
) {
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(260.dp).fillMaxHeight().padding(8.dp)) {
            items.forEach { item ->
                val isSelected = item == selected
                TextButton(onClick = { onSelect(item) }, modifier = Modifier.fillMaxWidth()) {
                    Text(label(item), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.fillMaxWidth())
                }
            }
            actions()
        }
        VerticalDivider()
        Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
            detail()
        }
    }
}
