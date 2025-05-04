package com.example.salao.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SupabaseAuthClient {

    private val supabaseUrl = "https://kljubsnvkyeqbqyhxvfs.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtsanVic252a3llcWJxeWh4dmZzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDE3OTAxNjIsImV4cCI6MjA1NzM2NjE2Mn0.FAKh25wwfPBNfA_Ynqr4ZdElikIBUfHPnVb1hLAxy8Y"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val response: HttpResponse = client.post("$supabaseUrl/auth/v1/token?grant_type=password") {
            contentType(ContentType.Application.Json)
            headers {
                append("apikey", supabaseKey)
            }
            setBody(AuthRequest(email, password))
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            throw Exception("Erro no login: $errorText")
        }

        return response.body()
    }

    @Serializable
    data class AuthRequest(val email: String, val password: String)

    @Serializable
    data class AuthResponse(
        val access_token: String,
        val token_type: String,
        val expires_in: Int,
        val refresh_token: String,
        val user: User
    )

    @Serializable
    data class User(
        val id: String,
        val email: String? = null,
        @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null
    )
}