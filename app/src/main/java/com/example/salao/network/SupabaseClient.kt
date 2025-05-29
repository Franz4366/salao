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

// IMPORTS DOS SEUS MODELOS UNIFICADOS
import com.example.salao.model.NovoCliente
import com.example.salao.model.Cliente
import com.example.salao.model.AgendamentoSupabase
import com.example.salao.model.Profile // Agora importando do pacote model!

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

    // REMOVIDAS AS DEFINIÇÕES DAS DATA CLASSES QUE ESTÃO AGORA EM SupabaseModels.kt

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

    // Usando a classe Profile unificada
    suspend fun getProfileById(profileId: String): Profile? { // Agora retorna Profile do model
        return try {
            Log.d("SupabaseClient", "Buscando perfil do profissional no Supabase com ID (UUID): $profileId")
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/profiles") {
                parameter("select", "id, nome, photo_url, cargo")
                parameter("id", "eq.$profileId")
                header("Accept", "application/json")
            }
            Log.d("SupabaseData", "Raw Supabase Response (Status): ${response.status}")

            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response (Profile by ID): $rawBody")
                // Use List<Profile> para desserializar, pois a API pode retornar uma lista
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

    suspend fun getAgendamentosPorData(data: String, profissionalId: String? = null): List<AgendamentoSupabase> {
        return try {
            Log.d("SupabaseClient", "Buscando agendamentos no Supabase para a data: $data e profissional: ${profissionalId ?: "todos"}")
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/agendamentos") {
                parameter("select", "id, cliente_id, data, hora, profissional, comentario")
                parameter("data", "eq.$data") // Filtra pela data

                if (profissionalId != null) {
                    parameter("profissional", "eq.$profissionalId") // Filtra pelo ID do profissional
                }
            }
            Log.d("SupabaseClient", "Resposta do Supabase (status): ${response.status}")

            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                Log.d("SupabaseClient", "Raw JSON Response (Agendamentos): $rawBody")
                return Json.decodeFromString<List<AgendamentoSupabase>>(rawBody)
            } else {
                Log.e("SupabaseClient", "Erro ao buscar agendamentos por data e/ou profissional: ${response.status} - ${response.bodyAsText()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao buscar agendamentos por data e/ou profissional: ${e.message}")
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

    // getProfissionais usando a classe Profile unificada
    suspend fun getProfissionais(): List<Profile> { // Agora retorna List<Profile> do model
        return try {
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/profiles") {
                parameter("select", "id,nome,cargo,photo_url")
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
        clienteId: Int,
        dataAgendamento: String,
        horaAgendamento: String,
        profissionalId: String, // ID do profissional (UUID)
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
            Log.d("SupabaseClient", "Resposta da exclusão (body): $responseBody")
            return response.status.isSuccess()
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Erro ao deletar agendamentos: ${e.message}")
            return false
        }
    }
}