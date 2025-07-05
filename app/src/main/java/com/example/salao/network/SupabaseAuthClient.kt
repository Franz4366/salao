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
import com.example.salao.BuildConfig
import android.util.Log
import kotlin.math.log

class SupabaseAuthClient {

    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

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

    suspend fun recoverPassword(email: String): String {
        val response: HttpResponse = client.post("$supabaseUrl/auth/v1/recover") {
            contentType(ContentType.Application.Json)
            headers {
                append("apikey", supabaseKey)
            }
            setBody(mapOf("email" to email))
        }
        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            throw Exception("Erro ao recuperar senha: $errorText")
        }
        return response.bodyAsText()
    }
}