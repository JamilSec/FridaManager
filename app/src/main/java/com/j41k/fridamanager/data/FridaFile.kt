package com.j41k.fridamanager.data

data class FridaFile(
    val name: String,
    val path: String,
    val isExecutable: Boolean = false
)