package com.zipper.compose.assetguard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zipper.compose.assetguard.ui.theme.AssetGuardTheme
import com.zipper.compose.assetguard.util.MoneyUtils

@Composable
fun MoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    prefix: String = "¥"
) {
    Text(
        text = "$prefix${MoneyUtils.centsToYuanString(cents)}",
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}

@Preview(showBackground = true)
@Composable
private fun MoneyTextPreview() {
    AssetGuardTheme {
        Column(modifier = Modifier.padding(8.dp)) {
            MoneyText(cents = 1234567L)
            MoneyText(cents = 0L)
            MoneyText(cents = -5000L, color = Color.Red)
        }
    }
}