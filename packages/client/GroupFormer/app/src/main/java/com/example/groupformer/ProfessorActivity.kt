package com.example.groupformer

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickbirdstudios.surveykit.*
import com.quickbirdstudios.surveykit.result.*
import com.quickbirdstudios.surveykit.steps.*
import com.quickbirdstudios.surveykit.survey.SurveyView
import com.example.groupformer.ui.theme.MyApplicationTheme

class ProfessorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ButtonsLayout(
                        modifier = Modifier.padding(innerPadding),
                        onStartSurvey = { startSurvey() },
                        onReviewSurveys = { reviewSurveys() }
                    )
                }
            }
        }
    }

    @Composable
    fun ButtonsLayout(
        modifier: Modifier = Modifier,
        onStartSurvey: () -> Unit,
        onReviewSurveys: () -> Unit
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { onStartSurvey() }) {
                Text(text = "Create Survey")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { onReviewSurveys() }) {
                Text(text = "Review Surveys")
            }
        }
    }

    private fun startSurvey() {
        Log.d("Survey", "Start Survey Button Clicked")
        val container = FrameLayout(this)
        setContentView(container)

        val survey = SurveyView(this)

        survey.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        container.addView(survey)
        Log.d("Survey", "Survey view added to container")
        val steps = listOf(
            InstructionStep(
                title = "Survey Introduction",
                text = "Welcome to the survey. Please click Next to continue.",
                buttonText = "Start"
            ),
            QuestionStep(
                title = "Your Name",
                text = "What is your name?",
                answerFormat = AnswerFormat.TextAnswerFormat(maxLines = 1)
            ),
            CompletionStep(
                title = "Thank You!",
                text = "You have completed the survey.",
                buttonText = "Submit"
            )
        )

        val task = NavigableOrderedTask(steps = steps)

        val configuration = SurveyTheme(
            themeColorDark = ContextCompat.getColor(this, R.color.purple_700),
            themeColor = ContextCompat.getColor(this, R.color.purple_500),
            textColor = ContextCompat.getColor(this, R.color.white),
        )

        // Handle survey completion
        survey.onSurveyFinish = { taskResult: TaskResult, reason: FinishReason ->
            if (reason == FinishReason.Completed) {
                taskResult.results.forEach { stepResult ->
                    Log.e("Survey", "Answer: ${stepResult.results.firstOrNull()}")
                }
            } else {
                Log.e("Survey", "Survey was canceled")
            }
            container.removeAllViews()
        }

        Log.d("Survey", "Starting survey...")
        survey.start(task, configuration)
    }

    private fun reviewSurveys() {
        Log.d("Survey", "Review surveys clicked")
        // TODO: Retrieve Answers of Surveys
    }
}
