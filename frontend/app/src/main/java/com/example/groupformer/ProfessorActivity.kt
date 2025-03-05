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
import com.example.groupformer.ui.theme.MyApplicationTheme
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType


class ProfessorActivity : ComponentActivity() {
    private val questionList = mutableStateListOf<QuestionStep>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                NavHost(navController, startDestination = "main_screen") {
                    composable("main_screen") { MainScreen(navController) }
                    composable("survey_builder") { SurveyBuilderScreen(navController) }
                    composable("survey_screen") { SurveyScreen(navController) }
                    composable("success_screen") { SuccessScreen(navController) }
                    composable("form_groups/{questionnaireId}") { backStackEntry ->
                        val questionnaireId = backStackEntry.arguments?.getString("questionnaireId") ?: ""
                        FormGroupsScreen(navController, questionnaireId)
                    }
                }
            }
        }
    }

    @Composable
    fun SurveyBuilderScreen(navController: NavController) {
        var surveyTitle by remember { mutableStateOf("") }
        var surveyDescription by remember { mutableStateOf("") }
        var questionTitle by remember { mutableStateOf("") }
        var questionError by remember { mutableStateOf(false) } // Track format error
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text("Create Your Own Survey", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            // Survey Title Input
            OutlinedTextField(
                value = surveyTitle,
                onValueChange = { surveyTitle = it },
                label = { Text("Enter survey title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Survey Description Input
            OutlinedTextField(
                value = surveyDescription,
                onValueChange = { surveyDescription = it },
                label = { Text("Enter survey description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Instruction for question format
            Text(
                "Questions must be in this format:\n" +
                        "\"On a scale of 1-5, ... ?\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )

            // Question Input
            OutlinedTextField(
                value = questionTitle,
                onValueChange = {
                    questionTitle = it
                    questionError = !it.startsWith("On a scale of 1-5") // Validate format
                },
                label = { Text("Enter question") },
                modifier = Modifier.fillMaxWidth(),
                isError = questionError
            )

            // Error Message
            if (questionError) {
                Text(
                    "Question must start with: \"On a scale of 1-5...\"",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Question Button
            Button(onClick = {
                if (questionTitle.isBlank()) {
                    Toast.makeText(context, "Question cannot be empty!", Toast.LENGTH_SHORT).show()
                } else if (questionError) {
                    Toast.makeText(context, "Question must follow the required format!", Toast.LENGTH_SHORT).show()
                } else {
                    questionList.add(
                        QuestionStep(
                            title = questionTitle,
                            text = "Answer this question",
                            answerFormat = AnswerFormat.ScaleAnswerFormat(
                                minimumValue = 1,
                                maximumValue = 5,
                                step = 1f,
                                minimumValueDescription = "",
                                maximumValueDescription = ""
                            )
                        )
                    )
                    questionTitle = "" // Reset input
                }
            }) {
                Text("Add Question")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Show added questions
            LazyColumn {
                items(questionList) { question ->
                    Text("- ${question.title}", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Survey Button
            Button(onClick = {
                if (surveyTitle.isBlank() || surveyDescription.isBlank()) {
                    Toast.makeText(context, "Enter a title and description!", Toast.LENGTH_SHORT).show()
                } else if (questionList.isEmpty()) {
                    Toast.makeText(context, "Add at least one question!", Toast.LENGTH_SHORT).show()
                } else {
                    sendSurveyToAPI(surveyTitle, surveyDescription, questionList, navController)
                }
            }) {
                Text("All Done! Create Survey")
            }
        }
    }

    private fun sendSurveyToAPI(
        title: String,
        description: String,
        questions: List<QuestionStep>,
        navController: NavController // Added for navigation
    ) {
        val questionList = questions.map {
            QuestionText(questionText = it.title)
        }

        submitSurveyOkHttp(
            title = title,
            description = description,
            questions = questionList,
            onSuccess = { questionnaireResponse ->
                Handler(Looper.getMainLooper()).post {
                    Log.d("Survey", "Survey submitted successfully: ${questionnaireResponse.id}")
                    navController.navigate("success_screen")
                }
            },
            onFailure = { errorMessage ->
                Log.e("Survey", "Error submitting survey: $errorMessage")
            }
        )
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
            Text("Survey Created Successfully!", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { navController.navigate("main_screen") }) {
                Text("Back to Main Page")
            }
        }
    }

    @Composable
    fun MainScreen(navController: NavController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Survey Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("survey_builder") }) {
                Text("Create Survey")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("survey_screen") }) {
                Text("Form Groups")
            }
        }
    }

    @Composable
    fun SurveyScreen(navController: NavController) {
        val questionnaires = remember { mutableStateListOf<Questionnaire>() }
        val context = LocalContext.current

        // Fetch surveys when the screen is loaded
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text("Choose a survey to form groups", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            if (questionnaires.isEmpty()) {
                Text("No surveys available.")
            } else {
                LazyColumn {
                    items(questionnaires) { questionnaire ->
                        SurveyItem(questionnaire) {
                            navController.navigate("form_groups/${questionnaire.id}")
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
    fun FormGroupsScreen(navController: NavController, questionnaireId: String) {
        var questionnaire by remember { mutableStateOf<QuestionnaireResponse?>(null) }
        val context = LocalContext.current

        var groupSize by remember { mutableStateOf("") } // User input for group size
        var isLoading by remember { mutableStateOf(false) } // Loading state
        var groupResults by remember { mutableStateOf<List<List<Int>>?>(null) } // Store API response

        // Fetch questionnaire details
        LaunchedEffect(questionnaireId) {
            fetchQuestionnaire(
                questionnaireId = questionnaireId,
                onSuccess = { survey -> questionnaire = survey },
                onFailure = { errorMessage ->
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Survey Details", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            questionnaire?.let { survey ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Title: ${survey.title}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Description: ${survey.description}", fontSize = 16.sp)
                Text("Created At: ${survey.createdAt}", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(16.dp))

                // Display the list of questions
                Text("Survey Questions:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(survey.questions) { question ->
                        Text("- ${question.questionText}", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            } ?: Text("Loading...")

            Spacer(modifier = Modifier.height(16.dp))

            // User input field for group size
            OutlinedTextField(
                value = groupSize,
                onValueChange = { groupSize = it.filter { char -> char.isDigit() } }, // Only allow numbers
                label = { Text("Enter Group Size") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // "Form Groups" Button
            Button(
                onClick = {
                    if (groupSize.isNotEmpty() && groupSize.toIntOrNull() != null) {
                        isLoading = true
                        formGroups(questionnaireId, groupSize.toInt()) { result, error ->
                            isLoading = false
                            if (error != null) {
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                            } else {
                                groupResults = result
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please enter a valid group size", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Forming Groups..." else "Form Groups")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show group results
            groupResults?.let { groups ->
                Text("Generated Groups:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    itemsIndexed(groups) { index, group ->
                        Text("Group ${index + 1}: ${group.joinToString(", ")}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back Button
            Button(onClick = { navController.navigate("review_screen") }) {
                Text("Back to List")
            }
        }
    }

}


