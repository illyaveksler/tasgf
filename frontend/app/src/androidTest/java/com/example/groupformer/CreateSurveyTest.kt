package com.example.groupformer

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Before

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateSurveyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ProfessorActivity>()

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        // Start the MockWebServer before the test
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Set the mock server URL as the base URL in your shared preferences
        val context = composeTestRule.activity.applicationContext
        val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("api_base_url", mockWebServer.url("/").toString()).apply()

        // Set a fake authentication token
        prefs.edit().putString("google_id_token", "mock_token_123").apply()
    }

    @After
    fun tearDown() {
        // Shut down the mock server after the test
        mockWebServer.shutdown()
    }

    @Test
    fun createSurveyTest() {
        // Navigate to Create Survey screen
        composeTestRule.onNodeWithText("Create Survey").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Create Your Own Survey", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Create Your Own Survey").assertExists()

        // Check "Enter survey title" field exists
        val surveyTitleField = composeTestRule.onNodeWithText("Enter survey title")
        surveyTitleField.assertExists("Survey title input field not found!")

        // Check "Enter survey description" field exists
        val surveyDescriptionField = composeTestRule.onNodeWithText("Enter survey description")
        surveyDescriptionField.assertExists("Survey description input field not found!")

        // Check "Enter question" field exists
        val questionField = composeTestRule.onNodeWithText("Enter question")
        surveyDescriptionField.assertExists("Question input field not found!")

        // Create survey with empty fields
        composeTestRule.onNodeWithText("All Done! Create Survey").performClick()

        // Check proper title error text appears
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Title must not be empty!").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Title must not be empty!").assertExists()

        // Check proper description error text appears
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Description must not be empty!").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Description must not be empty!").assertExists()

        // Check proper question error text appears
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("You must add at least one question before creating!").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("You must add at least one question before creating!").assertExists()

        // Try adding empty question
        composeTestRule.onNodeWithText("Add Question").performClick()

        // Check proper empty question error text appears
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Question must not be empty!").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Question must not be empty!").assertExists()

        // Enter title, description fields
        surveyTitleField.performTextInput("Title1")

        surveyDescriptionField.performTextInput("Description1")

        // Try adding question not in the right format
        questionField.performTextInput("On a scal of 1-5, Does this test work?")

        composeTestRule.onNodeWithText("Add Question").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Question must start with: \"On a scale of 1-5...\"").fetchSemanticsNodes().isNotEmpty()
        }

        questionField.performTextClearance()

        // Add question in right format
        questionField.performTextInput("On a scale of 1-5, Does this test work?")

        composeTestRule.onNodeWithText("Add Question").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("- On a scale of 1-5, Does this test work?").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("- On a scale of 1-5, Does this test work?").assertExists()

        // Add second question in right format
        questionField.performTextInput("On a scale of 1-5, Does this test work again?")

        composeTestRule.onNodeWithText("Add Question").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("- On a scale of 1-5, Does this test work again?").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("- On a scale of 1-5, Does this test work again?").assertExists()

        // Set up mock internet error before submission
        val mockErrorResponse = MockResponse()
            .setResponseCode(500) // Internal Server Error
            .setBody("Internal Server Error")
        mockWebServer.enqueue(mockErrorResponse)

        composeTestRule.onNodeWithText("All Done! Create Survey").performClick()

        // Check for network error message
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Error submitting survey: Error: Internal Server Error").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Error submitting survey: Error: Internal Server Error").assertExists()

        // Prepare proper api response
        val successfulSubmitResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
        { 
          "id": "abc123",
          "title": "Customer Satisfaction Survey",
          "description": "Survey to gauge customer satisfaction",
          "questions": [
            { "id": "q1", "questionText": "How satisfied are you with our service?" },
            { "id": "q2", "questionText": "Would you recommend us to a friend?" }
          ],
          "createdAt": "2025-02-27T12:34:56Z"
        }
    """.trimIndent())
        mockWebServer.enqueue(successfulSubmitResponse)

        composeTestRule.onNodeWithText("All Done! Create Survey").performClick()

        // Check for success response
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Survey Created Successfully!").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Survey Created Successfully!").assertExists()
    }
}
