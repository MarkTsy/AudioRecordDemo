package com.mi.audiorecord;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <pre>
 *     author : tao
 *     time   : 2022/03/01
 * </pre>
 */
public class WavEncoder implements Encoder {

    private static final int HEADER_LENGTH = 44;
    /** Indicates PCM format. */
    public static final short FORMAT_PCM = 1;
    /** Indicates ALAW format. */
    public static final short FORMAT_ALAW = 6;
    /** Indicates ULAW format. */
    public static final short FORMAT_ULAW = 7;

    private short mFormat;
    private short mNumChannels;
    private int mSampleRate;
    private short mBitsPerSample;

    public WavEncoder(short format, short numChannels, int sampleRate, short bitsPerSample) {
        mFormat = format;
        mSampleRate = sampleRate;
        mNumChannels = numChannels;
        mBitsPerSample = bitsPerSample;
    }

    @NonNull
    @Override
    public byte[] encode(@NonNull byte[] bytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writeId(out, "RIFF");
        writeInt(out, 36 + bytes.length);
        writeId(out, "WAVE");

        /* fmt chunk */
        writeId(out, "fmt ");
        writeInt(out, 16);
        writeShort(out, mFormat);
        writeShort(out, mNumChannels);
        writeInt(out, mSampleRate);
        writeInt(out, mNumChannels * mSampleRate * mBitsPerSample / 8);
        writeShort(out, (short)(mNumChannels * mBitsPerSample / 8));
        writeShort(out, mBitsPerSample);

        /* data chunk */
        writeId(out, "data");
        writeInt(out, bytes.length);

        out.write(bytes);

        return out.toByteArray();
    }

    private void writeId(OutputStream out, String id) throws IOException {
        for (int i = 0; i < id.length(); i++) out.write(id.charAt(i));
    }

    private void writeInt(OutputStream out, int val) throws IOException {
        out.write(val >> 0);
        out.write(val >> 8);
        out.write(val >> 16);
        out.write(val >> 24);
    }

    private void writeShort(OutputStream out, short val) throws IOException {
        out.write(val >> 0);
        out.write(val >> 8);
    }
}
