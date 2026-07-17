package com.aria.assistant.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class UiLanguage(
    val tag: String,
    val englishName: String,
    val nativeName: String
)

sealed interface TranslationStatus {
    data object Ready : TranslationStatus
    data class Downloading(val languageTag: String) : TranslationStatus
    data class Error(val message: String) : TranslationStatus
}

@Singleton
class UiTranslationManager @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val modelManager by lazy { RemoteModelManager.getInstance() }
    private val translators = ConcurrentHashMap<String, Translator>()
    private val readiness = ConcurrentHashMap<String, Deferred<Unit>>()
    private val cache = ConcurrentHashMap<String, String>()

    private val _targetLanguage = MutableStateFlow(ENGLISH)
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    private val _status = MutableStateFlow<TranslationStatus>(TranslationStatus.Ready)
    val status: StateFlow<TranslationStatus> = _status.asStateFlow()

    val supportedLanguages: List<UiLanguage> by lazy {
        TranslateLanguage.getAllLanguages()
            .map { tag ->
                val locale = Locale.forLanguageTag(tag)
                UiLanguage(
                    tag = tag,
                    englishName = locale.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { it.titlecase(Locale.ENGLISH) },
                    nativeName = locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
                )
            }
            .sortedWith(compareBy<UiLanguage> { it.tag != ENGLISH }.thenBy { it.englishName })
    }

    fun setTargetLanguage(languageTag: String) {
        val supportedTag = TranslateLanguage.fromLanguageTag(languageTag) ?: ENGLISH
        if (_targetLanguage.value == supportedTag) return
        if (supportedTag == ENGLISH) {
            _targetLanguage.value = ENGLISH
            _status.value = TranslationStatus.Ready
        } else {
            scope.launch {
                try {
                    prepareLanguage(supportedTag)
                    _targetLanguage.value = supportedTag
                } catch (_: Exception) {
                    // Keep the last usable language active. The error is exposed through status.
                }
            }
        }
    }

    suspend fun isLanguageDownloaded(languageTag: String): Boolean {
        val supportedTag = TranslateLanguage.fromLanguageTag(languageTag) ?: return false
        if (supportedTag == ENGLISH) return true
        return modelManager.isModelDownloaded(remoteModelFor(supportedTag)).awaitResult()
    }

    suspend fun prepareLanguage(languageTag: String) {
        val supportedTag = TranslateLanguage.fromLanguageTag(languageTag)
            ?: throw IllegalArgumentException("Unsupported translation language: $languageTag")
        if (supportedTag == ENGLISH) {
            _status.value = TranslationStatus.Ready
            return
        }
        if (isLanguageDownloaded(supportedTag)) {
            _status.value = TranslationStatus.Ready
            return
        }

        val job = readiness.getOrPut(supportedTag) {
            scope.async {
                _status.value = TranslationStatus.Downloading(supportedTag)
                try {
                    translatorFor(supportedTag).downloadModelIfNeeded(
                        DownloadConditions.Builder().build()
                    ).awaitResult()
                    if (!isLanguageDownloaded(supportedTag)) {
                        error("The downloaded language model could not be verified")
                    }
                    _status.value = TranslationStatus.Ready
                } catch (error: Exception) {
                    _status.value = TranslationStatus.Error(
                        error.message ?: "Could not download the language model"
                    )
                    throw error
                }
            }
        }
        try {
            job.await()
        } finally {
            readiness.remove(supportedTag, job)
        }
    }

    suspend fun translate(englishText: String, languageTag: String = _targetLanguage.value): String {
        val supportedTag = TranslateLanguage.fromLanguageTag(languageTag) ?: return englishText
        if (supportedTag == ENGLISH || englishText.isBlank()) return englishText
        val cacheKey = "$supportedTag\u0000$englishText"
        cache[cacheKey]?.let { return it }
        return try {
            prepareLanguage(supportedTag)
            translatorFor(supportedTag).translate(englishText).awaitResult().also {
                cache[cacheKey] = it
            }
        } catch (_: Exception) {
            englishText
        }
    }

    private fun translatorFor(targetTag: String): Translator {
        return translators.getOrPut(targetTag) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(targetTag)
                    .build()
            )
        }
    }

    private fun remoteModelFor(languageTag: String): TranslateRemoteModel =
        TranslateRemoteModel.Builder(languageTag).build()

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }

    companion object {
        const val ENGLISH = "en"
    }
}
