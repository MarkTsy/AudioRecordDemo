package com.mi.audiorecord;

import java.io.IOException;

/**
 * <pre>
 *     author : tao
 *     time   : 2022/03/01
 * </pre>
 */
public interface Encoder {
    byte[] encode(byte[] bytes) throws IOException;
}
