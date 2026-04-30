package com.j41k.fridamanager.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.j41k.fridamanager.data.FridaShell

class FridaService : Service() {

    private val fridaShell = FridaShell()
    private var isServerRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val serverPath = intent?.getStringExtra("SERVER_PATH")
        val port = intent?.getStringExtra("PORT") ?: "27042"
        val address = intent?.getStringExtra("ADDRESS") ?: "0.0.0.0"

        when (action) {
            "START" -> {
                if (!isServerRunning && serverPath != null) {
                    startForegroundService()
                    launchFridaServer(serverPath, address, port)
                }
            }
            "STOP" -> {
                stopFridaServer()
            }
        }

        return START_NOT_STICKY
    }

    private fun launchFridaServer(path: String, address: String, port: String) {
        Thread {
            isServerRunning = true
            // Primero matamos cualquier instancia previa por seguridad
            fridaShell.stopFrida()
            // Ejecutamos en segundo plano real
            fridaShell.runCommand("nohup $path -l $address:$port > /dev/null 2>&1 &")
        }.start()
    }

    private fun stopFridaServer() {
        isServerRunning = false
        fridaShell.stopFrida()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (isServerRunning) {
            fridaShell.stopFrida()
        }
        super.onDestroy()
    }

    private fun startForegroundService() {
        val channelId = "frida_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, "Frida Server", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Frida Server")
            .setContentText("El servidor de Frida está corriendo...")
            .setSmallIcon(android.R.drawable.stat_sys_warning) // Cambia luego por tu icono
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}