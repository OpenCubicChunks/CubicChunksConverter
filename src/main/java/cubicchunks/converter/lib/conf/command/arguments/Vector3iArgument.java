package cubicchunks.converter.lib.conf.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import cubicchunks.converter.lib.util.Vector3i;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class Vector3iArgument implements ArgumentType<Vector3i> {

//    private final Vector3i position;
//
//    public Vector3iArgument(int x, int y, int z) {
//        this.position = new Vector3i(x, y, z);
//    }
//
//    public Vector3i getPosition() {
//        return position;
//    }

    @Override
    public Vector3i parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        int locationpart = parseInt(reader);
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            int locationpart1 = parseInt(reader);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                int locationpart2 = parseInt(reader);
                return new Vector3i(locationpart, locationpart1, locationpart2);
            } else {
                reader.setCursor(i);
                throw new RuntimeException("Incomplete or invalid command!");
            }
        } else {
            reader.setCursor(i);
            throw new RuntimeException("Incomplete or invalid command!");
        }
    }

    public static int parseInt(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw new RuntimeException("Expected integer got end of command!");
        } else {
            return reader.readInt();
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
