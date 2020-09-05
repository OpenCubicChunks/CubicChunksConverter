package cubicchunks.converter.lib.conf.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.Vector3i;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class BoundingBoxArgument implements ArgumentType<BoundingBox> {

    @Override
    public BoundingBox parse(StringReader reader) throws CommandSyntaxException {
        if(parseLiteral(reader, "all") >= 0) {
            return new BoundingBox(Vector3i.MIN_VECTOR, Vector3i.MAX_VECTOR);
        }

        int i = reader.getCursor();
        Vector3i minPos = Vector3iArgument.parseVector(reader);
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            Vector3i maxPos = Vector3iArgument.parseVector(reader);
            return new BoundingBox(minPos, maxPos);
        } else {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
    }

    private int parseLiteral(final StringReader reader, final String literal) {
        final int start = reader.getCursor();
        if (reader.canRead(literal.length())) {
            final int end = start + literal.length();
            if (reader.getString().substring(start, end).equals(literal)) {
                reader.setCursor(end);
                if (!reader.canRead() || reader.peek() == ' ') {
                    return end;
                } else {
                    reader.setCursor(start);
                }
            }
        }
        return -1;
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