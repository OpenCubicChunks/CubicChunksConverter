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

import cubicchunks.converter.gui.ConverterWorker;
import cubicchunks.converter.lib.Registry;
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.WorldConverter;
import cubicchunks.converter.lib.util.Utils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SwtConverterGui {

    private static List<Registry.ClassPair> CONVERTERS = new ArrayList<>(Registry.getConverters());
    private static List<String> CONVERTER_NAMES = CONVERTERS.stream()
            .map(c -> Registry.getReaderName(c.getIn()) + " -> " + Registry.getWriterName(c.getOut()))
            .collect(Collectors.toList());

    private final Display display;
    private final Shell shell;

    private final ProgressBar mainProgress;
    private final ProgressBar convertQueueBar;
    private final ProgressBar ioQueueBar;

    private Button convertButton;

    private Text srcInput;
    private Text dstInput;

    private Combo formatSelect;

    public static void main(String[] args) {
        new SwtConverterGui().start();
    }

    private SwtConverterGui() {
        display = new Display();
        shell = new Shell(display);
        shell.setText("CubicChunks Save Converter");
        shell.setLayout(new GridLayout(1, false));

        Composite fileSelectContainer = new Composite(shell, SWT.NONE);
        fileSelectContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        fileSelectContainer.setLayout(new GridLayout(3, false));

        srcInput = makeFileSelectionRow(shell, fileSelectContainer, "source", txt -> {
            dstInput.setText(txt + " - CubicChunks");
            updateConvertBtn(convertButton, srcInput, dstInput);
        });
        dstInput = makeFileSelectionRow(shell, fileSelectContainer, "destination", txt -> {
            updateConvertBtn(convertButton, srcInput, dstInput);
        });

        Composite convertComposite = new Composite(shell, SWT.NONE);
        convertComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        convertComposite.setLayout(new GridLayout(3, false));

        mainProgress = makeProgressBar(convertComposite, "Progress: ");
        convertButton = new Button(convertComposite, SWT.NONE);
        convertButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 3));
        convertButton.setText("Convert");
        convertButton.setEnabled(false);
        convertButton.addSelectionListener(widgetSelectedAdapter(e -> startConversion()));

        convertQueueBar = makeProgressBar(convertComposite, "Convert queue fill: ");
        ioQueueBar = makeProgressBar(convertComposite, "Write queue fill: ");
        makeFormatSelection(convertComposite);

        shell.pack();
        shell.setMinimumSize(shell.getSize());
    }

    private void start() {
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

    private static Text makeFileSelectionRow(Shell shell, Composite composite, String displayText, Consumer<String> onSetPath) {
        String labelText = Character.toUpperCase(displayText.charAt(0)) + displayText.substring(1) + ": ";
        Label label = new Label(composite, SWT.NONE);
        label.setText(labelText);
        label.setAlignment(SWT.RIGHT);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        label.pack();

        Text text = new Text(composite, SWT.NONE);
        text.setEditable(true);
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Button button = new Button(composite, SWT.NONE);
        button.setText("...");
        button.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        button.addSelectionListener(widgetSelectedAdapter(e -> selectFile(shell, text, displayText, onSetPath)));

        return text;
    }

    private static void selectFile(Shell shell, Text text, String message, Consumer<String> onSetPath) {
        DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
        dialog.setMessage("Select " + message + " folder");
        String path = dialog.open();
        if (path == null) {
            return;
        }
        text.setText(path);
        onSetPath.accept(path);
    }

    private static void updateConvertBtn(Button convert, Text src, Text dst) {
        convert.setEnabled(!src.getText().isEmpty() && !dst.getText().isEmpty()
                && Utils.isDirectory(src.getText()) && Utils.isValidPath(dst.getText()));
    }

    private static ProgressBar makeProgressBar(Composite parent, String label) {
        Label labelWidget = new Label(parent, SWT.NONE);
        labelWidget.setAlignment(SWT.RIGHT);
        labelWidget.setText(label);
        labelWidget.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        ProgressBar bar = new ProgressBar(parent, SWT.SMOOTH);
        bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return bar;
    }

    private void makeFormatSelection(Composite parent) {
        Composite formatSelectComposite = new Composite(parent, SWT.NONE);
        formatSelectComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        formatSelectComposite.setLayout(new GridLayout(2, false));

        Label formatLabel = new Label(formatSelectComposite, SWT.NONE);
        formatLabel.setText("Converter: ");
        formatLabel.setAlignment(SWT.RIGHT);
        formatLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        formatSelect = new Combo(formatSelectComposite, SWT.DROP_DOWN);
        formatSelect.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        for (String converter : CONVERTER_NAMES) {
            formatSelect.add(converter);
        }
        formatSelect.select(0);
    }

    // ----------------------------
    // conversion
    // ----------------------------

    private void startConversion() {

        String src = srcInput.getText();
        String dst = dstInput.getText();
        if (!Utils.isDirectory(src)) {
            convertButton.setEnabled(false);
            showErrorBox("Specified input path \"" + src + "\" is not a valid directory");
            return;
        }
        if (!Utils.isValidPath(dst)) {
            convertButton.setEnabled(false);
            showErrorBox("Specified output path \"" + dst + "\" is not a valid path");
            return;
        }
        if (Utils.fileExists(dst) && !Utils.isDirectory(dst)) {
            convertButton.setEnabled(false);
            showErrorBox("Specified output path \"" + dst + "\" already exists and is not a directory");
            return;
        }
        Path srcPath = Paths.get(src);
        Path dstPath = Paths.get(dst);
        try {
            Utils.createDirectories(dstPath);
        } catch (IOException e) {
            convertButton.setEnabled(false);
            e.printStackTrace();
            showErrorBox("An error occurred while trying to create directory \"" + dst + "\": " + e.toString() + ". See log for details");
            return;
        }
        try {
            if (!Utils.isEmpty(dstPath)) {
                MessageBox dialog = new MessageBox(this.shell, SWT.IGNORE | SWT.CANCEL);
                dialog.setText("Directory is not empty");
                dialog.setMessage("Specified directory " + dstPath.toAbsolutePath() + " is not empty.\n" +
                        "This may result in overwriting or losing all data in this directory!");
                int result = dialog.open();
                if (result == SWT.CANCEL) {
                    return;
                }
            }
        } catch (IOException e) {
            showErrorBox("Error while checking if destination directory is empty!");
            return;
        }
        mainProgress.setState(SWT.NORMAL);
        convertQueueBar.setState(SWT.NORMAL);
        ioQueueBar.setState(SWT.NORMAL);
        convertButton.setEnabled(false);

        @SuppressWarnings("unchecked")
        Registry.ClassPair<Object, Object> converterClasses = CONVERTERS.get(this.formatSelect.getSelectionIndex());

        WorldConverter<?, ?> converter = new WorldConverter<>(
                Registry.getLevelConverter(converterClasses).apply(srcPath, dstPath),
                Registry.getReader(converterClasses.getIn()).apply(srcPath),
                Registry.getConverter(converterClasses).get(),
                Registry.getWriter(converterClasses.getOut()).apply(dstPath)
        );
        SwtConvertWorker convert = new SwtConvertWorker(display, converter, mainProgress, convertQueueBar, ioQueueBar, () -> {
            updateConvertBtn(convertButton, srcInput, dstInput);
        });
        convert.start();

    }

    private void showErrorBox(String txt) {
        MessageBox dialog = new MessageBox(this.shell, SWT.ICON_ERROR | SWT.OK);
        dialog.setText("Error");
        dialog.setMessage(txt);
        dialog.open();
    }

}

