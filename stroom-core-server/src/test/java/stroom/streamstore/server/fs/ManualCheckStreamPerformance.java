/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.streamstore.server.fs;

import org.apache.commons.lang.StringUtils;
import stroom.util.io.FileUtil;
import stroom.util.io.StreamUtil;
import stroom.util.thread.ThreadUtil;
import stroom.util.zip.HeaderMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class ManualCheckStreamPerformance {
    static int testThreadCount = 10;
    static int testSize = 100000;

    public static File getTempFile() throws IOException {
        final File tempFile = File.createTempFile("test", "test", FileUtil.getTempDir());
        FileUtil.deleteFile(tempFile);
        return tempFile;
    }

    public abstract InputStream getInputStream() throws IOException;

    public abstract OutputStream getOutputStream() throws IOException;

    public abstract long getFileSize() throws IOException;

    public abstract void onCloseOutput(OutputStream outputStream);

    public static class BlockGzipManualCheckStreamPerformance extends ManualCheckStreamPerformance {
        File tempFile;
        int blockSize;
        long blockCount;

        public BlockGzipManualCheckStreamPerformance(final int blockSize) {
            this.blockSize = blockSize;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new BufferedInputStream(new BlockGZIPInputFile(tempFile));
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            tempFile = getTempFile();
            return new BufferedOutputStream(new BlockGZIPOutputFile(tempFile, blockSize));
        }

        @Override
        public long getFileSize() throws IOException {
            return tempFile.length();
        }

        @Override
        public void onCloseOutput(final OutputStream arg0) {
            blockCount = ((BlockGZipOutputStream) arg0).getBlockCount();
        }

        @Override
        public String toString() {
            return "BlockGzip blockSize=" + blockSize + " blockCount=" + blockCount;
        }
    }

    public static class UncompressedCheckStreamPerformance extends ManualCheckStreamPerformance {
        File tempFile;

        @Override
        public InputStream getInputStream() throws IOException {
            return new BufferedInputStream(new FileInputStream(tempFile));
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            tempFile = getTempFile();
            return new BufferedOutputStream(new FileOutputStream(tempFile));
        }

        @Override
        public long getFileSize() throws IOException {
            return tempFile.length();
        }

        @Override
        public String toString() {
            return "Uncompressed";
        }

        @Override
        public void onCloseOutput(final OutputStream arg0) {
        }
    }

    public static class GzipCheckStreamPerformance extends ManualCheckStreamPerformance {
        File tempFile;

        @Override
        public InputStream getInputStream() throws IOException {
            return new BufferedInputStream(new GZIPInputStream(new FileInputStream(tempFile)));
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            tempFile = getTempFile();
            return new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(tempFile)));
        }

        @Override
        public long getFileSize() throws IOException {
            return tempFile.length();
        }

        @Override
        public String toString() {
            return "Gzip";
        }

        @Override
        public void onCloseOutput(final OutputStream arg0) {
        }
    }

    public long writeLargeFileTest() throws IOException {
        final long startTime = System.currentTimeMillis();
        final OutputStream os = getOutputStream();
        for (int i = 0; i < testSize; i++) {
            os.write("some data that may compress quite well TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            os.write(("some other information TEST\n" + i).getBytes(StreamUtil.DEFAULT_CHARSET));
            os.write("concurrent testing TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            os.write("TEST TEST TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            os.write("JAMES BETTY TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            os.write("FRED TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            os.write("<XML> TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
        }

        os.close();

        onCloseOutput(os);

        final long timeTaken = System.currentTimeMillis() - startTime;
        return timeTaken;
    }

    public long readLargeFileTest() throws IOException {
        try (OutputStream os = getOutputStream()) {
            for (int i = 0; i < testSize; i++) {
                os.write("some data that may compress quite well TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
                os.write(("some other information TEST\n" + i).getBytes(StreamUtil.DEFAULT_CHARSET));
                os.write("concurrent testing TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
                os.write("TEST TEST TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
                os.write("JAMES BETTY TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
                os.write("FRED TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
                os.write("<XML> TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            }

            os.close();

            onCloseOutput(os);
        }

        final long startTime = System.currentTimeMillis();

        try (LineNumberReader reader = new LineNumberReader(
                new InputStreamReader(getInputStream(), StreamUtil.DEFAULT_CHARSET))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("TEST")) {
                    throw new RuntimeException("Something has gone wrong");
                }

            }

            final long timeTaken = System.currentTimeMillis() - startTime;
            return timeTaken;
        }
    }

    public long seekLargeFileTest() throws IOException {
        final byte[] sb = "some data that may compress quite well TEST\n".getBytes(StreamUtil.DEFAULT_CHARSET);

        try (OutputStream os = getOutputStream()) {
            for (int i = 0; i < testSize; i++) {
                os.write(sb);
            }

            os.close();

            onCloseOutput(os);
        }

        final long startTime = System.currentTimeMillis();

        try (InputStream is = getInputStream()) {
            StreamUtil.skip(is, (testSize / 2) * sb.length);
            try (LineNumberReader reader = new LineNumberReader(
                    new InputStreamReader(is, StreamUtil.DEFAULT_CHARSET))) {
                final String line1 = reader.readLine();
                final String line2 = reader.readLine();

                if (line1 == null || line2 == null || !line1.contains("TEST") || !line2.contains("TEST")) {
                    throw new RuntimeException("Something has gone wrong");
                }
            }

            final long timeTaken = System.currentTimeMillis() - startTime;
            return timeTaken;
        }
    }

    public interface TimedAction {
        long newTimedAction() throws IOException;
    }

    public static void averageTimeCheck(final String msg, final TimedAction provider) throws IOException {
        final HashMap<Thread, Long> threadTimes = new HashMap<>();
        for (int i = 0; i < testThreadCount; i++) {
            final Thread t = new Thread((() -> {
                try {
                    threadTimes.put(Thread.currentThread(), provider.newTimedAction());
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }));

            threadTimes.put(t, 0L);
            t.start();
        }
        boolean running = false;
        do {
            ThreadUtil.sleep(1000);

            running = false;
            for (final Thread thread : threadTimes.keySet()) {
                if (thread.isAlive()) {
                    running = true;
                    break;
                }
            }
        } while (running);

        long totalTime = 0;
        for (final Thread thread : threadTimes.keySet()) {
            totalTime += threadTimes.get(thread);
        }
        final long average = totalTime / threadTimes.size();

        System.out.println(
                "Average for " + StringUtils.leftPad(msg, 20) + " is " + StringUtils.leftPad("" + average, 10));
    }

    public static void main(final String[] args) throws IOException {
        final HeaderMap map = new HeaderMap();
        map.loadArgs(args);

        if (map.containsKey("testThreadCount")) {
            testThreadCount = Integer.parseInt(map.get("testThreadCount"));
        }
        if (map.containsKey("testSize")) {
            testSize = Integer.parseInt(map.get("testSize"));
        }

        averageTimeCheck("W BGZIP 1000000",
                () -> new BlockGzipManualCheckStreamPerformance(1000000).writeLargeFileTest());
        averageTimeCheck("W Gzip", () -> new GzipCheckStreamPerformance().writeLargeFileTest());
        averageTimeCheck("W Uncompressed", () -> new UncompressedCheckStreamPerformance().writeLargeFileTest());
        averageTimeCheck("R BGZIP 1000000",
                () -> new BlockGzipManualCheckStreamPerformance(1000000).readLargeFileTest());
        averageTimeCheck("R Gzip", () -> new GzipCheckStreamPerformance().readLargeFileTest());
        averageTimeCheck("R Uncompressed", () -> new UncompressedCheckStreamPerformance().readLargeFileTest());
        averageTimeCheck("SEEK BGZIP 1000000",
                () -> new BlockGzipManualCheckStreamPerformance(1000000).seekLargeFileTest());
        averageTimeCheck("SEEK Gzip", () -> new GzipCheckStreamPerformance().seekLargeFileTest());
        averageTimeCheck("SEEK Uncompressed", () -> new UncompressedCheckStreamPerformance().seekLargeFileTest());
    }

}
