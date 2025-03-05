package com.example.groupformer

data class QuestionText(
    val questionText: String
)

data class Question(
    val id: String,
    val questionText: String
)

data class QuestionnaireRequest(
    val title: String,
    val description: String,
    val questions: List<QuestionText>
)

data class QuestionnaireResponse(
    val id: String,
    val title: String,
    val description: String,
    val questions: List<Question>,
    val createdAt: String
)

data class Questionnaire(
    val id: String,
    val title: String,
    val description: String,
    val createdAt: String,
)

data class Answer(
    val questionId: String,
    val answer: String,
)
