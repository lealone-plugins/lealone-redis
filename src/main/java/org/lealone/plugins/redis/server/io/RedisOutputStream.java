/*
 * Copyright 2009-2010 MBTE Sweden AB. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package org.lealone.plugins.redis.server.io;

/**
 * The class implements a buffered output stream without synchronization There are also special
 * operations like in-place string encoding. This stream fully ignore mark/reset and should not be
 * used outside Jedis
 */
public final class RedisOutputStream {

    public static final int OUTPUT_BUFFER_SIZE = Integer.parseInt(System
            .getProperty("redis.bufferSize.output", System.getProperty("redis.bufferSize", "8192")));

    private final static int[] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999,
            999999999, Integer.MAX_VALUE };

    private final static byte[] DigitTens = { '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1', '1',
            '1', '1', '1', '1', '1', '1', '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4', '4',
            '4', '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6', '6', '6',
            '6', '6', '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8',
            '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', };

    private final static byte[] DigitOnes = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1',
            '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3',
            '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', };

    private final static byte[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z' };

    private final NetBufferOutput out;
    private final byte[] buf;
    private int count;

    public RedisOutputStream(final NetBufferOutput out) {
        this(out, OUTPUT_BUFFER_SIZE);
    }

    public RedisOutputStream(final NetBufferOutput out, final int size) {
        this.out = out;
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    private void flushBuffer() {
        if (count > 0) {
            out.write(buf, 0, count);
            count = 0;
        }
    }

    public void write(int b) {
        write((byte) b);
    }

    public void write(final byte b) {
        if (count == buf.length) {
            flushBuffer();
        }
        buf[count++] = b;
    }

    public void write(final byte[] b) {
        write(b, 0, b.length);
    }

    public void write(final byte[] b, final int off, final int len) {
        if (len >= buf.length) {
            flushBuffer();
            out.write(b, off, len);
        } else {
            if (len >= buf.length - count) {
                flushBuffer();
            }

            System.arraycopy(b, off, buf, count, len);
            count += len;
        }
    }

    public void writeCrLf() {
        if (2 >= buf.length - count) {
            flushBuffer();
        }

        buf[count++] = '\r';
        buf[count++] = '\n';
    }

    public void writeIntCrLf(int value) {
        if (value < 0) {
            write((byte) '-');
            value = -value;
        }

        int size = 0;
        while (value > sizeTable[size])
            size++;

        size++;
        if (size >= buf.length - count) {
            flushBuffer();
        }

        int q, r;
        int charPos = count + size;

        while (value >= 65536) {
            q = value / 100;
            r = value - ((q << 6) + (q << 5) + (q << 2));
            value = q;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        for (;;) {
            q = (value * 52429) >>> (16 + 3);
            r = value - ((q << 3) + (q << 1));
            buf[--charPos] = digits[r];
            value = q;
            if (value == 0)
                break;
        }
        count += size;

        writeCrLf();
    }

    public void flush() {
        flushBuffer();
        out.flush();
    }
}
