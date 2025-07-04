package com.example.salao.network

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.salao.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.salao.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "DefaultChannel" // ID único para o canal de notificação
    }

    /**
     * Chamado quando um novo token de registro do FCM é gerado.
     * Este token identifica o dispositivo e é necessário para enviar notificações direcionadas.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // IMPORTANTE: Se você pretende enviar notificações para usuários específicos ou grupos,
        // você precisará enviar este 'token' para o seu servidor Supabase/backend.
        // Por exemplo, você pode associar este token ao ID do usuário no seu banco de dados Supabase.
        // Ex: sendRegistrationTokenToSupabase(token)
    }

    /**
     * Chamado quando uma mensagem do FCM é recebida.
     * As mensagens podem vir com um payload de dados, um payload de notificação, ou ambos.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // 1. Processar dados da mensagem (key-value pairs)
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            // Aqui você pode processar dados personalizados enviados na notificação.
            // Por exemplo, dados sobre um agendamento, um ID de cliente, etc.
            // Estes dados podem ser usados para navegar para uma tela específica ao clicar na notificação,
            // ou para atualizar a UI do app sem exibir uma notificação visual.
        }

        // 2. Exibir notificação visual (se presente)
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            Log.d(TAG, "Message Notification Title: ${it.title}")

            // Exibe a notificação para o usuário na barra de status
            sendNotification(it.title, it.body)
        }
    }

    /**
     * Cria e exibe a notificação visual na barra de status do dispositivo.
     */
    private fun sendNotification(title: String?, messageBody: String?) {
        // Intent que será disparada ao clicar na notificação.
        // Leva o usuário de volta para a MainActivity.
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Limpa a pilha de atividades
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE é obrigatório para API 23+
        )

        // Criar o Notification Channel (necessário para Android 8.0/API 26 e superior)
        createNotificationChannel()

        // Constrói a notificação visual
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true) // A notificação desaparece ao ser clicada
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Define a prioridade da notificação (influencia como é exibida)

        with(NotificationManagerCompat.from(this)) {
            // Verificar permissão de notificação para Android 13 (API 33) e superior
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        this@MyFirebaseMessagingService,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Se a permissão não foi concedida (no Android 13+), não podemos exibir a notificação.
                    // VOCÊ PRECISA SOLICITAR ESTA PERMISSÃO EM TEMPO DE EXECUÇÃO NA SUA ACTIVITY PRINCIPAL.
                    // Por simplicidade e para não bloquear o fluxo aqui, estamos apenas logando.
                    Log.w(TAG, "Permissão POST_NOTIFICATIONS não concedida. Não é possível exibir a notificação.")
                    return
                }
            }
            notify(0 /* ID da notificação. Use um ID único se tiver várias notificações. */, notificationBuilder.build())
        }
    }

    /**
     * Cria um Notification Channel.
     * Necessário para Android 8.0 (API 26) e superior para agrupar notificações e permitir que o usuário controle.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificações Gerais do Salão" // Nome visível para o usuário nas configurações do app
            val descriptionText = "Canal para notificações importantes do aplicativo Salão, como lembretes de agendamento."
            val importance = NotificationManager.IMPORTANCE_DEFAULT // Importância padrão
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Registre o canal no sistema.
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}