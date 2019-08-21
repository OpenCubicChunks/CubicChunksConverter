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
package cubicchunks.converter.swt;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import cubicchunks.converter.lib.IProgressListener;
import cubicchunks.converter.lib.convert.WorldConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class SwtConvertWorker extends Thread {

    private final Display display;
    private final WorldConverter<?, ?> converter;
    private final ProgressBar mainProgress;
    private final ProgressBar convertQueueBar;
    private final ProgressBar ioQueueBar;
    private final Runnable onDone;

    private volatile boolean finishedProgressUpdate = true;
    private volatile long finishedProgressUpdateTime = 0;

    SwtConvertWorker(Display display, WorldConverter<?, ?> converter, ProgressBar mainProgress, ProgressBar convertQueueBar,
            ProgressBar ioQueueBar,
            Runnable onDone) {
        this.display = display;
        this.converter = converter;
        this.mainProgress = mainProgress;
        this.convertQueueBar = convertQueueBar;
        this.ioQueueBar = ioQueueBar;
        this.onDone = onDone;
    }

    private void updateProgress() {
        if (!finishedProgressUpdate || (System.currentTimeMillis() - finishedProgressUpdateTime) < 50) {
            return;
        }
        int submitted = this.converter.getSubmittedChunks();
        int total = this.converter.getTotalChunks();

        int convMaxSize = this.converter.getConvertBufferMaxSize();
        int convSize = this.converter.getConvertBufferFill();

        int ioMaxSize = this.converter.getIOBufferMaxSize();
        int ioSize = this.converter.getIOBufferFill();

        display.asyncExec(() -> {
            this.mainProgress.setMinimum(0);
            this.mainProgress.setMaximum(total);
            this.mainProgress.setSelection(submitted);

            this.convertQueueBar.setMinimum(0);
            this.convertQueueBar.setMaximum(convMaxSize);
            this.convertQueueBar.setSelection(convSize);

            this.ioQueueBar.setMinimum(0);
            this.ioQueueBar.setMaximum(ioMaxSize);
            this.ioQueueBar.setSelection(ioSize);

            this.finishedProgressUpdate = true;
            this.finishedProgressUpdateTime = System.currentTimeMillis();
        });
    }

    @Override public void run() {
        try {
            this.converter.convert(new IProgressListener() {
                @Override public void update(Void progress) {
                    updateProgress();
                }

                @Override public IProgressListener.ErrorHandleResult error(Throwable t) {
                    try {
                        CompletableFuture<ErrorHandleResult> future = new CompletableFuture<>();
                        display.asyncExec(() -> future.complete(showErrorMessage(t)));
                        return future.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ErrorHandleResult.STOP_KEEP_DATA;
                    }
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            display.syncExec(() -> showErrorMessage(t));
        }
        display.asyncExec(() -> {
            mainProgress.setSelection(0);
            convertQueueBar.setSelection(0);
            ioQueueBar.setSelection(0);
            onDone.run();
        });
    }


    private IProgressListener.ErrorHandleResult showErrorMessage(Throwable error) {
        Shell shell = new Shell(display.getActiveShell(), SWT.APPLICATION_MODAL);
        shell.setLayout(new GridLayout(4, true));

        Label message = new Label(shell, SWT.NONE);
        message.setText("An error occurred while converting chunks. Do you want to continue?\nDetails:");
        message.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1));

        Text details = new Text(shell, SWT.MULTI | SWT.READ_ONLY);
        details.setText(exceptionString(error));
        details.setSize(details.getSize().x, 40);
        details.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1));

        Button ignore = new Button(shell, SWT.NONE);
        ignore.setText("Ignore");

        Button ignoreAll = new Button(shell, SWT.NONE);
        ignore.setText("Ignore all");

        Button stopDelete = new Button(shell, SWT.NONE);
        ignore.setText("Stop, delete results");

        Button stopKeep = new Button(shell, SWT.NONE);
        ignore.setText("Stop, keep result");

        SelectionListener selectionListener = widgetSelectedAdapter(e -> {
            shell.setData("result", e.item);
            shell.close();
        });

        ignore.addSelectionListener(selectionListener);
        ignoreAll.addSelectionListener(selectionListener);
        stopDelete.addSelectionListener(selectionListener);
        stopKeep.addSelectionListener(selectionListener);

        Object pressed = shell.getData("result");
        if (pressed == ignore) {
            return IProgressListener.ErrorHandleResult.IGNORE;
        }
        if (pressed == ignoreAll) {
            return IProgressListener.ErrorHandleResult.IGNORE_ALL;
        }
        if (pressed == stopDelete) {
            return IProgressListener.ErrorHandleResult.STOP_DISCARD;
        }
        if (pressed == stopKeep) {
            return IProgressListener.ErrorHandleResult.STOP_KEEP_DATA;
        }
        assert false;
        return IProgressListener.ErrorHandleResult.IGNORE;
    }

    private static String exceptionString(Throwable t) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps;
        try {
            ps = new PrintStream(out, true, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            throw new Error(e1);
        }
        t.printStackTrace(ps);
        ps.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
