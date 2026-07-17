package com.aria.assistant.presentation.screen

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.aria.assistant.translation.TranslatedText as Text
import com.aria.assistant.translation.translatedUiText
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aria.assistant.data.feedback.BugReport
import com.aria.assistant.data.feedback.GithubComment
import com.aria.assistant.presentation.component.GlassCard
import com.aria.assistant.presentation.component.NebulaBackground
import com.aria.assistant.presentation.ui.theme.AuroraAmber
import com.aria.assistant.presentation.ui.theme.AuroraLavender
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.GlassStroke
import com.aria.assistant.presentation.ui.theme.NebulaBase
import com.aria.assistant.presentation.ui.theme.TextPrimary
import com.aria.assistant.presentation.ui.theme.TextSecondary
import com.aria.assistant.presentation.ui.theme.TextTertiary
import com.aria.assistant.presentation.viewmodel.FeedbackFormState
import com.aria.assistant.presentation.viewmodel.FeedbackViewModel
import com.aria.assistant.presentation.viewmodel.IssueDetailState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val formState by viewModel.formState.collectAsState()
    val issueDetail by viewModel.issueDetail.collectAsState()
    val bugReports by viewModel.bugReports.collectAsState(initial = emptyList())

    var activeIssueNumber by remember { mutableStateOf<Int?>(null) }
    var showNewReport by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.updateImageUri(uri)
    }

    val replyImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.updateReplyImageUri(uri)
    }

    NebulaBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = translatedUiText("Back"), tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Support & Feedback", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { viewModel.resetForm(); showNewReport = true },
                    enabled = viewModel.isConfigured,
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("New Report", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                val configErr = viewModel.configError
                if (configErr != null) {
                    Text(
                        text = configErr,
                        style = MaterialTheme.typography.bodySmall,
                        color = AuroraAmber,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuroraAmber.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (bugReports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No bug reports submitted yet.", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                    }
                } else {
                    bugReports.forEach { report ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeIssueNumber = report.number }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(report.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1)
                                    Text("#${report.number} · ${report.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                val statusColor = if (report.status == "open") AuroraTeal else Color(0xFFEF4444)
                                Text(
                                    report.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                        .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showNewReport) {
        ReportDialogFull(
            feedbackViewModel = viewModel,
            formState = formState,
            onImagePick = { imagePickerLauncher.launch("image/*") },
            onDismiss = { showNewReport = false }
        )
    }

    activeIssueNumber?.let { issueNumber ->
        IssueDetailDialogFull(
            feedbackViewModel = viewModel,
            issueDetail = issueDetail,
            issueNumber = issueNumber,
            onReplyImagePick = { replyImagePickerLauncher.launch("image/*") },
            onDismiss = {
                activeIssueNumber = null
                viewModel.clearIssueDetail()
            }
        )
    }
}

@Composable
private fun ReportDialogFull(
    feedbackViewModel: FeedbackViewModel,
    formState: FeedbackFormState,
    onImagePick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NebulaBase)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Report a Problem", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = translatedUiText("Close"), tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your report will be submitted to this app's GitHub issue tracker. " +
                    "Do not include passwords, private keys, medical information, financial " +
                    "information, or anything you do not want visible to the repository " +
                    "maintainers.",
            style = MaterialTheme.typography.bodySmall,
            color = AuroraAmber,
            modifier = Modifier
                .fillMaxWidth()
                .background(AuroraAmber.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .border(0.5.dp, AuroraAmber.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val scrollState = rememberScrollState()
        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
            OutlinedTextField(
                value = formState.title,
                onValueChange = { feedbackViewModel.updateTitle(it) },
                label = { Text("Title / Subject *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = formState.description,
                onValueChange = { feedbackViewModel.updateDescription(it) },
                label = { Text("Description *") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = formState.includeDiagnostics,
                    onCheckedChange = { feedbackViewModel.updateIncludeDiagnostics(it) },
                    colors = CheckboxDefaults.colors(checkedColor = AuroraViolet, uncheckedColor = TextTertiary)
                )
                Text("Include phone/app diagnostics", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = formState.userName,
                onValueChange = { feedbackViewModel.updateUserName(it) },
                label = { Text("Name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = formState.userEmail,
                onValueChange = { feedbackViewModel.updateUserEmail(it) },
                label = { Text("Email (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onImagePick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassStroke)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Attach Screenshot", style = MaterialTheme.typography.labelSmall)
            }

            formState.imageUri?.let { uri ->
                Spacer(modifier = Modifier.height(8.dp))
                ImagePreview(uri = uri, modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp))
            }
        }

        formState.error?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss, enabled = !formState.isSubmitting) {
                Text("Cancel", color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { feedbackViewModel.submitReport() },
                enabled = formState.title.isNotBlank() && formState.description.isNotBlank() && !formState.isSubmitting && feedbackViewModel.isConfigured,
                colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet)
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (formState.isSubmitting) "Submitting..." else "Submit", color = Color.White)
            }
        }
    }
}

@Composable
private fun IssueDetailDialogFull(
    feedbackViewModel: FeedbackViewModel,
    issueDetail: IssueDetailState,
    issueNumber: Int,
    onReplyImagePick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!issueDetail.isLoading && issueDetail.issue == null && issueDetail.error == null) {
        feedbackViewModel.loadIssueDetail(issueNumber)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NebulaBase)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(20.dp)
    ) {
        when {
            issueDetail.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AuroraViolet)
                }
            }
            issueDetail.error != null -> {
                Text(issueDetail.error, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFEF4444))
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { feedbackViewModel.loadIssueDetail(issueNumber) },
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry")
                }
            }
            else -> {
                val issue = issueDetail.issue
                if (issue != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(issue.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 2, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = translatedUiText("Close"), tint = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#$issueNumber", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        val statusColor = if (issue.state == "open") AuroraTeal else Color(0xFFEF4444)
                        Text(
                            issue.state.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Created ${issue.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Text("Comments (${issueDetail.comments.size})", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp))

                val commentsScrollState = rememberScrollState()
                Column(modifier = Modifier.weight(1f).verticalScroll(commentsScrollState)) {
                    if (issueDetail.comments.isEmpty()) {
                        Text("No comments yet.", style = MaterialTheme.typography.bodySmall, color = TextTertiary, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        issueDetail.comments.forEach { comment ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(comment.user.login, style = MaterialTheme.typography.labelMedium, color = AuroraViolet)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(comment.createdAt.take(10), style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(comment.body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                OutlinedTextField(
                    value = issueDetail.replyText,
                    onValueChange = { feedbackViewModel.updateReplyText(it) },
                    label = { Text("Write a reply...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReplyImagePick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassStroke)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Image", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { feedbackViewModel.postReply(issueNumber) },
                        enabled = issueDetail.replyText.isNotBlank() && !issueDetail.isPostingReply,
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal)
                    ) {
                        if (issueDetail.isPostingReply) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(if (issueDetail.isPostingReply) "..." else "Reply", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePreview(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) { null }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = translatedUiText("Image preview"),
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .border(0.5.dp, GlassStroke, RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = AuroraViolet,
    unfocusedBorderColor = GlassStroke,
    cursorColor = AuroraViolet,
    focusedLabelColor = AuroraLavender,
    unfocusedLabelColor = TextTertiary
)
