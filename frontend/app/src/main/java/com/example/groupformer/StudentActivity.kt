package com.example.groupformer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.quickbirdstudios.surveykit.*
import com.quickbirdstudios.surveykit.steps.*
import com.quickbirdstudios.surveykit.result.*
import com.quickbirdstudios.surveykit.survey.*
import com.quickbirdstudios.surveykit.AnswerFormat.*
import com.example.groupformer.ui.theme.MyApplicationTheme
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.quickbirdstudios.surveykit.result.question_results.IntegerQuestionResult
import com.quickbirdstudios.surveykit.result.question_results.ScaleQuestionResult
import kotlinx.coroutines.delay

class StudentActivity : ComponentActivity() {

    companion object {
        private const val TAG = "StudentActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                NavHost(navController, startDestination = "main_screen") {
                    composable("main_screen") { MainScreen(navController) }
                    composable("success_screen") { SuccessScreen(navController) }
                    composable("survey_screen/{questionnaireId}") { backStackEntry ->
                        val questionnaireId = backStackEntry.arguments?.getString("questionnaireId") ?: ""
                        SurveyScreen(questionnaireId, navController)
                    }
                    composable("error_screen/{errorMessage}") { backStackEntry ->
                        val errorMessage = backStackEntry.arguments?.getString("errorMessage") ?: "Unknown Error"
                        ErrorScreen(navController, errorMessage)
                    }
                }
            }
        }
    }

    @Composable
    fun MainScreen(navController: NavController) {
        val questionnaires = remember { mutableStateListOf<Questionnaire>() }
        val context = LocalContext.current
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Fetch surveys when the screen is loaded
        LaunchedEffect(Unit) {
            while (true) {
                fetchQuestionnaires(
                    context = context,
                    onSuccess = { fetchedSurveys ->
                        questionnaires.clear()
                        questionnaires.addAll(fetchedSurveys)
                        isLoading = false
                        errorMessage = null
                    },
                    onFailure = { error ->
                        isLoading = false
                        errorMessage = error
                    }
                )
                delay(1000L) // Refresh every 1 second
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text("Surveys to choose", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> Text("Fetching Surveys...", fontSize = 16.sp, color = Color.Black)
                errorMessage != null -> Text("Error fetching surveys: $errorMessage", fontSize = 16.sp, color = Color.Red)
                questionnaires.isEmpty() -> Text("No Surveys Available.", fontSize = 16.sp, color = Color.Black)
                else -> LazyColumn {
                    items(questionnaires) { questionnaire ->
                        SurveyItem(questionnaire) {
                            navController.navigate("survey_screen/${questionnaire.id}")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val activity = LocalActivity.current
            Button(onClick = { activity?.finish() }) {
                Text("Back to Role Selection")
            }
        }
    }

    @Composable
    fun SuccessScreen(navController: NavController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Survey Submitted Successfully!", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { navController.navigate("main_screen") }) {
                Text("Back to Main Page")
            }
        }
    }

    @Composable
    fun SurveyItem(questionnaire: Questionnaire, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onClick() }
                .semantics { contentDescription = "Survey Item" },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(questionnaire.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(questionnaire.description, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Created: ${questionnaire.createdAt}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }

    @Composable
    fun SurveyScreen(questionnaireId: String, navController: NavController) {
        val context = LocalContext.current
        var questionnaire by remember { mutableStateOf<QuestionnaireResponse?>(null) }

        LaunchedEffect(questionnaireId) {
            fetchQuestionnaire(
                context,
                questionnaireId,
                onSuccess = { survey -> questionnaire = survey },
                onFailure = { errorMessage ->
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Show loading until questionnaire is available
        if (questionnaire == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    val surveyView = SurveyView(ctx)
                    surveyView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    val studentIdQuestion = QuestionStep(
                        title = "Student ID",
                        text = "Please enter your Student ID",
                        answerFormat = IntegerAnswerFormat(), // Ensure integer input
                        id = StepIdentifier("student_id")
                    )

                    val questionList = questionnaire!!.questions.map { question ->
                        QuestionStep(
                            title = question.questionText,
                            text = "Please provide your answer",
                            answerFormat = ScaleAnswerFormat(
                                minimumValue = 1,
                                maximumValue = 5,
                                step = 1f,
                                maximumValueDescription = "",
                                minimumValueDescription = ""
                            ),
                            id = StepIdentifier(question.id)
                        )
                    }

                    val steps = listOf(
                        InstructionStep(
                            title = "Welcome to ${questionnaire!!.title}",
                            text = questionnaire!!.description,
                            buttonText = "Start"
                        ),
                        studentIdQuestion
                    ) + questionList + listOf(
                        CompletionStep(
                            title = "Survey Complete",
                            text = "Thank you for your responses!",
                            buttonText = "Submit Answers"
                        )
                    )

                    val task = NavigableOrderedTask(steps = steps)

                    surveyView.onSurveyFinish = { taskResult: TaskResult, reason: FinishReason ->
                        if (reason == FinishReason.Completed) {
                            var studentId: Int? = null
                            val responses = taskResult.results.mapNotNull { stepResult ->
                                val questionId = stepResult.id.id

                                // Extract Student ID separately
                                if (questionId == "student_id") {
                                    studentId = (stepResult.results.firstOrNull() as? IntegerQuestionResult)?.answer
                                    Log.d(TAG, "Collected Student ID: $studentId") // Log Student ID
                                    return@mapNotNull null // Don't include in survey responses
                                }

                                val answer = when (val result = stepResult.results.firstOrNull()) {
                                    is ScaleQuestionResult -> result.answer?.toString()
                                    else -> null
                                }

                                answer?.let { Answer(questionId, it) }
                            }

                            sendAnswersToAPI(
                                context,
                                questionnaire!!.id,
                                responses,
                                studentId!!,
                                onSuccess = {
                                    navController.navigate("success_screen")
                                },
                                onFailure = { errorMessage ->
                                    navController.navigate("error_screen/$errorMessage")
                                }
                            )
                        } else {
                            Log.d(TAG, "Survey was canceled")
                            navController.navigate("main_screen")
                        }
                    }

                    val configuration = SurveyTheme(
                        themeColorDark = ContextCompat.getColor(ctx, R.color.purple_700),
                        themeColor = ContextCompat.getColor(ctx, R.color.purple_500),
                        textColor = ContextCompat.getColor(ctx, R.color.white)
                    )

                    surveyView.start(task, configuration)
                    surveyView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("SurveyView")
            )
        }
    }

    private fun sendAnswersToAPI(context: Context, questionnaireId: String, answers: List<Answer>, studentId: Int, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        submitAnswersOkHttp(
            context = context,
            questionnaireId = questionnaireId,
            answers = answers,
            studentId = studentId,
            onSuccess = {
                Handler(Looper.getMainLooper()).post {
                    onSuccess()
                    Log.d(TAG, "Answers submitted successfully")
                }
            },
            onFailure = { errorMessage ->
                onFailure(errorMessage)
                Log.e(TAG, "Error submitting answers: $errorMessage")
            }
        )
    }
}

@Composable
fun ErrorScreen(navController: NavController, errorMessage: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error Submitting Survey",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Red
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("main_screen") }) {
            Text("Back to Main Screen")
        }
    }
}

