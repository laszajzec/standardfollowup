package standard;

public class CheckPosition implements Cloneable{
	enum Reason {NEW, DIFFERENT, REWOKED, NOT_FOUND}
	
	private static CheckPosition instance;
	private String institute;
	private String uri;
	private String fileName;
	private String path;
	private String tag;
	private String what;
	private String expectedValue;
	private String currentValue;
	Reason reason;

	public static CheckPosition get() {
		if (instance == null) {
			instance = new CheckPosition();
		}
		return instance;
	}

	public Object clone() throws CloneNotSupportedException {
        return super.clone();
	}

	public String getInstitute() {
		return institute == null ? "" : institute;
	}

	public CheckPosition setInstitute(String institute) {
		this.institute = institute;
		return this;
	}

	public String getUri() {
		return uri == null ? "" : uri;
	}

	public CheckPosition setUri(String uri) {
		this.uri = uri;
		return this;
	}

	public String getFileName() {
		return fileName == null ? "" : fileName;
	}

	public CheckPosition setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getPath() {
		return path == null ? "" : path;
	}

	public CheckPosition setPath(String path) {
		this.path = path;
		return this;
	}

	public String getTag() {
		return tag == null ? "" : tag;
	}

	public CheckPosition setTag(String tag) {
		this.tag = tag;
		return this;
	}

	public String getWhat() {
		return what == null ? "" : what;
	}

	public CheckPosition setWhat(String what) {
		this.what = what;
		return this;
	}

	public String getExpectedValue() {
		return expectedValue == null ? "" : expectedValue;
	}

	public CheckPosition setExpectedValue(String expectedValue) {
		this.expectedValue = expectedValue;
		return this;
	}

	public String getCurrentValue() {
		return currentValue == null ? "" : currentValue;
	}

	public CheckPosition setCurrentValue(String currentValue) {
		this.currentValue = currentValue;
		return this;
	}

	public String getReason() {
		return reason == null ? "" : reason.toString();
	}

	public CheckPosition setReason(Reason reason) {
		this.reason = reason;
		return this;
	}
	
}
