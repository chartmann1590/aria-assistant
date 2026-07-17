package com.aria.assistant.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.assistant.BuildConfig
import com.aria.assistant.data.feedback.BugReport
import com.aria.assistant.data.feedback.BugReportRepo
import com.aria.assistant.data.feedback.DiagnosticsHelper
import com.aria.assistant.data.feedback.GithubClient
import com.aria.assistant.data.feedback.GithubComment
import com.aria.assistant.data.feedback.GithubIssue
import com.aria.assistant.data.feedback.ImageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class FeedbackFormState(
    val title: String = "",
    val description: String = "",
    val includeDiagnostics: Boolean = true,
    val userName: String = "",
    val userEmail: String = "",
    val imageUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

data class IssueDetailState(
    val isLoading: Boolean = false,
    val issue: GithubIssue? = null,
    val comments: List<GithubComment> = emptyList(),
    val replyText: String = "",
    val replyImageUri: Uri? = null,
    val isPostingReply: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val bugReportRepo: BugReportRepo,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isConfigured: Boolean
        get() = GithubClient.isConfigured

    val configError: String?
        get() = when {
            BuildConfig.GITHUB_REPO_OWNER.isBlank() || BuildConfig.GITHUB_REPO_NAME.isBlank() ->
                "GitHub repository not configured. Add github.repo.owner and github.repo.name to local.properties."
            else -> null
        }

    private val _formState = MutableStateFlow(FeedbackFormState())
    val formState: StateFlow<FeedbackFormState> = _formState.asStateFlow()

    private val _issueDetail = MutableStateFlow(IssueDetailState())
    val issueDetail: StateFlow<IssueDetailState> = _issueDetail.asStateFlow()

    val bugReports = bugReportRepo.bugReports

    fun updateTitle(title: String) {
        _formState.value = _formState.value.copy(title = title)
    }

    fun updateDescription(desc: String) {
        _formState.value = _formState.value.copy(description = desc)
    }

    fun updateIncludeDiagnostics(include: Boolean) {
        _formState.value = _formState.value.copy(includeDiagnostics = include)
    }

    fun updateUserName(name: String) {
        _formState.value = _formState.value.copy(userName = name)
    }

    fun updateUserEmail(email: String) {
        _formState.value = _formState.value.copy(userEmail = email)
    }

    fun updateImageUri(uri: Uri?) {
        _formState.value = _formState.value.copy(imageUri = uri)
    }

    fun resetForm() {
        _formState.value = FeedbackFormState()
    }

    fun submitReport() {
        val state = _formState.value
        if (state.title.isBlank() || state.description.isBlank()) return
        if (!isConfigured) return

        _formState.value = state.copy(isSubmitting = true, error = null, success = false)

        viewModelScope.launch {
            try {
                val body = buildIssueBody(state)
                val issueTitle = "[Feedback] ${state.title}"
                val githubIssue = GithubClient.createIssue(issueTitle, body)

                val report = BugReport(
                    number = githubIssue.number,
                    title = state.title,
                    status = githubIssue.state,
                    createdAt = githubIssue.createdAt,
                    htmlUrl = githubIssue.htmlUrl
                )

                bugReportRepo.saveBugReport(report)
                _formState.value = FeedbackFormState(success = true)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSubmitting = false,
                    error = "Failed to submit: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    private suspend fun buildIssueBody(state: FeedbackFormState): String {
        val parts = mutableListOf<String>()

        parts.add("## Description")
        parts.add("")
        parts.add(state.description)
        parts.add("")

        parts.add("## Contact Info")
        parts.add("")
        parts.add("- Name: ${state.userName.ifBlank { "Not provided" }}")
        parts.add("- Email: ${state.userEmail.ifBlank { "Not provided" }}")
        parts.add("")

        if (state.imageUri != null) {
            val result = ImageHelper.uriToBase64(context, state.imageUri)
            result.onSuccess { base64 ->
                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val random = (1000..9999).random()
                val filename = "issue-$timestamp-$random.png"
                val downloadUrl = GithubClient.uploadAsset(filename, base64)
                if (downloadUrl != null) {
                    parts.add("## Attachment")
                    parts.add("")
                    parts.add("![Screenshot]($downloadUrl)")
                    parts.add("")
                }
            }
        }

        if (state.includeDiagnostics) {
            parts.add(DiagnosticsHelper.collect(context))
        }

        return parts.joinToString("\n")
    }

    fun loadIssueDetail(number: Int) {
        _issueDetail.value = IssueDetailState(isLoading = true)
        viewModelScope.launch {
            try {
                val issue = GithubClient.getIssue(number)
                val comments = GithubClient.getComments(number)
                _issueDetail.value = IssueDetailState(
                    isLoading = false,
                    issue = issue,
                    comments = comments
                )
                val reports = bugReportRepo.getBugReportsList()
                val existing = reports.find { it.number == number }
                if (existing != null && existing.status != issue.state) {
                    bugReportRepo.saveBugReport(existing.copy(status = issue.state))
                }
            } catch (e: Exception) {
                _issueDetail.value = IssueDetailState(
                    isLoading = false,
                    error = "Failed to load issue: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun updateReplyText(text: String) {
        _issueDetail.value = _issueDetail.value.copy(replyText = text)
    }

    fun updateReplyImageUri(uri: Uri?) {
        _issueDetail.value = _issueDetail.value.copy(replyImageUri = uri)
    }

    fun postReply(issueNumber: Int) {
        val state = _issueDetail.value
        if (state.replyText.isBlank()) return

        _issueDetail.value = state.copy(isPostingReply = true, error = null)

        viewModelScope.launch {
            try {
                val body = buildReplyBody(state)
                val comment = GithubClient.postComment(issueNumber, body)

                val updatedComments = _issueDetail.value.comments + comment
                _issueDetail.value = _issueDetail.value.copy(
                    comments = updatedComments,
                    replyText = "",
                    replyImageUri = null,
                    isPostingReply = false
                )
            } catch (e: Exception) {
                _issueDetail.value = _issueDetail.value.copy(
                    isPostingReply = false,
                    error = "Failed to post reply: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    private suspend fun buildReplyBody(state: IssueDetailState): String {
        val parts = mutableListOf<String>()
        parts.add("## Reply")
        parts.add("")
        parts.add(state.replyText)
        parts.add("")

        if (state.replyImageUri != null) {
            val result = ImageHelper.uriToBase64(context, state.replyImageUri)
            result.onSuccess { base64 ->
                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val random = (1000..9999).random()
                val filename = "comment-$timestamp-$random.png"
                val downloadUrl = GithubClient.uploadAsset(filename, base64)
                if (downloadUrl != null) {
                    parts.add("## Attachment")
                    parts.add("")
                    parts.add("![Screenshot]($downloadUrl)")
                    parts.add("")
                }
            }
        }

        return parts.joinToString("\n")
    }

    fun clearIssueDetail() {
        _issueDetail.value = IssueDetailState()
    }
}
