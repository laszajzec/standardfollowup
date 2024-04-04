package standard;

public class CheckPosition {
	
	private static CheckPosition instance;
	String institute;
	String uri;
	String fileName;
	String path;
	String tag;
	String what;

	public static CheckPosition get() {
		if (instance == null) {
			instance = new CheckPosition();
		}
		return instance;
	}

}
