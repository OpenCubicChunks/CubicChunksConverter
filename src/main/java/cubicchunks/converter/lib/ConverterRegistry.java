package cubicchunks.converter.lib;

public class ConverterRegistry {
	private static final ISaveConverter[][] convertMapping = new ISaveConverter[SaveFormat.values().length][SaveFormat.values().length];

	static {
		registerIdentityConverters();
		registerConverters();
	}

	private static void registerIdentityConverters() {
		for (SaveFormat format : SaveFormat.values()) {
			register(format, format, new IdentityConverter());
		}
	}

	private static void registerConverters() {
		register(SaveFormat.VANILLA_ANVIL, SaveFormat.CUBIC_CHUNKS, new AnvilToCubicChunksConverter());
	}

	public static void register(SaveFormat src, SaveFormat dst, ISaveConverter converter) {
		convertMapping[src.ordinal()][dst.ordinal()] = converter;
	}

	public static ISaveConverter getConverter(SaveFormat src, SaveFormat dst) {
		return convertMapping[src.ordinal()][dst.ordinal()];
	}
}
