package dev.prootcode.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null,
    maxLength: Int? = null,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            if (maxLength == null) onValueChange(text) else onValueChange(text.take(maxLength))
        },
        label = { Text(label) },
        singleLine = singleLine,
        isError = isError,
        supportingText =
            if (supportingText != null) {
                { Text(supportingText) }
            } else {
                null
            },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation =
            if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier.fillMaxWidth(),
    )
}
