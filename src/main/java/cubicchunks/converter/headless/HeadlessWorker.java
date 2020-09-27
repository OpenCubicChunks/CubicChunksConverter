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
package cubicchunks.converter.headless;

import cubicchunks.converter.lib.IProgressListener;
import cubicchunks.converter.lib.conf.command.EditTaskCommands;
import cubicchunks.converter.lib.convert.WorldConverter;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class HeadlessWorker extends SwingWorker<Throwable, Void> {
    private final WorldConverter converter;
    private final Runnable onFail;
    private Runnable onDone;

    private static final Logger LOGGER = Logger.getLogger(EditTaskCommands.class.getSimpleName());

    public HeadlessWorker(WorldConverter converter, Runnable onDone, Runnable onFail) {
        this.converter = converter;
        this.onDone = onDone;
        this.onFail = onFail;
    }

    @Override protected Throwable doInBackground() {
        try {
            this.converter.convert(new IProgressListener() {
                @Override public void update(Void progress) {
                    publish(progress);
                }

                @Override public IProgressListener.ErrorHandleResult error(Throwable t) {
                    try {
                        onFail.run();
                        CompletableFuture<ErrorHandleResult> future = new CompletableFuture<>();
                        EventQueue.invokeAndWait(() -> future.complete(showErrorMessage(t)));
                        return future.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ErrorHandleResult.STOP_KEEP_DATA;
                    }
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            onFail.run();
            return t;
        }
        return null;
    }

    private IProgressListener.ErrorHandleResult showErrorMessage(Throwable error) {
        String[] options = {"Ignore", "Ignore all", "Stop, delete results", "Stop, keep result"};

        LOGGER.info(exceptionString(error));
        LOGGER.info("An error occurred while converting chunks. Do you want to continue?");

//            int code = JOptionPane.showOptionDialog(
//                parent,
//                infoPanel,
//                "An error occurred while converting chunks", 0, JOptionPane.ERROR_MESSAGE,
//                null, options, "Ignore");
        int code = 0; //askUserForDecision();
        if (code == JOptionPane.CLOSED_OPTION) {
            return IProgressListener.ErrorHandleResult.IGNORE;
        }
        switch (code) {
            case 0:
                return IProgressListener.ErrorHandleResult.IGNORE;
            case 1:
                return IProgressListener.ErrorHandleResult.IGNORE_ALL;
            case 2:
                return IProgressListener.ErrorHandleResult.STOP_DISCARD;
            case 3:
                return IProgressListener.ErrorHandleResult.STOP_KEEP_DATA;
            default:
                assert false;
                return IProgressListener.ErrorHandleResult.IGNORE;
        }
    }

    @Override protected void process(List<Void> l) {
        int submitted = converter.getSubmittedChunks();
        int total = converter.getTotalChunks();
        double progress = 100 * submitted / (float) total;
        String messageRead = String.format("Submitted chunk tasks: %d/%d %.2f%%", submitted, total, progress);

        int maxSize = this.converter.getConvertBufferMaxSize();
        int size = this.converter.getConvertBufferFill();
        String messageConvert = String.format("Convert queue fill: %d/%d", size, maxSize);

        maxSize = this.converter.getIOBufferMaxSize();
        size = this.converter.getIOBufferFill();
        String messageWrite = String.format("IO queue fill: %d/%d", size, maxSize);

        System.out.println(messageRead + "\n" + messageConvert + "\n" + messageWrite);
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
        JOptionPane.showMessageDialog(null, exceptionString(t));
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
