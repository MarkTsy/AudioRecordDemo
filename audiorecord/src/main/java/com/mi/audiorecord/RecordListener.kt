package com.mi.audiorecord

import java.lang.Exception

/**
 * <pre>
 *     author : tao
 *     time   : 2022/03/01
 * </pre>
 */
interface RecordListener {
    fun onStartRecord()
    fun onRecording(bytes: ByteArray)
    fun onFinishRecord(bytes: ByteArray)
    fun onRecordError(ex : Exception)
}