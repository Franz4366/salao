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
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders

class SupabaseClient {

    private val _supabaseUrl = "https://kljubsnvkyeqbqyhxvfs.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtsanVic252a3llcWJxeWh4dmZzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDE3OTAxNjIsImV4cCI6MjA1NzM2NjE2Mn0.FAKh25wwfPBNfA_Ynqr4ZdElikIBUfHPnVb1hLAxy8Y"

    private val _client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        defaultRequest {
            header("apikey", supabaseKey)
            header(HttpHeaders.Authorization, "Bearer $supabaseKey")
        }
    }

    // Propriedade pública para acessar a URL do Supabase
    val supabaseUrl: String
        get() = _supabaseUrl

    // Propriedade pública para acessar o HttpClient
    val client: HttpClient
        get() = _client

    @Serializable
    data class NovoCliente(
        val nome: String,
        val telefone: String? = null,
        val email: String? = null,
        @SerialName("data_nascimento") val dataNascimento: String? = null
    )

    @Serializable
    data class Cliente(
        val id: Int,
        val nome: String,
        val telefone: String? = null,
        val email: String? = null,
        @SerialName("data_nascimento") val dataNascimento: String? = null
    )

    suspend fun buscarClientesPorNome(prefixo: String): List<Cliente> {
        val response: HttpResponse = client.get("$supabaseUrl/rest/v1/clientes") {
            parameter("select", "id, nome, telefone, email, data_nascimento")
            parameter("nome", "ilike.$prefixo%")
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            throw Exception("Erro ao buscar clientes: $errorText")
        }

        return response.body()
    }

    suspend fun cadastrarCliente(novoCliente: NovoCliente) {
        val response: HttpResponse = client.post("$supabaseUrl/rest/v1/clientes") {
            contentType(ContentType.Application.Json)
            setBody(novoCliente)
            header("Prefer", "return=minimal")
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            throw Exception("Erro ao cadastrar cliente: $errorText")
        }
    }

    suspend fun deletarCliente(clienteId: Int) {
        val response: HttpResponse = client.delete("$supabaseUrl/rest/v1/clientes") {
            parameter("id", "eq.$clienteId") // Filtra o cliente pelo ID
            header("Prefer", "return=minimal")
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            throw Exception("Erro ao deletar cliente: $errorText")
        }
    }
}