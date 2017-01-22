package cubicchunks.converter.lib;

public class Dimension {
	private final String name;
	private final String directory;

	public Dimension(String name, String directory) {
		this.name = name;
		this.directory = directory;
	}

	public String getName() {
		return this.name;
	}

	public String getDirectory() {
		return directory;
	}
}
