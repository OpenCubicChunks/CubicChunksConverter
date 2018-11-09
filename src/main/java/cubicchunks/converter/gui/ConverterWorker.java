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
package cubicchunks.converter.gui;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.*;

import cubicchunks.converter.lib.AnvilToCubicChunksConverter;
import cubicchunks.converter.lib.ConvertProgress;
import cubicchunks.converter.lib.ISaveConverter;

public class ConverterWorker extends SwingWorker<Throwable, Void> {
	private final AnvilToCubicChunksConverter converter;
	private final Path srcPath;
	private final Path dstPath;
	private JProgressBar progressBar;
	private JProgressBar convertQueueFill;
	private JProgressBar ioQueueFill;
	private Runnable onDone;

	public ConverterWorker(AnvilToCubicChunksConverter converter, Path srcPath, Path dstPath,
	                       JProgressBar progressBar, JProgressBar convertQueueFill, JProgressBar ioQueueFill,
	                       Runnable onDone) {
		this.converter = converter;
		this.srcPath = srcPath;
		this.dstPath = dstPath;
		this.progressBar = progressBar;
		this.convertQueueFill = convertQueueFill;
		this.ioQueueFill = ioQueueFill;
		this.onDone = onDone;
	}

	@Override protected Throwable doInBackground() throws Exception {
		try {
			this.converter.convert(this::publish, srcPath, dstPath);
		} catch(Throwable t) {
			t.printStackTrace();
			return t;
		}
		return null;
	}

	@Override protected void process(List<Void> l) {
		int submitted = converter.getSubmittedChunks();
		int total = converter.getTotalChunks();
		double progress = 100*submitted/(float) total;
		String message = String.format("Submitted chunk tasks: %d/%d %.2f%%", submitted, total, progress);
		this.progressBar.setMinimum(0);
		this.progressBar.setMaximum(total);
		this.progressBar.setValue(submitted);
		this.progressBar.setString(message);

		int maxSize = this.converter.getConvertBufferMaxSize();
		int size = this.converter.getConvertBufferFill();
		this.convertQueueFill.setMinimum(0);
		this.convertQueueFill.setMaximum(maxSize);
		this.convertQueueFill.setValue(size);
		this.convertQueueFill.setString(String.format("Convert queue fill: %d/%d", size, maxSize));

		maxSize = this.converter.getIOBufferMaxSize();
		size = this.converter.getIOBufferFill();
		this.ioQueueFill.setMinimum(0);
		this.ioQueueFill.setMaximum(maxSize);
		this.ioQueueFill.setValue(size);
		this.ioQueueFill.setString(String.format("IO queue fill: %d/%d", size, maxSize));
	}

	@Override
	protected void done() {
		onDone.run();
		Throwable t;
		try {
			t = get();
		} catch (InterruptedException | ExecutionException e) {
			t = e;
		}
		if (t == null) {
			return;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream ps;
		try {
			ps = new PrintStream(out, true, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			throw new Error(e1);
		}
		t.printStackTrace(ps);
		ps.close();
		String str;
		try {
			str = new String(out.toByteArray(), "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			throw new Error(e1);
		}
		JOptionPane.showMessageDialog(null, str);
	}
}
