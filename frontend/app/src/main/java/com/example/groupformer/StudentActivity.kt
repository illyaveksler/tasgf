package com.example.groupformer

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
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.quickbirdstudios.surveykit.result.question_results.ScaleQuestionResult

class StudentActivity : ComponentActivity() {

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
                        SurveyScreen(questionnaireId, navController) // Pass questionnaireId
                    }
                }
            }
        }
    }

    @Composable
    fun MainScreen(navController: NavController) {
        val questionnaires = remember { mutableStateListOf<Questionnaire>() }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            fetchQuestionnaires(
                onSuccess = { fetchedSurveys ->
                    questionnaires.clear()
                    questionnaires.addAll(fetchedSurveys)
                },
                onFailure = { errorMessage ->
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Surveys to choose", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (questionnaires.isEmpty()) {
                Text("No surveys available.")
            } else {
                LazyColumn {
                    items(questionnaires) { questionnaire ->
                        SurveyItem(questionnaire) {
                            navController.navigate("survey_screen/${questionnaire.id}")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("main_screen") }) {
                Text("Back to Dashboard")
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
                .clickable { onClick() },
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
            // Use AndroidView to integrate SurveyView
            AndroidView(
                factory = { ctx ->
                    val surveyView = SurveyView(ctx)
                    surveyView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
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
                        )
                    ) + questionList + listOf(
                        CompletionStep(
                            title = "Survey Complete",
                            text = "Thank you for your responses!",
                            buttonText = "Finish"
                        )
                    )

                    val task = NavigableOrderedTask(steps = steps)

                    surveyView.onSurveyFinish = { taskResult: TaskResult, reason: FinishReason ->
                        if (reason == FinishReason.Completed) {
                            val responses = taskResult.results.mapNotNull { stepResult ->
                                val questionId = stepResult.id.id

                                val answer = when (val result = stepResult.results.firstOrNull()) {
                                    is ScaleQuestionResult -> result.answer?.toString()
                                    else -> null
                                }

                                answer?.let { Answer(questionId, it) }
                            }

                            // Send responses to API
                            sendAnswersToAPI(questionnaire!!.id, responses)

                            // Navigate to the success screen
                            navController.navigate("success_screen")
                        } else {
                            Log.d("Survey", "Survey was canceled")
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
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun sendAnswersToAPI(questionnaireId: String, answers: List<Answer>) {
        submitAnswersOkHttp(
            questionnaireId = questionnaireId,
            answers = answers,
            onSuccess = {
                Handler(Looper.getMainLooper()).post {
                    Log.d("Survey", "Answers submitted successfully")
                }
            },
            onFailure = { errorMessage ->
                Log.e("Survey", "Error submitting answers: $errorMessage")
            }
        )
    }
}
