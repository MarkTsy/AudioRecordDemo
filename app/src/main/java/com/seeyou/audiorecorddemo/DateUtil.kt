package com.seeyou.audiorecorddemo

import java.text.SimpleDateFormat
import java.util.*

/**
 * <pre>
 *     author : tao
 *     time   : 2022/02/28
 * </pre>
 */
class DateUtil {


    companion object {
        val date : String
            get() {
                var simpleDateFormat = SimpleDateFormat("MM-dd-HH-mm-ss")
                return  simpleDateFormat.format(Date())
            }
    }
}