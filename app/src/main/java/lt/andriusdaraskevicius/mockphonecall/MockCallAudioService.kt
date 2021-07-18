package lt.andriusdaraskevicius.mockphonecall

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import android.provider.MediaStore
import androidx.annotation.RequiresApi

class MockCallAudioService: Service() {

    private lateinit var mediaPlayer: MediaPlayer

    companion object {
        const val CALL_NOTIFICATION_ID = 1337
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onCreate() {
        super.onCreate()

        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.background_music).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel("service_call", "Call service")
        else
            ""

        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
                this, 1, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, channelId)
        else
            Notification.Builder(this)

        val notification = builder
            .setContentText(getString(R.string.ongoing_call))
            .setSubText(getString(R.string.tap_to_return))
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_call_end)
            .build()

        startForeground(CALL_NOTIFICATION_ID, notification)

        startAudio()
        return super.onStartCommand(intent, flags, startId)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }


    private fun startAudio() {
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            val intent = Intent().apply {
                action = "broadcast_audio_ended"
            }
            sendBroadcast(intent)

            stopForeground(true)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.reset()
        mediaPlayer.release()
    }
}