package com.example.salao.utils

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.salao.R
import com.example.salao.model.Cliente
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class BirthdayClientsAdapter(private val clients: List<Cliente>) :
    RecyclerView.Adapter<BirthdayClientsAdapter.BirthdayClientViewHolder>() {

    private val cardColors = arrayOf(
        R.color.card_color_1,
        R.color.card_color_2,
        R.color.card_color_3,
        R.color.card_color_4,
        R.color.card_color_5,
        R.color.card_color_6,
        R.color.card_color_7,
        R.color.card_color_8
    )

    inner class BirthdayClientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clientName: TextView = itemView.findViewById(R.id.birthday_client_name)
        val sendMessageButton: Button = itemView.findViewById(R.id.send_message_button)
        val cardView: CardView = itemView.findViewById(R.id.birthday_card_root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirthdayClientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_birthday_card, parent, false)
        return BirthdayClientViewHolder(view)
    }

    override fun onBindViewHolder(holder: BirthdayClientViewHolder, position: Int) {
        val client = clients[position]
        holder.clientName.text = "Hoje é o aniversário de ${client.nome}!"


        val colorResId = cardColors[position % cardColors.size]
        val color = ContextCompat.getColor(holder.itemView.context, colorResId)
        holder.cardView.setCardBackgroundColor(color)

        holder.sendMessageButton.setOnClickListener {
            client.telefone?.let { phoneNumber ->
                val cleanPhoneNumber = phoneNumber.replace("[^0-9]".toRegex(), "")
                val whatsappPhoneNumber = "55$cleanPhoneNumber"

                val defaultMessage = "Feliz aniversário, ${client.nome}! " +
                        "Desejamos um dia cheio de alegria e muito brilho! " +
                        "Esperamos vê-la(o) em breve no nosso salão para celebrar sua beleza. 🎉✨"

                val encodedMessage = Uri.encode(defaultMessage)
                val context = holder.itemView.context

                try {
                    var whatsappIntent = Intent(Intent.ACTION_VIEW)
                    whatsappIntent.data = Uri.parse("whatsapp://send?phone=$whatsappPhoneNumber&text=$encodedMessage")
                    whatsappIntent.setPackage("com.whatsapp")

                    if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(whatsappIntent)
                        return@setOnClickListener // Sucesso, sai do listener
                    }

                    whatsappIntent = Intent(Intent.ACTION_VIEW)
                    whatsappIntent.data = Uri.parse("whatsapp://send?phone=$whatsappPhoneNumber&text=$encodedMessage")
                    whatsappIntent.setPackage("com.whatsapp.w4b")

                    if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(whatsappIntent)
                        return@setOnClickListener
                    }

                    val webIntent = Intent(Intent.ACTION_VIEW)
                    webIntent.data = Uri.parse("https://api.whatsapp.com/send?phone=$whatsappPhoneNumber&text=$encodedMessage")

                    if (webIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(webIntent)
                        return@setOnClickListener
                    }

                    Toast.makeText(context, "WhatsApp não está instalado ou configurado corretamente.", Toast.LENGTH_LONG).show()

                } catch (e: Exception) {
                    Toast.makeText(context, "Erro inesperado ao tentar abrir o WhatsApp: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }

            } ?: run {
                Toast.makeText(holder.itemView.context, "Número de telefone não disponível para ${client.nome}.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return clients.size
    }
}