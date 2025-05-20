package com.example.salao.network

import android.util.Log
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
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders

class SupabaseClient {

    private val _supabaseUrl = "https://kljubsnvkyeqbqyhxvfs.supabase.co"
    private val supabaseKey =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtsanVic252a3llcWJxeWh4dmZzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDE3OTAxNjIsImV4cCI6MjA1NzM2NjE2Mn0.FAKh25wwfPBNfA_Ynqr4ZdElikIBUfHPnVb1hLAxy8Y"
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

    @Serializable
    data class AgendamentoSupabase(
        @SerialName("id") val id: Int? = null,
        @SerialName("cliente_id") val clienteId: Int,
        @SerialName("data") val dataAgendamento: String,
        @SerialName("hora") val horaAgendamento: String,
        @SerialName("profissional") val profissionalNome: String,
        val comentario: String? = null
        // Adicione outras colunas conforme necessário
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

    suspend fun getAgendamentosPorData(data: String): List<AgendamentoSupabase> {
        return try {
            Log.d("SupabaseClient", "Buscando agendamentos no Supabase para a data: $data")
            val response: HttpResponse =
                client.get("$supabaseUrl/rest/v1/agendamentos") {
                    parameter("select", "id, cliente_id, data, hora, profissional, comentario") // Include id in select
                    parameter("data", "eq.$data")
                }
            Log.d("SupabaseClient", "Resposta do Supabase (status): ${response.status}")

            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response: $rawBody")
                return Json.decodeFromString(rawBody)
            } else {
                Log.e(
                    "SupabaseClient",
                    "Erro ao buscar agendamentos por data: ${response.status} - ${response.bodyAsText()}"
                )
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar agendamentos por data: ${e.message}")
            emptyList()
        }
    }


    suspend fun getClientePorNome(nome: String): Cliente? {
        return try {
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/clientes") {
                parameter("select", "id, nome, telefone, email, data_nascimento")
                parameter("nome", "eq.$nome")
                parameter("limit", 1)
            }

            if (response.status.value in 200..299) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response: $rawBody")
                val clientes = Json.decodeFromString<List<Cliente>>(rawBody)
                return clientes.firstOrNull()
            } else {
                Log.e("SupabaseClient", "Erro ao buscar cliente por nome: ${response.status} - ${response.bodyAsText()}")
                return null
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar cliente por nome: ${e.message}")
            return null
        }
    }

    suspend fun cadastrarCliente(novoCliente: NovoCliente) {
        val response: HttpResponse = client.post("$supabaseUrl/rest/v1/clientes") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(novoCliente))
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

    suspend fun getProfissionais(): List<com.example.salao.Agendamento.Profile> { // Use o caminho completo
        return try {
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/profiles") {
                headers {
                    append("apikey", supabaseKey)
                    append("Authorization", "Bearer $supabaseKey")
                }
                parameter("select", "nome,cargo,photo_url")
                parameter("cargo", "not.eq.null")
            }
            val rawBody = response.bodyAsText() // Get raw body
            Log.d("SupabaseClient", "Raw JSON Response: $rawBody")
            return Json.decodeFromString(rawBody)
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao obter profissionais: ${e.message}")
            return emptyList()
        }
    }

    suspend fun criarAgendamento(
        clienteId: Int,
        dataAgendamento: String,
        horaAgendamento: String,
        profissionalNome: String,
        comentario: String? = null
    ): Boolean {
        return try {
            val agendamento = AgendamentoSupabase(
                clienteId = clienteId,
                dataAgendamento = dataAgendamento,
                horaAgendamento = horaAgendamento,
                profissionalNome = profissionalNome,
                comentario = comentario
            )
            val response: HttpResponse = client.post("$supabaseUrl/rest/v1/agendamentos") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(agendamento))
                header("Prefer", "return=minimal")
            }

            response.status.value in 200..299
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao criar agendamento: ${e.message}")
            return false
        }
    }

    suspend fun getClientePorId(clienteId: Int): Cliente? {
        return try {
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/clientes") {
                parameter("select", "id, nome")
                parameter("id", "eq.$clienteId")
                parameter("limit", 1)
            }
            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response: $rawBody")
                val clientes = Json.decodeFromString<List<Cliente>>(rawBody)
                return clientes.firstOrNull()
            } else {
                Log.e("SupabaseClient", "Erro ao buscar cliente por ID: ${response.status} - ${response.bodyAsText()}")
                return null
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar cliente por ID: ${e.message}")
            return null
        }
    }

    suspend fun deletarAgendamentos(ids: List<Int>): Boolean {
        return try {
            val url = URLBuilder("$supabaseUrl/rest/v1/agendamentos")
            ids.forEach { id ->
                url.parameters.append("id", "eq.$id")
            }
            val urlFinal = url.build()
            Log.d("deletarAgendamentos", "URL da requisição DELETE: $urlFinal")
            val response = client.delete(urlFinal) {
                contentType(ContentType.Application.Json)
            }
            Log.d("deletarAgendamentos", "Resposta da exclusão (status): ${response.status}")
            val responseBody = response.bodyAsText()
            Log.d("SupabaseClient", "Resposta da exclusão (body): $responseBody") // Log the response body
            return response.status.isSuccess()
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao deletar agendamentos: ${e.message}")
            return false
        }
    }
}

