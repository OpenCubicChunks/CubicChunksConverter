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
package cubicchunks.converter.lib;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import cubicchunks.regionlib.impl.save.MinecraftSaveSection;

import static cubicchunks.regionlib.impl.save.MinecraftSaveSection.MinecraftRegionType.MCA;

public class AnvilToCubicChunksConverter implements ISaveConverter {

	private static final int THREADS = Runtime.getRuntime().availableProcessors();
	private static final int CONVERT_QUEUE_SIZE = 32*THREADS;
	private static final int IO_QUEUE_SIZE = 8*THREADS;

	private static final BiFunction<Dimension, Path, Path> LOCATION_FUNC_SRC = (d, p) -> {
		if (!d.getDirectory().isEmpty()) {
			p = p.resolve(d.getDirectory());
		}
		return p.resolve("region");
	};

	private static final BiFunction<Dimension, Path, Path> LOCATION_FUNC_DST = (d, p) -> {
		if (!d.getDirectory().isEmpty()) {
			p = p.resolve(d.getDirectory());
		}
		return p;
	};

	private volatile int chunkCount = -1;
	private volatile int fileCount = -1;

	private int copyChunks = -1;
	private int copiedFiles = -1;
	private Map<Dimension, MinecraftSaveSection> saves = new ConcurrentHashMap<>();


	private boolean countingFiles;
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

	@Override
	public void convert(IProgressListener progress, Path srcDir, Path dstDir) throws IOException {
		saves.clear();
		chunkCount = 0;
		countingChunks = true;
		fileCount = 0;
		countingFiles = true;
		copyChunks = 0;
		copiedFiles = 0;
		initDimensions(srcDir);
		startCounting(srcDir);

		copyIndependentData(progress, srcDir, dstDir);

		new AnvilToCCLevelInfoConverter(srcDir, dstDir).run();
		try {
			for (Dimension d : Dimensions.getDimensions()) {
				Path srcLoc = LOCATION_FUNC_SRC.apply(d, srcDir);
				if (!Files.exists(srcLoc)) {
					continue;
				}

				MinecraftSaveSection vanillaSave = saves.get(d);

				try (SaveCubeColumns saveCubic = SaveCubeColumns.create(LOCATION_FUNC_DST.apply(d, dstDir))) {
					vanillaSave.forAllKeys(mcPos -> {
						convertQueue.submit(new ChunkConvertTask(
							progress, convertQueue, ioQueue, vanillaSave, saveCubic, mcPos.getEntryX(), mcPos.getEntryZ()
						));
						copyChunks++;
						progress.update(null);
					});
				}
			}
		} catch (IOException ex) {
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

			for (MinecraftSaveSection save : saves.values()) {
				try {
					save.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public int getSubmittedChunks() {
		return copyChunks;
	}

	public int getTotalChunks() {
		return chunkCount;
	}

	public boolean isCountingChunks() {
		return countingChunks;
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

	private void initDimensions(Path src) {
		for (Dimension d : Dimensions.getDimensions()) {
			Path srcLoc = LOCATION_FUNC_SRC.apply(d, src);
			if (!Files.exists(srcLoc)) {
				continue;
			}

			MinecraftSaveSection vanillaSave = MinecraftSaveSection.createAt(LOCATION_FUNC_SRC.apply(d, src), MCA);
			saves.put(d, vanillaSave);
		}
	}

	private void copyIndependentData(IProgressListener progress, Path srcDir, Path dstDir) throws IOException {
		Utils.copyEverythingExcept(srcDir, srcDir, dstDir, file ->
				file.toString().contains("level.dat") ||
					Dimensions.getDimensions().stream().anyMatch(dim ->
						srcDir.resolve(dim.getDirectory()).resolve("region").equals(file)
					),
			f -> {
				copiedFiles++;
				progress.update(null);
			}
		);
	}

	private void startCounting(Path src) {
		countingChunks = true;
		new Thread(() -> {
			for (MinecraftSaveSection save : saves.values()) {
				try {
					// increment is non-atomic but it's safe here because we don't need it to be anywhere close to correct while counting
					save.forAllKeys(loc -> chunkCount++);
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					fileCount = Utils.countFiles(src);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			countingChunks = false;
		}, "Chunk and File counting thread").start();
	}


	public static class ChunkConvertTask implements Callable<ConvertedData> {
		private static final AnvilToCubicChunksConvertDataConverter converter = new AnvilToCubicChunksConvertDataConverter();
		private IProgressListener progress;
		private final ThreadPoolExecutor executor;
		private final ThreadPoolExecutor ioExecutor;
		private final int x;
		private final int z;
		private final MinecraftSaveSection save;
		private final SaveCubeColumns destSave;

		public ChunkConvertTask(IProgressListener progress, ThreadPoolExecutor thisExecutor, ThreadPoolExecutor ioExecutor, MinecraftSaveSection save, SaveCubeColumns destSave, int x, int z) {
			this.progress = progress;
			this.executor = thisExecutor;
			this.ioExecutor = ioExecutor;
			this.x = x;
			this.z = z;
			this.save = save;
			this.destSave = destSave;
		}

		public int getX() {
			return x;
		}

		public int getZ() {
			return z;
		}

		public MinecraftSaveSection getSave() {
			return save;
		}

		public SaveCubeColumns getDestSave() {
			return destSave;
		}

		@Override public ConvertedData call() throws Exception {
			if (save == null) {
				executor.shutdown();
				return null;
			}
			ConvertedData data = converter.convert(this);
			progress.update(null);
			ioExecutor.submit(data);
			return data;
		}
	}

	public static class ConvertedData implements Callable<Void> {
		private final ChunkConvertTask task;
		private final Map<Integer, ByteBuffer> cubes;
		private final ByteBuffer column;

		public ConvertedData(ChunkConvertTask task, Map<Integer, ByteBuffer> cubes, ByteBuffer column) {
			this.task = task;
			this.cubes = cubes;
			this.column = column;
		}


		@Override public Void call() throws Exception {
			task.destSave.save2d(
				new EntryLocation2D(task.getX(), task.getZ()),
				column
			);
			for (int y : cubes.keySet()) {
				task.destSave.save3d(
					new EntryLocation3D(task.getX(), y, task.getZ()),
					cubes.get(y)
				);
			}
			task.progress.update(null);
			return null;
		}
	}
}
