package com.mi.audiorecord;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <pre>
 *     author : tao
 *     time   : 2022/03/02
 * </pre>
 */
public class AccEncoder implements Encoder {

    private String encodeType = MediaFormat.MIMETYPE_AUDIO_AAC;
    //比特率
    private final static int KEY_BIT_RATE = 64000;
    //读取数据的最大字节数
    private final static int KEY_MAX_INPUT_SIZE = 1024 * 1024;

    private int sampleRate;
    private int channelCount;
    private MediaCodec mediaEncode;
    private ByteBuffer[] encodeInputBuffers;
    private ByteBuffer[] encodeOutputBuffers;
    private MediaCodec.BufferInfo encodeBufferInfo;

    private byte[] chunkAudio = new byte[0];

    public AccEncoder(int sampleRate, int channelCount) {
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        initAACMediaEncode();
    }

    /**
     * 初始化AAC编码器
     */
    private void initAACMediaEncode() {
        try {
            //参数对应-> mime type、采样率、声道数
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(encodeType, sampleRate, channelCount);
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE);
            encodeFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
            encodeFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
            encodeFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, KEY_MAX_INPUT_SIZE);

            mediaEncode = MediaCodec.createEncoderByType(encodeType);
            mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mediaEncode == null) {
            return;
        }

        mediaEncode.start();
        encodeInputBuffers = mediaEncode.getInputBuffers();
        encodeOutputBuffers = mediaEncode.getOutputBuffers();
        encodeBufferInfo = new MediaCodec.BufferInfo();
    }

    /**
     * 编码PCM数据 得到AAC格式的音频文件
     */
    @Override
    public byte[] encode(byte[] pcmData) throws IOException {

        int inputIndex;
        ByteBuffer inputBuffer;

        int outputIndex;
        ByteBuffer outputBuffer;

        int outBitSize;
        int outPacketSize;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        //获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
        inputIndex = mediaEncode.dequeueInputBuffer(-1);

        if (inputIndex >= 0) {
            inputBuffer = encodeInputBuffers[inputIndex];
            inputBuffer.clear();
            inputBuffer.limit(pcmData.length);
            inputBuffer.put(pcmData);

            mediaEncode.queueInputBuffer(inputIndex, 0, pcmData.length, 0, 0);
        }

        //获取可用的outputBuffer
        outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 0);

        Log.e("index", "outputIndex : " + outputIndex);

        while (outputIndex >= 0) {
            outBitSize = encodeBufferInfo.size;  //数据量
            outPacketSize = outBitSize + 7;     //数据量 + 头
            outputBuffer = encodeOutputBuffers[outputIndex];  //获取outputbuffer

            outputBuffer.position(encodeBufferInfo.offset);  //定位数据开始位置
            outputBuffer.limit(encodeBufferInfo.offset + outBitSize);  //显示数据长度

            //添加ADTS头
            byte[] outData = new byte[outPacketSize];   //保存数据数组
            addADTStoPacket(outData, outPacketSize);  //添加头


            outputBuffer.get(outData, 7, outBitSize);  //将编码得到的acc 放到数组中
            outputBuffer.position(encodeBufferInfo.offset);   //定位到数据开始位置

            baos.write(outData); //数据写入流中

            mediaEncode.releaseOutputBuffer(outputIndex, false); //重置
            outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 0); //接着获取可用outputBuffer 确保所有buffer都被消耗重置
        }

        byte[] out = baos.toByteArray();

        baos.flush();
        baos.reset();
        baos.close();

        return out;
    }

    /**
     * 添加ADTS头
     *
     * @param packet
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 8;  //8 --> 16K, 11 --> 8K, 4 --> 44.1KHz
        int chanCfg = 1;  //1 单声道 2 立体声 CPE

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


}
