package com.example.salao.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Modelos de Autenticação (de LoginResponse.kt)
@Serializable
data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String,
    val user: SupabaseUser
)

@Serializable
data class SupabaseUser(
    val id: String,
    val email: String
)

// Modelos de Cliente (de SupabaseClient.kt)
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

// Modelo de Agendamento (de SupabaseClient.kt)
@Serializable
data class AgendamentoSupabase(
    @SerialName("id") val id: Int? = null,
    @SerialName("cliente_id") val clienteId: Int,
    @SerialName("data") val dataAgendamento: String,
    @SerialName("hora") val horaAgendamento: String,
    @SerialName("profissional") val profissionalId: String,
    val comentario: String? = null
)

// Modelo de Perfil (Unificado - vamos usar este no lugar de UserProfile e Profile)
@Serializable
data class Profile(
    val id: String,
    @SerialName("nome") val nome: String,
    @SerialName("cargo") val cargo: String? = null,
    @SerialName("photo_url") val fotoUrl: String? = null
)