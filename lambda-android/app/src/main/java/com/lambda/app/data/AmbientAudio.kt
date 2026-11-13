package com.lambda.app.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Offline ambient noise generator. Audio is generated on-device and never stored or transmitted. */
object AmbientAudio {
    @Volatile private var running = false
    @Volatile private var track: AudioTrack? = null
    @Volatile private var worker: Thread? = null
    @Volatile private var volume: Float = .35f

    @Synchronized fun start(mode: String, level: Float) {
        stop()
        if (mode == "off") return
        volume = level.coerceIn(0f, 1f)
        val sampleRate = 22050
        val channelMask = if (mode == "binaural") AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val min = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
        val audio = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(channelMask).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(min * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = audio
        running = true
        audio.play()
        worker = Thread {
            val frames = 2048
            val out = ShortArray(if (mode == "binaural") frames * 2 else frames)
            var brown = 0.0
            var p0=0.0; var p1=0.0; var p2=0.0; var p3=0.0; var p4=0.0; var p5=0.0; var p6=0.0
            var phaseL=0.0; var phaseR=0.0
            while (running && !Thread.currentThread().isInterrupted) {
                for (i in 0 until frames) {
                    val sample = when (mode) {
                        "pink" -> {
                            val w=Random.nextDouble(-1.0,1.0)
                            p0=.99886*p0+w*.0555179; p1=.99332*p1+w*.0750759; p2=.96900*p2+w*.1538520
                            p3=.86650*p3+w*.3104856; p4=.55*p4+w*.5329522; p5=-.7616*p5-w*.0168980
                            val v=(p0+p1+p2+p3+p4+p5+p6+w*.5362)*.10; p6=w*.115926; v
                        }
                        "binaural" -> 0.0
                        else -> { brown=(brown+.02*Random.nextDouble(-1.0,1.0))/1.02; brown*3.0 }
                    }
                    if (mode == "binaural") {
                        val left=sin(phaseL)*.13; val right=sin(phaseR)*.13
                        phaseL += 2.0*PI*180.0/sampleRate; phaseR += 2.0*PI*184.0/sampleRate
                        out[i*2]=(left*volume*Short.MAX_VALUE).toInt().coerceIn(-32768,32767).toShort()
                        out[i*2+1]=(right*volume*Short.MAX_VALUE).toInt().coerceIn(-32768,32767).toShort()
                    } else out[i]=(sample*volume*.20*Short.MAX_VALUE).toInt().coerceIn(-32768,32767).toShort()
                }
                try { audio.write(out, 0, out.size) } catch (_: Exception) { running=false }
            }
        }.apply { name="LambdaAmbientAudio"; isDaemon=true; start() }
    }

    fun setVolume(level: Float) { volume = level.coerceIn(0f,1f) }

    @Synchronized fun stop() {
        running = false
        worker?.interrupt(); worker = null
        track?.let { runCatching { it.pause(); it.flush(); it.stop(); it.release() } }
        track = null
    }
}
