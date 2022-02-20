/*
 *  This file is part of CubicChunksConverter, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2017-2021 contributors
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
package cubicchunks.converter.lib.conf.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import cubicchunks.converter.lib.util.Matrix4d;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class Matrix4dArgument implements ArgumentType<Matrix4d> {
    @Override
    public Matrix4d parse(StringReader reader) throws CommandSyntaxException {
        return parseVector(reader);
    }

    public static Matrix4d parseVector(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        double m00 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m01 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m02 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m03 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }


        double m10 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m11 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m12 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m13 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }


        double m20 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m21 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m22 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m23 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }


        double m30 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m31 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m32 = parseDouble(reader);
        if(!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
        reader.skip();
        double m33 = parseDouble(reader);
        return new Matrix4d(
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        );
    }

    public static double parseDouble(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw new RuntimeException("Expected integer got end of command!");
        } else {
            return reader.readDouble();
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return null;
    }

    @Override
    public Collection<String> getExamples() {
        return null;
    }
}
