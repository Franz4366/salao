package com.example.salao

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp // Import necessário para FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("FirebaseInit", "Tentando inicializar FirebaseApp...")
        try {
            FirebaseApp.initializeApp(this)
            Log.d("FirebaseInit", "FirebaseApp inicializado com sucesso.")
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Erro ao inicializar FirebaseApp: ${e.message}", e)
        }
    }
}