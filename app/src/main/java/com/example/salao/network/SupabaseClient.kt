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
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLBuilder
import com.example.salao.BuildConfig


class SupabaseClient {

    private val _supabaseUrl = BuildConfig.SUPABASE_URL
    private val _supabaseKey = BuildConfig.SUPABASE_ANON_KEY

    private val _client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        defaultRequest {
            header("apikey", _supabaseKey)
            header(HttpHeaders.Authorization, "Bearer $_supabaseKey")
            contentType(ContentType.Application.Json)
        }
    }

    val supabaseUrl: String
        get() = _supabaseUrl

    val supabaseKey: String
        get() = _supabaseKey

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
        @SerialName("profissional") val profissionalId: String,
        val comentario: String? = null
        // Adicione outras colunas conforme necessário
    )

    @Serializable
    data class UserProfile(
        val id: String,
        val nome: String? = null,
        @SerialName("photo_url") val photo_url: String? = null,
        val cargo: String? = null // Adicione cargo se for usar na consulta de profissionais
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

    suspend fun getProfileById(profileId: String): UserProfile? {
        return try {
            Log.d(
                "SupabaseClient",
                "Buscando perfil do profissional no Supabase com ID (UUID): $profileId"
            )
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/profiles") {
                parameter("select", "id, nome, photo_url, cargo") // Inclua cargo se for usar na resposta
                parameter("id", "eq.$profileId")
                header("Accept", "application/json")
            }
            Log.d("SupabaseData", "Raw Supabase Response (Status): ${response.status}") // Imprima a resposta bruta


            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response: $rawBody")
                val profiles: List<UserProfile> = Json.decodeFromString(rawBody)
                return profiles.firstOrNull()
            } else {
                Log.e(
                    "SupabaseClient",
                    "Erro ao buscar perfil do profissional: ${response.status} - ${response.bodyAsText()}"
                )
                null
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar perfil do profissional: ${e.message}")
            null
        }
    }


    suspend fun getAgendamentosPorData(data: String): List<AgendamentoSupabase> {
        return try {
            Log.d("SupabaseClient", "Buscando agendamentos no Supabase para a data: $data")
            val response: HttpResponse =
                client.get("$supabaseUrl/rest/v1/agendamentos") {
                    parameter("select", "id, cliente_id, data, hora, profissional, comentario")
                    parameter("data", "eq.$data")
                }
            Log.d("SupabaseClient", "Resposta do Supabase (status): ${response.status}")

            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response: $rawBody")
                return Json.decodeFromString<List<AgendamentoSupabase>>(rawBody)
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

    suspend fun getProfissionais(): List<com.example.salao.Agendamento.Profile> {
        return try {
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/profiles") {

                parameter("select", "id,nome,cargo,photo_url")
                parameter("cargo", "not.eq.null")
            }
            val rawBody = response.bodyAsText()
            Log.d("SupabaseClient", "Raw JSON Response (Profissionais): $rawBody")

            return Json.decodeFromString<List<com.example.salao.Agendamento.Profile>>(rawBody)
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao obter profissionais: ${e.message}")
            return emptyList()
        }
    }

    suspend fun criarAgendamento(
        clienteId: Int,
        dataAgendamento: String,
        horaAgendamento: String,
        profissionalId: String,
        comentario: String? = null
    ): Boolean {
        return try {
            val agendamento = AgendamentoSupabase(
                clienteId = clienteId,
                dataAgendamento = dataAgendamento,
                horaAgendamento = horaAgendamento,
                profissionalId = profissionalId,
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
                Log.d("SupabaseClient", "Raw JSON Response (Cliente por ID): $rawBody")
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
            val url = URLBuilder("$supabaseUrl/rest/v1/agendamentos") // Agora URLBuilder será reconhecido
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