package com.example.salao.network

import android.util.Log
import com.example.salao.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

import com.example.salao.model.NovoCliente
import com.example.salao.model.Cliente
import com.example.salao.model.AgendamentoSupabase
import com.example.salao.model.Profile
import com.example.salao.model.ProfileUpdate
import io.ktor.client.request.patch
import io.ktor.client.request.put

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
            contentType(ContentType.Application.Json)
        }
    }

    val supabaseUrl: String
        get() = _supabaseUrl

    val supabaseKey: String
        get() = _supabaseKey

    val client: HttpClient
        get() = _client

    suspend fun buscarClientesPorNome(prefixo: String, userJwtToken: String): List<Cliente> {
        val response: HttpResponse = client.get("$supabaseUrl/rest/v1/clientes") {
            parameter("select", "id, nome, telefone, email, data_nascimento")
            parameter("nome", "ilike.$prefixo%")
            header("Authorization", "Bearer $userJwtToken")
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            throw Exception("Erro ao buscar clientes: $errorText")
        }

        return response.body()
    }

    suspend fun getProfileById(profileId: String, userJwtToken: String): Profile? {
        return try {
            Log.d("SupabaseClient", "Buscando perfil do profissional no Supabase com ID (UUID): $profileId")
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/profiles") {
                parameter("select", "id, nome, telefone, email, photo_url, cargo")
                parameter("id", "eq.$profileId")
                header("Accept", "application/json")
                header("Authorization", "Bearer $userJwtToken")
            }
            Log.d("SupabaseData", "Raw Supabase Response (Status): ${response.status}")

            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response (Profile by ID): $rawBody")
                val profiles: List<Profile> = Json.decodeFromString(rawBody)
                return profiles.firstOrNull()
            } else {
                Log.e("SupabaseClient", "Erro ao buscar perfil do profissional: ${response.status} - ${response.bodyAsText()}")
                null
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar perfil do profissional: ${e.message}")
            null
        }
    }

    suspend fun updateProfile(userId: String, profileUpdate: ProfileUpdate, userJwtToken: String): Boolean {
        return try {
            val jsonBody = Json.encodeToString(profileUpdate)
            Log.d("SupabaseClient", "JSON enviado para atualização de perfil: $jsonBody")
            Log.d("SupabaseClient", "Token JWT enviado para updateProfile: $userJwtToken")

            val response: HttpResponse = client.patch("$supabaseUrl/rest/v1/profiles") {
                parameter("id", "eq.$userId")
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
                header("Authorization", "Bearer $userJwtToken")
                header("Prefer", "return=representation")
            }
            Log.d("SupabaseClient", "Resposta ao atualizar perfil (status): ${response.status}")
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                Log.e("SupabaseClient", "Erro detalhado ao atualizar perfil: $errorBody")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao atualizar perfil: ${e.message}", e)
            false
        }
    }

    suspend fun getAgendamentosPorData(data: String, profissionalId: String? = null, userJwtToken: String): List<AgendamentoSupabase> {
        return try {
            Log.d("SupabaseClient", "Buscando agendamentos no Supabase para a data: $data e profissional: ${profissionalId ?: "todos"}")
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/agendamentos") {
                parameter("select", "id, cliente_id, data, hora, profissional, comentario")
                parameter("data", "eq.$data")

                if (profissionalId != null) {
                    parameter("profissional", "eq.$profissionalId")
                }
                header("Authorization", "Bearer $userJwtToken")
            }
            Log.d("SupabaseClient", "Resposta do Supabase (status): ${response.status}")

            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response (Agendamentos): $rawBody")
                return Json.decodeFromString<List<AgendamentoSupabase>>(rawBody)
            } else {
                Log.e("SupabaseClient", "Erro ao buscar agendamentos por data: ${response.status} - ${response.bodyAsText()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar agendamentos por data: ${e.message}")
            emptyList()
        }
    }

    suspend fun getClientePorNome(nome: String, userJwtToken: String): Cliente? {
        return try {
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/clientes") {
                parameter("select", "id, nome, telefone, email, data_nascimento")
                parameter("nome", "eq.$nome")
                parameter("limit", 1)
                header("Authorization", "Bearer $userJwtToken")
            }

            if (response.status.value in 200..299) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response (Cliente por Nome): $rawBody")
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

    suspend fun cadastrarCliente(novoCliente: NovoCliente, userJwtToken: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$supabaseUrl/rest/v1/clientes") {
                header("Authorization", "Bearer $userJwtToken")
                contentType(ContentType.Application.Json)
                setBody(novoCliente)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao cadastrar cliente: ${e.message}")
            false
        }
    }

    suspend fun deletarCliente(clienteId: String, userJwtToken: String): Boolean {
        return try {
            val response: HttpResponse = client.delete("$supabaseUrl/rest/v1/clientes") {
                parameter("id", "eq.$clienteId")
                header("Authorization", "Bearer $userJwtToken")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao deletar cliente: ${e.message}")
            false
        }
    }

    suspend fun getProfissionais(userJwtToken: String): List<Profile> {
        return try {
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/profiles") {
                parameter("select", "id,nome,cargo,photo_url")
                header("Authorization", "Bearer $userJwtToken")

            }
            val rawBody = response.bodyAsText()
            Log.d("SupabaseClient", "Raw JSON Response (Profissionais): $rawBody")

            return Json.decodeFromString<List<Profile>>(rawBody)
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao obter profissionais: ${e.message}")
            return emptyList()
        }
    }

    suspend fun criarAgendamento(
        clienteId: String,
        dataAgendamento: String,
        horaAgendamento: String,
        profissionalId: String,
        comentario: String? = null,
        userJwtToken: String
    ): Boolean {
        return try {
            val agendamento = AgendamentoSupabase(
                clienteId = clienteId.toString(),
                dataAgendamento = dataAgendamento,
                horaAgendamento = horaAgendamento,
                profissionalId = profissionalId,
                comentario = comentario
            )
            val response: HttpResponse = client.post("$supabaseUrl/rest/v1/agendamentos") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(agendamento))
                header("Prefer", "return=minimal")
                header("Authorization", "Bearer $userJwtToken") // <-- Adicionado o cabeçalho de autorização
            }
            Log.d("SupabaseClient", "Resposta ao criar agendamento (status): ${response.status}")
            if (!response.status.isSuccess()) {
                Log.e("SupabaseClient", "Erro detalhado ao criar agendamento: ${response.bodyAsText()}")
            }

            response.status.isSuccess()
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao criar agendamento: ${e.message}")
            return false
        }
    }

    suspend fun getClientePorId(clienteId: String, userJwtToken: String): Cliente? { // <-- AGORA REQUER userJwtToken
        return try {
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/clientes") {
                parameter("select", "id, nome")
                parameter("id", "eq.$clienteId")
                parameter("limit", 1)
                header("Authorization", "Bearer $userJwtToken") // <-- Adicionado o cabeçalho de autorização
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
            val url = URLBuilder("$supabaseUrl/rest/v1/agendamentos")
            ids.forEach { id ->
                url.parameters.append("id", "eq.$id")
            }
            val urlFinal = url.build()
            Log.d("deletarAgendamentos", "URL da requisição DELETE: $urlFinal")
            val response = client.delete(urlFinal) {
                contentType(ContentType.Application.Json)
               // header("Authorization", "Bearer $userJwtToken")
            }
            Log.d("deletarAgendamentos", "Resposta da exclusão (status): ${response.status}")
            val responseBody = response.bodyAsText()
            Log.d("SupabaseClient", "Resposta da exclusão (body): $responseBody")
            return response.status.isSuccess()
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao deletar agendamentos: ${e.message}")
            return false
        }
    }

    suspend fun getAllClientes(userJwtToken: String): List<Cliente> {
        return try {
            Log.d("SupabaseClient", "Buscando todos os clientes no Supabase...")
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/clientes") {
                parameter("select", "id, nome, telefone, email, data_nascimento")
                header("Authorization", "Bearer $userJwtToken")
            }
            Log.d("SupabaseClient", "Resposta ao buscar todos os clientes (status): ${response.status}")

            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response (All Clients): $rawBody")
                return Json.decodeFromString<List<Cliente>>(rawBody)
            } else {
                val errorBody = response.bodyAsText()
                Log.e("SupabaseClient", "Erro ao buscar todos os clientes: ${response.status.value} - $errorBody")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar todos os clientes: ${e.message}", e)
            emptyList()
        }
    }
}