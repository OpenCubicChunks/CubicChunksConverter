package cubicchunks.converter.lib.conf.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Arrays;
import java.util.Collection;

public class WildcardIntegerArgument implements ArgumentType<Integer> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0", "123", "-123", "*");

    private final int minimum;
    private final int maximum;

    private WildcardIntegerArgument(final int minimum, final int maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public static WildcardIntegerArgument integer() {
        return integer(Integer.MIN_VALUE);
    }

    public static WildcardIntegerArgument integer(final int min) {
        return integer(min, Integer.MAX_VALUE);
    }

    public static WildcardIntegerArgument integer(final int min, final int max) {
        return new WildcardIntegerArgument(min, max);
    }

    /**
     * @return value of null specifies a wildcard
     */
    public static Integer getInteger(final CommandContext<?> context, final String name) {
        String argument = context.getArgument(name, String.class);
        return argument.equals("*") ? null : Integer.parseInt(argument);
    }

    public int getMinimum() {
        return minimum;
    }

    public int getMaximum() {
        return maximum;
    }

    @Override
    public Integer parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final int result = reader.readInt();
        if (result < minimum) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, result, minimum);
        }
        if (result > maximum) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, result, maximum);
        }
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WildcardIntegerArgument)) return false;

        final WildcardIntegerArgument that = (WildcardIntegerArgument) o;
        return maximum == that.maximum && minimum == that.minimum;
    }

    @Override
    public int hashCode() {
        return 31 * minimum + maximum;
    }

    @Override
    public String toString() {
        if (minimum == Integer.MIN_VALUE && maximum == Integer.MAX_VALUE) {
            return "wildcard_integer()";
        } else if (maximum == Integer.MAX_VALUE) {
            return "wildcard_integer(" + minimum + ")";
        } else {
            return "wildcard_integer(" + minimum + ", " + maximum + ")";
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}

