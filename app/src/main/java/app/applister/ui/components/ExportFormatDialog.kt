package app.applister.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.applister.R
import app.applister.data.Constants

@Composable
fun ExportFormatDialog(
    title: String = stringResource(R.string.export_format),
    defaultFormat: Int = Constants.ExportFormat.MARKDOWN,
    onFormatSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableIntStateOf(defaultFormat) }

    val formats = listOf(
        Constants.ExportFormat.MARKDOWN to stringResource(R.string.markdown_md),
        Constants.ExportFormat.PLAIN_TEXT to stringResource(R.string.plain_text_txt),
        Constants.ExportFormat.JSON to stringResource(R.string.json_format),
        Constants.ExportFormat.HTML to stringResource(R.string.html_format)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                formats.forEach { (format, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onFormatSelected(selectedFormat) }) {
                Text(stringResource(R.string.export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
