package com.example.groupformer

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
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
class FormGroupsTest {

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
    fun formGroupsTest() {

        // Navigate to "Form Groups" screen
        composeTestRule.onNodeWithText("Form Groups").performClick()

        // Wait until "Choose a survey to form groups" text is visible
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Choose a survey to form groups", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Check that the text "Choose a survey to form groups" is displayed
        composeTestRule.onNodeWithText("Choose a survey to form groups").assertExists()

        // Check for empty survey list response
        val emptySurveyResponse = MockResponse()
            .setResponseCode(200)
            .setBody("[]") // Empty list
        mockWebServer.enqueue(emptySurveyResponse)


        // Check for "No Surveys Available" text after the empty response
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("No Surveys Available.").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No Surveys Available.").assertExists()

        // Check for failure scenario (Internal Server Error)
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

        // Prepare successful API Response
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

        // Wait for the survey details screen to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasTestTag("LoadingIndicator")).fetchSemanticsNodes().isNotEmpty()
        }

        // Que bad response for loading Survey Details
        mockWebServer.enqueue(mockErrorResponse)

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Error fetching survey: Internal Server Error", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Mock response for Survey Details API
        val mockSurveyResponse3 = MockResponse()
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
        mockWebServer.enqueue(mockSurveyResponse3)

        // Check "Enter Group Size" field
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Enter Group Size").fetchSemanticsNodes().isNotEmpty()
        }

        val groupSizeField = composeTestRule.onNodeWithText("Enter Group Size")
        groupSizeField.assertExists("Group Size input field not found!")

        // Failure Scenario: Click "Form Groups" button with no input
        composeTestRule.onNodeWithText("Form Groups").performClick()

        composeTestRule.onNodeWithText("Please enter a valid group size").assertExists("Error message not found!")

        // Enter a valid group size
        groupSizeField.performTextInput("3")

        // Mock response for group generation API
        val mockGroupResponse = MockResponse()
            .setResponseCode(200)
            .setBody(
                """
            {
                "groups": [
                    [1, 2, 3],
                    [4, 5, 6]
                ]
            }
        """.trimIndent()
            )
        mockWebServer.enqueue(mockGroupResponse)

        // Click "Form Groups"
        composeTestRule.onNodeWithText("Form Groups").performClick()

        // Wait for groups to form
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Generated Groups:").fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Verify if groups are displayed
        val groupsExist =
            composeTestRule.onAllNodesWithText("Generated Groups:").fetchSemanticsNodes()
                .isNotEmpty()

        assert(groupsExist ) { "No group message was found!" }

        // If groups exist, verify their size does not exceed 3
        if (groupsExist) {
            val groupNodes = composeTestRule.onAllNodesWithText("Group").fetchSemanticsNodes()

            for (groupNode in groupNodes) {
                val groupText =
                    groupNode.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text ?: ""

                // Extract the part after "Group X: "
                val membersText = groupText.substringAfter(":").trim()

                // Split by ", " to get the individual members
                val members = membersText.split(", ")

                assert(members.size <= 3) { "A group has more than 3 members! Found: ${members.size}" }
            }
        }

        mockWebServer.enqueue(mockErrorResponse)

        // Check if error message pops up
        composeTestRule.onNodeWithText("Form Groups").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Error forming groups: Internal Server Error").fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}

