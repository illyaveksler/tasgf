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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.endsWith

@RunWith(AndroidJUnit4::class)
class FillOutSurveyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        // Start a new instance of the mock server
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
    fun fillOutSurveyTest() {
        // Mock an empty response for /questionnaires
        val emptySurveyResponse = MockResponse()
            .setResponseCode(200)
            .setBody("[]") // Empty list
        mockWebServer.enqueue(emptySurveyResponse)

        // Navigate to "Student" screen
        composeTestRule.onNodeWithText("Student").performClick()

        // Check for "No Surveys Available" text
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("No Surveys Available.").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No Surveys Available.").assertExists()

        val mockErrorResponse = MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error")
        mockWebServer.enqueue(mockErrorResponse)

        // Verify the error message appears in the UI
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Error fetching surveys: Internal Server Error", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Mock a new response with available surveys for student page
        val mockSurveyResponse = MockResponse()
            .setResponseCode(200)
            .setBody(
                """
            [
                {
                    "id": "abc123",
                    "title": "Customer Satisfaction Survey",
                    "description": "Survey to gauge customer satisfaction",
                    "createdAt": "2025-02-27T12:34:56Z"
                },
                {
                    "id": "def456",
                    "title": "Employee Feedback Survey",
                    "description": "Survey to gather employee feedback",
                    "createdAt": "2025-02-26T09:00:00Z"
                }
            ]
        """.trimIndent()
            )
        mockWebServer.enqueue(mockSurveyResponse)

        // Wait for survey items to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithContentDescription("Survey Item").fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Click the first survey item
        composeTestRule.onAllNodesWithContentDescription("Survey Item")[0].performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasTestTag("LoadingIndicator")).fetchSemanticsNodes().isNotEmpty()
        }

        mockWebServer.enqueue(mockErrorResponse)

        // Verify the error message appears in the UI
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Error fetching survey: Internal Server Error", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Mock response for Survey Details API
        val mockSurveyResponse2 = MockResponse()
            .setResponseCode(200)
            .setBody(
                """
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
            """.trimIndent()
            )

        mockWebServer.enqueue(mockSurveyResponse2)

        // Wait for the survey details screen to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("SurveyView").fetchSemanticsNodes().isNotEmpty()
        }

        // Start survey by finding start button and clicking
        onView(withText("Start")).perform(click())

        // Enter Student ID (e.g., "12345") and press Next
        onView(withClassName(endsWith("EditText"))) // Find input field
            .perform(replaceText("12345"), closeSoftKeyboard())
        onView(withText("Next")).perform(click())

        Thread.sleep(1000)
        // Answer first question by pressing Next
        onView(withText("Next")).perform(click())

        Thread.sleep(1000)
        // Answer second question by pressing Next
        onView(withText("Next")).perform(click())

        // Prepare successful response
        val mockSurveyResponse3 = MockResponse()
            .setResponseCode(200)
            .setBody("Success")
        mockWebServer.enqueue(mockSurveyResponse3)

        // Submit survey successfully
        Thread.sleep(1000)
        onView(withText("Submit Answers")).perform(click())

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Survey Submitted Successfully!").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // This test is the same except it checks for when there's a network error at the end submitting the survey
    @Test
    fun errorSubmitSurveyTest() {
        val emptySurveyResponse = MockResponse()
            .setResponseCode(200)
            .setBody("[]") // Empty list
        mockWebServer.enqueue(emptySurveyResponse)

        // Navigate to "Student" screen
        composeTestRule.onNodeWithText("Student").performClick()

        // Check for "No Surveys Available" text
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("No Surveys Available.").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No Surveys Available.").assertExists()

        // Mock a new response with available surveys for student page
        val mockSurveyResponse = MockResponse()
            .setResponseCode(200)
            .setBody(
                """
            [
                {
                    "id": "abc123",
                    "title": "Customer Satisfaction Survey",
                    "description": "Survey to gauge customer satisfaction",
                    "createdAt": "2025-02-27T12:34:56Z"
                },
                {
                    "id": "def456",
                    "title": "Employee Feedback Survey",
                    "description": "Survey to gather employee feedback",
                    "createdAt": "2025-02-26T09:00:00Z"
                }
            ]
        """.trimIndent()
            )
        mockWebServer.enqueue(mockSurveyResponse)

        // Wait for survey items to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithContentDescription("Survey Item").fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Click the first survey item
        composeTestRule.onAllNodesWithContentDescription("Survey Item")[0].performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasTestTag("LoadingIndicator")).fetchSemanticsNodes().isNotEmpty()
        }

        // Mock response for Survey Details API
        val mockSurveyResponse2 = MockResponse()
            .setResponseCode(200)
            .setBody(
                """
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
            """.trimIndent()
            )

        mockWebServer.enqueue(mockSurveyResponse2)

        // Wait for the survey details screen to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("SurveyView").fetchSemanticsNodes().isNotEmpty()
        }

        // Start survey by finding start button and clicking
        onView(withText("Start")).perform(click())

        // Enter Student ID (e.g., "12345") and press Next
        onView(withClassName(endsWith("EditText"))) // Find input field
            .perform(replaceText("12345"), closeSoftKeyboard())
        onView(withText("Next")).perform(click())

        Thread.sleep(1000)
        // Answer first question by pressing Next
        onView(withText("Next")).perform(click())

        Thread.sleep(1000)
        // Answer second question by pressing Next
        onView(withText("Next")).perform(click())

        val mockErrorResponse = MockResponse()
            .setResponseCode(500) // Internal Server Error
            .setBody("Internal Server Error")
        mockWebServer.enqueue(mockErrorResponse)

        // Finish survey and check for error
        Thread.sleep(1000)
        onView(withText("Submit Answers")).perform(click())

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Error Submitting Survey").fetchSemanticsNodes().isNotEmpty()
        }

    }
}