package com.example.salao.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

@Serializable
data class NovoCliente(
    val nome: String,
    val telefone: String? = null,
    val email: String? = null,
    @SerialName("data_nascimento") val dataNascimento: String? = null
)

@Serializable
data class Cliente(
    val id: String,
    val nome: String,
    val telefone: String? = null,
    val email: String? = null,
    @SerialName("data_nascimento") val dataNascimento: String? = null
)

@Serializable
data class AgendamentoSupabase(
    @SerialName("id") val id: Int? = null,
    @SerialName("cliente_id") val clienteId: String,
    @SerialName("data") val dataAgendamento: String,
    @SerialName("hora") val horaAgendamento: String,
    @SerialName("profissional") val profissionalId: String,
    val comentario: String? = null
)

@Serializable
data class Profile(
    val id: String,
    @SerialName("nome") val nome: String,
    @SerialName("cargo") val cargo: String? = null,
    @SerialName("photo_url") val fotoUrl: String? = null,
    @SerialName("telefone") val telefone: String? = null,
    @SerialName("email") val email: String? = null
)

@Serializable
data class ProfileUpdate(
    @SerialName("nome") val nome: String,
    @SerialName("telefone") val telefone: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("cargo") val cargo: String? = null,
    @SerialName("photo_url") val fotoUrl: String? = null

)

data class AgendamentoItem(
    val id: Int?,
    val clienteNome: String,
    val data: String,
    val hora: String,
    val profissionalNome: String,
    val comentario: String?
)