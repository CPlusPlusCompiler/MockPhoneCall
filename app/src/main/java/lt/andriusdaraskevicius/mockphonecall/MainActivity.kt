package lt.andriusdaraskevicius.mockphonecall

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.widget.ImageButton
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var audioManager: AudioManager
    private lateinit var callIntent: Intent

    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null

    private lateinit var powerManager: PowerManager
    private var wakeLock: WakeLock? = null

    private lateinit var clContainer: ConstraintLayout

    private val broadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action == "broadcast_audio_ended")
                finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ongoing_call)

        val intentFilter = IntentFilter().apply {
            addAction("broadcast_audio_ended")
        }

        registerReceiver(broadcastReceiver, intentFilter)

        clContainer = findViewById(R.id.clContainer)

        setupAudioService()

        setupSensor()

        setupButtons()
    }


    private fun setupButtons() {
        val btnSpeaker = findViewById<ImageButton>(R.id.btnSpeaker)
        btnSpeaker.setOnClickListener {
            toggleSpeaker()
        }

        val btnEndCall = findViewById<MaterialButton>(R.id.btnEndCall)
        btnEndCall.setOnClickListener {
            finish()
        }

        val btnMute = findViewById<ImageButton>(R.id.btnMute)
        btnMute.setOnClickListener {
            showSnackbar(R.string.not_implemented)
        }

        val btnBluetooth = findViewById<ImageButton>(R.id.btnBluetooth)
        btnBluetooth.setOnClickListener {
            showSnackbar(R.string.not_implemented)
        }

        val btnVideoCall = findViewById<ImageButton>(R.id.btnVideoCall)
        btnVideoCall.setOnClickListener {
            showSnackbar(R.string.not_implemented)
        }

        val btnKeypad = findViewById<ImageButton>(R.id.btnKeypad)
        btnKeypad.setOnClickListener {
            showSnackbar(R.string.not_implemented)
        }
    }


    private fun setupSensor() {
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY, true)
    }


    private fun setupAudioService() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false

        callIntent = Intent(this, MockCallAudioService::class.java)
        startService(callIntent)
    }


    private fun showSnackbar(@StringRes message: Int) {
        Snackbar.make(clContainer, message, Snackbar.LENGTH_SHORT).show()
    }


    private fun toggleSpeaker() {
        audioManager.isSpeakerphoneOn = !audioManager.isSpeakerphoneOn
    }


    private fun turnOnScreen() {
        // turn on screen
        wakeLock?.release()
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "myapp:mywakelocktag")
        wakeLock?.setReferenceCounted(false)

        if(!wakeLock!!.isHeld)
            wakeLock?.acquire(10*60*1000L /*10 minutes*/)
    }


    @TargetApi(21) //Suppress lint error for PROXIMITY_SCREEN_OFF_WAKE_LOCK
    private fun turnOffScreen() {
        // turn off screen
        wakeLock?.release()
        wakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "myapp:mywakelocktag")
        wakeLock?.setReferenceCounted(false)
        if(!wakeLock!!.isHeld)
            wakeLock?.acquire(10*60*1000L /*10 minutes*/)
    }


    override fun onSensorChanged(event: SensorEvent?) {
        val proximity = event?.values?.firstOrNull()
        proximity?.let {
            if(it > 0)
                turnOnScreen()
            else
                turnOffScreen()

            Log.d(applicationInfo.name, it.toString())
        }

    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }


    override fun onResume() {
        super.onResume()
        proximitySensor?.let {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }


    override fun onPause() {
        super.onPause()
        proximitySensor?.let {
            sensorManager.unregisterListener(this)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopService(callIntent)
        audioManager.isSpeakerphoneOn = true
        unregisterReceiver(broadcastReceiver)
        wakeLock?.release()
        wakeLock = null
    }
}