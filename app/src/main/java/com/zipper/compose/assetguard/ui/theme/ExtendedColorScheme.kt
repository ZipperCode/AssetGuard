package com.zipper.compose.assetguard.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColorScheme(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

val LightExtendedColorScheme = ExtendedColorScheme(
    success = success_light,
    onSuccess = onSuccess_light,
    successContainer = successContainer_light,
    onSuccessContainer = onSuccessContainer_light,
    warning = warning_light,
    onWarning = onWarning_light,
    warningContainer = warningContainer_light,
    onWarningContainer = onWarningContainer_light,
    info = info_light,
    onInfo = onInfo_light,
    infoContainer = infoContainer_light,
    onInfoContainer = onInfoContainer_light,
)

val DarkExtendedColorScheme = ExtendedColorScheme(
    success = success_dark,
    onSuccess = onSuccess_dark,
    successContainer = successContainer_dark,
    onSuccessContainer = onSuccessContainer_dark,
    warning = warning_dark,
    onWarning = onWarning_dark,
    warningContainer = warningContainer_dark,
    onWarningContainer = onWarningContainer_dark,
    info = info_dark,
    onInfo = onInfo_dark,
    infoContainer = infoContainer_dark,
    onInfoContainer = onInfoContainer_dark,
)

val LocalExtendedColorScheme = staticCompositionLocalOf {
    LightExtendedColorScheme
}
