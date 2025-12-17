package com.easy.peasy

data class Question(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

data class Block(
    val id: Int,
    val title: String,
    val questions: List<Question>
)