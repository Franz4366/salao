package com.example.salao.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.example.salao.BuildConfig
import com.example.salao.model.LoginResponse
import com.example.salao.model.AuthRequest
import io.ktor.client.plugins.HttpTimeout


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
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 15000
        }
    }

    suspend fun login(email: String, password: String): LoginResponse {
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

    suspend fun refreshSession(refreshToken: String): LoginResponse {
        val response: HttpResponse = client.post("$supabaseUrl/auth/v1/token?grant_type=refresh_token") {
            contentType(ContentType.Application.Json)
            headers {
                append("apikey", supabaseKey)
            }
            setBody(mapOf("refresh_token" to refreshToken))
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            throw Exception("Erro ao renovar sessão: $errorText")
        }

        return response.body<LoginResponse>()
    }

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