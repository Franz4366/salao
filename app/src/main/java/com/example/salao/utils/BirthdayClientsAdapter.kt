// com/example/salao/utils/BirthdayClientsAdapter.kt

package com.example.salao.utils // Cuidado com o pacote, você tinha salaao na última vez, mas aqui está "salao"

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView // Adicione este import
import androidx.recyclerview.widget.RecyclerView
import com.example.salao.R
import com.example.salao.model.Cliente
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class BirthdayClientsAdapter(private var clients: List<Cliente>) : // MUDE DE 'val' PARA 'var' AQUI!
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
        val birthdayGirlImage: ImageView = itemView.findViewById(R.id.birthday_girl_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirthdayClientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_birthday_card, parent, false)
        return BirthdayClientViewHolder(view)
    }

    override fun onBindViewHolder(holder: BirthdayClientViewHolder, position: Int) {
        if (clients.isEmpty()) {
            // Caso 1: Não há aniversariantes. Exibe a mensagem e oculta outros elementos.
            holder.clientName.text = "Não há aniversariantes hoje."
            holder.clientName.textSize = 18f
            holder.clientName.gravity = View.TEXT_ALIGNMENT_CENTER

            holder.sendMessageButton.visibility = View.GONE
            holder.birthdayGirlImage.visibility = View.GONE

            val noBirthdayColor = ContextCompat.getColor(holder.itemView.context, R.color.card_color_4)
            holder.cardView.setCardBackgroundColor(noBirthdayColor)

        } else {
            // Caso 2: Há aniversariantes. Exibe os dados normais do cliente.
            val client = clients[position]
            holder.clientName.text = "Hoje é o aniversário de ${client.nome}!"
            holder.clientName.textSize = 18f
            holder.clientName.gravity = View.TEXT_ALIGNMENT_VIEW_START

            holder.sendMessageButton.visibility = View.VISIBLE
            holder.birthdayGirlImage.visibility = View.VISIBLE

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
                            return@setOnClickListener
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
    }

    override fun getItemCount(): Int {
        // Se a lista de clientes estiver vazia, retornamos 1 para que um único cartão seja exibido
        // com a mensagem "Não há aniversariantes hoje."
        return if (clients.isEmpty()) 1 else clients.size
    }

    // ADICIONE ESTA FUNÇÃO!
    fun updateClients(newClients: List<Cliente>) {
        this.clients = newClients
        notifyDataSetChanged() // Notifica o RecyclerView que os dados mudaram
    }
}