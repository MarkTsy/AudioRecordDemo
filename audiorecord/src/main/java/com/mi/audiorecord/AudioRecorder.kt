package com.mi.audiorecord

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * <pre>
 *     author : tao
 *     time   : 2022/03/01
 * </pre>
 */
class AudioRecorder private constructor(var builder : Builder): Record {

    private val tag : String = javaClass.simpleName

    private var mRecording = false

    private var recorderData : ByteArrayOutputStream? = null

    private var audioRecorder : AudioRecord? = null

    @SuppressLint("MissingPermission")
    private fun createAudioRecord() : AudioRecord {

        var mBufferSize = AudioRecord.getMinBufferSize(
            builder.sampleRateInHz,
            builder.channelConfig,
            builder.audioFormat
        )

        var audioRecorder =  AudioRecord(builder.audioSoruce, builder.sampleRateInHz, builder.channelConfig, builder.audioFormat, mBufferSize)

        if (audioRecorder.state != AudioRecord.STATE_INITIALIZED) {
            audioRecorder.release()
            throw IOException("Create AudioRecorder Error!!!")
        }

        return audioRecorder
    }


    override fun startRecord() {
        if(mRecording) return

        mRecording = true

        builder.listener?.onStartRecord()

        Thread {

            audioRecorder = createAudioRecord()

            audioRecorder!!.startRecording()

            if (audioRecorder!!.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IOException("Unexpected Recording State ${audioRecorder!!.state}")
            }

            recorderData = ByteArrayOutputStream()

            try {
                while(mRecording) {

                    val buffer = ByteArray(512)
                    var ret = audioRecorder!!.read(buffer, 0, buffer.size)

                    when(ret) {
                        AudioRecord.ERROR_INVALID_OPERATION -> throw IOException("AudioRecord.read returned: ERROR_INVALID_OPERATION")
                        AudioRecord.ERROR_BAD_VALUE -> throw IOException("AudioRecord.read returned: ERROR_BAD_VALUE")
                    }

                    if (ret > 0) {
                        recorderData!!.write(buffer, 0, ret)
                        builder.listener?.onRecording(buffer)
                    }

                }
            } finally {
                recorderData!!.close()
            }

        }.start()
    }

    override fun stopRecord() {
        if(!mRecording) return

        mRecording = false

        audioRecorder?.release()

        builder.encoder?.let {
            builder.listener?.onFinishRecord(it.encode(recorderData?.toByteArray()))
        }
    }

    class Builder {

        companion object {
            val SAMPLE_RATE_IN_HZ_44100 = 44100
            val SAMPLE_RATE_IN_HZ_22050 = 22050
            val SAMPLE_RATE_IN_HZ_16000 = 16000
            val SAMPLE_RATE_IN_HZ_11025 = 11025
        }

        var channelConfig = AudioFormat.CHANNEL_IN_MONO
        var audioFormat = AudioFormat.ENCODING_PCM_16BIT
        var audioSoruce = AudioSource.VOICE_RECOGNITION
        var sampleRateInHz = SAMPLE_RATE_IN_HZ_44100
        var listener : RecordListener? = null
        var encoder : Encoder? = null

        fun build() : AudioRecorder {
            return AudioRecorder(this)
        }

        fun setChannelConfig(channelConfig: Int) : Builder {
            this.channelConfig = channelConfig
            return this
        }

        fun setAudioFormat(audioFormat: Int) : Builder {
            this.audioFormat = audioFormat
            return this
        }

        fun setAudioSoruce(audioSoruce: Int) : Builder {
            this.audioSoruce = audioSoruce
            return this
        }

        fun setSampleRateInHz(sampleRateInHz: Int) : Builder {
            this.sampleRateInHz = sampleRateInHz
            return this
        }

        fun setRecordListener(listener : RecordListener) : Builder {
            this.listener = listener
            return this
        }

        fun setEncoder(encoder: Encoder) : Builder {
            this.encoder = encoder
            return this
        }

    }

}