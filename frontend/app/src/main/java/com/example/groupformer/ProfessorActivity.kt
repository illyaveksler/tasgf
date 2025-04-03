package com.example.groupformer

import android.os.Bundle
import android.util.Log
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
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.delay


class ProfessorActivity : ComponentActivity() {
    companion object {
        private const val TAG = "ProfActivity"
    }

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
        var surveyTitleError by remember { mutableStateOf(false) }
        var surveyDescriptionError by remember { mutableStateOf(false) }
        var questionError by remember { mutableStateOf(false) }
        var surveySubmitAttempted by remember { mutableStateOf(false) }
        val questionList = remember { mutableStateListOf<QuestionStep>() }
        var networkErrorMessage by remember { mutableStateOf<String?>(null) }

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
                onValueChange = {
                    surveyTitle = it
                    surveyTitleError = it.isBlank()
                },
                label = { Text("Enter survey title") },
                modifier = Modifier.fillMaxWidth(),
                isError = surveyTitleError
            )
            if (surveyTitleError) {
                Text(
                    "Title must not be empty!",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Survey Description Input
            OutlinedTextField(
                value = surveyDescription,
                onValueChange = {
                    surveyDescription = it
                    surveyDescriptionError = it.isBlank()
                },
                label = { Text("Enter survey description") },
                modifier = Modifier.fillMaxWidth(),
                isError = surveyDescriptionError
            )
            if (surveyDescriptionError) {
                Text(
                    "Description must not be empty!",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
                )
            }

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
                    questionError = it.isBlank() || !it.startsWith("On a scale of 1-5")
                },
                label = { Text("Enter question") },
                modifier = Modifier.fillMaxWidth(),
                isError = questionError
            )

            // Error Message for Question Input
            if (questionError) {
                Text(
                    if (questionTitle.isBlank()) "Question must not be empty!"
                    else "Question must start with: \"On a scale of 1-5...\"",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Question Button
            Button(onClick = {
                questionError = questionTitle.isBlank() || !questionTitle.startsWith("On a scale of 1-5")
                if (!questionError) {
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
                    questionError = false // Remove error state
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
                surveySubmitAttempted = true
                surveyTitleError = surveyTitle.isBlank()
                surveyDescriptionError = surveyDescription.isBlank()

                if (!surveyTitleError && !surveyDescriptionError && questionList.isNotEmpty()) {
                    sendSurveyToAPI(context, surveyTitle, surveyDescription, questionList, navController, onFailure = { errorMessage ->
                        networkErrorMessage = errorMessage
                    })
                }
            }) {
                Text("All Done! Create Survey")
            }

            // Error message if user tries to submit without questions
            if (surveySubmitAttempted && (questionList.isEmpty() || networkErrorMessage != null)) {
                Text(networkErrorMessage ?: "You must add at least one question before creating!",
                    color = Color.Red,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center, // Ensures text is centered inside the Text composable
                    modifier = Modifier
                        .fillMaxWidth() // Makes sure it spans the full width for centering
                        .padding(start = 8.dp, top = 4.dp)
                        .align(Alignment.CenterHorizontally) // Centers the Text inside the Column
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { navController.navigate("main_screen") }) {
                Text("Back to Dashboard")
            }
        }
    }

    private fun sendSurveyToAPI(
        context: Context,
        title: String,
        description: String,
        questions: List<QuestionStep>,
        navController: NavController, // Added for navigation
        onFailure: (String) -> Unit
    ) {
        val questionList = questions.map {
            QuestionText(questionText = it.title)
        }

        submitSurveyOkHttp(
            context = context,
            title = title,
            description = description,
            questions = questionList,
            onSuccess = { questionnaireResponse ->
                Handler(Looper.getMainLooper()).post {
                    Log.d(TAG, "Survey submitted successfully: ${questionnaireResponse.id}")
                    navController.navigate("success_screen")
                }
            },
            onFailure = { errorMessage ->
                Handler(Looper.getMainLooper()).post {
                    Log.e(TAG, "Error submitting survey: $errorMessage")
                    onFailure(errorMessage)
                }
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

            Spacer(modifier = Modifier.height(16.dp))

            val activity = LocalActivity.current
            Button(onClick = { activity?.finish() }) {
                Text("Back to Role Selection")
            }
        }
    }

    @Composable
    fun SurveyScreen(navController: NavController) {
        val questionnaires = remember { mutableStateListOf<Questionnaire>() }
        val context = LocalContext.current
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val currentBackStackEntry by navController.currentBackStackEntryAsState()

        // Fetch surveys when the screen is loaded
        LaunchedEffect(currentBackStackEntry?.destination?.route) {
            if (currentBackStackEntry?.destination?.route == "survey_screen") {
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

            when {
                isLoading -> Text("Fetching Surveys...", fontSize = 16.sp, color = Color.Black)
                errorMessage != null -> Text("$errorMessage", fontSize = 16.sp, color = Color.Red)
                questionnaires.isEmpty() -> Text("No Surveys Available.", fontSize = 16.sp, color = Color.Black, modifier = Modifier.testTag("NoSurveysText"))
                else -> LazyColumn {
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
    fun FormGroupsScreen(navController: NavController, questionnaireId: String) {
        val context = LocalContext.current
        var isLoading by remember { mutableStateOf(true) }
        var questionnaire by remember { mutableStateOf<QuestionnaireResponse?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Fetch questionnaire details
        LaunchedEffect(questionnaireId) {
            // Fetch questionnaire when the screen is loaded
            fetchQuestionnaire(
                context = context,
                questionnaireId = questionnaireId,
                onSuccess = { survey ->
                    questionnaire = survey
                    isLoading = false
                    errorMessage = null
                },
                onFailure = { error ->
                    isLoading = false
                    errorMessage = error
                    questionnaire = null // Reset questionnaire if there’s an error
                }
            )
        }

        // Polling logic: Retry every 1 second if there's an error or no questionnaire
        LaunchedEffect(errorMessage) {
            if (errorMessage != null) {
                delay(1000L) // Retry after a 1 second delay
                fetchQuestionnaire(
                    context = context,
                    questionnaireId = questionnaireId,
                    onSuccess = { survey ->
                        questionnaire = survey
                        isLoading = false
                        errorMessage = null
                    },
                    onFailure = { error ->
                        isLoading = false
                        errorMessage = error
                        questionnaire = null // Reset questionnaire if there’s an error
                    }
                )
            }
        }
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().testTag("LoadingIndicator"), contentAlignment = Alignment.Center) {
                Text(text = "Loading Questionnaire...", fontSize = 18.sp, color = Color.Gray)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$errorMessage", color = Color.Red, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("survey_screen") }) {
                        Text("Back to List")
                    }
                }
            }
        } else {
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

                // State for group size input
                var groupSizeError by remember { mutableStateOf(false) }
                var networkErrorMessage by remember { mutableStateOf("") }
                var groupSize by remember { mutableStateOf("") } // User input for group size
                var groupLoading by remember { mutableStateOf(false) } // Loading state
                var groupResults by remember { mutableStateOf<List<List<Int>>?>(null) } // Store API response

                // Input field for group size
                OutlinedTextField(
                    value = groupSize,
                    onValueChange = {
                        groupSize = it.filter { char -> char.isDigit() }
                        groupSizeError = (groupSize.isNotEmpty() && groupSize.toIntOrNull() == 0)
                    },
                    label = { Text("Enter Group Size") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = groupSizeError // Set error state based on validation
                )

                // Error message if the group size is invalid
                if (groupSizeError) {
                    Text(
                        "Please enter a valid group size",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "Form Groups" Button
                Button(
                    onClick = {
                        if (groupSize.isEmpty() || groupSize.toIntOrNull() == null || groupSize.toInt() <= 0) {
                            groupSizeError =
                                true // Set error when the user tries to submit with invalid input
                            return@Button
                        }

                        // Reset errors and perform the network request
                        groupSizeError = false
                        groupLoading = true

                        formGroups(context, questionnaireId, groupSize.toInt()) { result, error ->
                            groupLoading = false
                            if (error != null) {
                                networkErrorMessage = error // Store the network error message
                            } else {
                                groupResults = result // Set generated groups on success
                                networkErrorMessage = "" // Reset network error message
                            }
                        }
                    },
                    enabled = !groupLoading
                ) {
                    Text(if (groupLoading) "Forming Groups..." else "Form Groups")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (networkErrorMessage.isNotEmpty()) {
                    Text(
                        networkErrorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
                    )
                } else if (groupResults != null) {
                    // Show the generated groups if they exist
                    groupResults?.let { groups ->
                        Text("Generated Groups:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            itemsIndexed(groups) { index, group ->
                                Text("Group ${index + 1}: ${group.joinToString(", ")}")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { navController.navigate("survey_screen") }) {
                    Text("Back to List")
                }
            }
        }
    }
}


