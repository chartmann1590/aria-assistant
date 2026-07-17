package com.aria.assistant.translation

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LocalTranslationManager = staticCompositionLocalOf<UiTranslationManager?> { null }
private val LocalUiLanguage = staticCompositionLocalOf { UiTranslationManager.ENGLISH }

@Composable
fun UiTranslationProvider(
    languageTag: String,
    manager: UiTranslationManager,
    content: @Composable () -> Unit
) {
    LaunchedEffect(languageTag) { manager.setTargetLanguage(languageTag) }
    val activeLanguage by manager.targetLanguage.collectAsState()
    val layoutDirection = if (activeLanguage in RTL_LANGUAGES) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalTranslationManager provides manager,
        LocalUiLanguage provides activeLanguage,
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}

@Composable
fun translatedUiText(englishText: String, translate: Boolean = true): String {
    val manager = LocalTranslationManager.current
    val languageTag = LocalUiLanguage.current
    if (!translate || manager == null || languageTag == UiTranslationManager.ENGLISH) return englishText
    var translated by remember(englishText, languageTag, manager) { mutableStateOf(englishText) }
    LaunchedEffect(englishText, languageTag, manager) {
        translated = withContext(Dispatchers.IO) {
            manager.translate(englishText, languageTag)
        }
    }
    return translated
}

@Composable
fun TranslatedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    translate: Boolean = true
) {
    MaterialText(
        text = translatedUiText(text, translate),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

private val RTL_LANGUAGES = setOf("ar", "fa", "he", "ur")
