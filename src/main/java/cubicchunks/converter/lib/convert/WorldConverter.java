/*
 *  This file is part of CubicChunksConverter, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2017 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.converter.lib.convert;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import cubicchunks.converter.lib.anvil2cc.Anvil2CCLevelInfoConverter;
import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.Dimensions;
import cubicchunks.converter.lib.IProgressListener;
import cubicchunks.converter.lib.Utils;

public class WorldConverter<IN, OUT> {

	private static final int THREADS = Runtime.getRuntime().availableProcessors();
	private static final int CONVERT_QUEUE_SIZE = 32*THREADS;
	private static final int IO_QUEUE_SIZE = 8*THREADS;
    private final LevelInfoConverter<IN, OUT> levelConverter;
    private final ChunkDataReader<IN> reader;
    private final ChunkDataConverter<IN, OUT> converter;
    private final ChunkDataWriter<OUT> writer;

    private AtomicInteger chunkCount = new AtomicInteger(-1);

    private static final BiFunction<Dimension, Path, Path> LOCATION_FUNC_DST = (d, p) -> {
        if (!d.getDirectory().isEmpty()) {
            p = p.resolve(d.getDirectory());
        }
        return p;
    };
	private int copyChunks = -1;

	private boolean countingChunks;

	private ArrayBlockingQueue<Runnable> convertQueueImpl = new ArrayBlockingQueue<>(CONVERT_QUEUE_SIZE);
	private ArrayBlockingQueue<Runnable> ioQueueImpl = new ArrayBlockingQueue<>(IO_QUEUE_SIZE);

	private ThreadPoolExecutor convertQueue = new ThreadPoolExecutor(
		THREADS, THREADS, 0L, TimeUnit.MILLISECONDS, convertQueueImpl);
	private ThreadPoolExecutor ioQueue = new ThreadPoolExecutor(
		THREADS, THREADS, 0L, TimeUnit.MILLISECONDS, ioQueueImpl);

	{
		RejectedExecutionHandler handler = ((r, executor) -> {
			try {
				if (!executor.isShutdown()) {
					executor.getQueue().put(r);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RejectedExecutionException("Executor was interrupted while the task was waiting to put on work queue", e);
			}
		});
		convertQueue.setRejectedExecutionHandler(handler);
		ioQueue.setRejectedExecutionHandler(handler);
	}

	public WorldConverter(
	    LevelInfoConverter<IN, OUT> levelConverter,
        ChunkDataReader<IN> reader,
        ChunkDataConverter<IN, OUT> converter,
        ChunkDataWriter<OUT> writer) {

        this.levelConverter = levelConverter;
        this.reader = reader;
        this.converter = converter;
        this.writer = writer;
    }

	public void convert(IProgressListener progress) throws IOException {
		chunkCount.set(0);
		countingChunks = true;
		copyChunks = 0;
		startCounting();

		levelConverter.convert();
		try(ChunkDataReader<IN> reader = this.reader; ChunkDataWriter<OUT> writer = this.writer) {
		    reader.loadChunks(inData -> {
                convertQueue.submit(new ChunkConvertTask<>(converter, writer, progress, ioQueue, inData));
				copyChunks++;
            });
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			convertQueue.shutdown();
			boolean shutdownNow = false;
			try {
				convertQueue.awaitTermination(Long.MAX_VALUE/2, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				convertQueue.shutdownNow();
				shutdownNow = true;
			}
			// convert finished, now shut down IO
			if (shutdownNow) ioQueue.shutdownNow();
			else ioQueue.shutdown();

			try {
				ioQueue.awaitTermination(Long.MAX_VALUE/2, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				ioQueue.shutdownNow();
			}
		}
	}

	public int getSubmittedChunks() {
		return copyChunks;
	}

	public int getTotalChunks() {
		return chunkCount.get();
	}

	public int getConvertBufferFill() {
		return convertQueueImpl.size();
	}

	public int getConvertBufferMaxSize() {
		return CONVERT_QUEUE_SIZE;
	}

	public int getIOBufferFill() {
		return ioQueueImpl.size();
	}

	public int getIOBufferMaxSize() {
		return IO_QUEUE_SIZE;
	}

	private void startCounting() {
		countingChunks = true;
		new Thread(() -> {
            try {
                reader.countInputChunks(() -> chunkCount.getAndIncrement());
            } catch (IOException e) {
                e.printStackTrace();
            }
            countingChunks = false;
		}, "Chunk and File counting thread").start();
	}


	public static class ChunkConvertTask<IN, OUT> implements Callable<IOWriteTask> {
		private final ChunkDataConverter<IN, OUT> converter;
        private final ChunkDataWriter<OUT> writer;
        private final IProgressListener progress;
		private final ThreadPoolExecutor ioExecutor;
        private final IN toConvert;

        public ChunkConvertTask(
            ChunkDataConverter<IN, OUT> converter,
            ChunkDataWriter<OUT> writer,
            IProgressListener progress,
            ThreadPoolExecutor ioExecutor,
            IN toConvert) {

            this.converter = converter;
            this.writer = writer;
            this.progress = progress;
			this.ioExecutor = ioExecutor;
            this.toConvert = toConvert;
        }

		@Override public IOWriteTask call() {
            OUT converted = converter.convert(toConvert);
			IOWriteTask<OUT> data = new IOWriteTask<>(converted, writer);
			progress.update(null);
			ioExecutor.submit(data);
			return data;
		}
	}

	public static class IOWriteTask<OUT> implements Callable<Void> {
		private final OUT toWrite;
		private final ChunkDataWriter<OUT> writer;

        public IOWriteTask(OUT toWrite, ChunkDataWriter<OUT> writer) {
            this.toWrite = toWrite;
            this.writer = writer;
        }

        @Override public Void call() throws Exception {
			writer.accept(toWrite);
			return null;
		}
	}
}
