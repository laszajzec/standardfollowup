package standard;

import java.time.LocalDate;
import java.util.Objects;

public class CheckPosition implements Cloneable{
	public enum Reason {NEW, DIFFERENT, REWOKED, NOT_FOUND, UNCERTAIN}
	
	private static CheckPosition instance;
	private String institute;		// Regulatory issuer: ISO, DIN, IEC, ...
	private String id; 				// Name or sign of norm
	private String uri;				// Where to find 
	private String fileName;		// Document if stored
	private String fileLink;		// Document in web
	private String path;			// Element within page
	private String tag;				// Tag name within page
	private String what;
	private String expectedValue;
	private String currentValue;	// Found value
	private Reason reason;			// Art of change
	private LocalDate changeDate;	// Validity date

	public static CheckPosition get() {
		if (instance == null) {
			instance = new CheckPosition();
		}
		return instance;
	}

	public CheckPosition clone() {
		try {
        return (CheckPosition)super.clone();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void clearAll() {
		institute = null;
		id = null;
		uri = null; 
		fileName = null;
		fileLink = null;
		path = null;
		tag = null;
		what = null;
		expectedValue = null;
		currentValue = null;
		reason = null;
		changeDate = null;
	}
	
	

	@Override
	public int hashCode() {
		return Objects.hash(institute, id, fileLink, fileName, path);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CheckPosition other = (CheckPosition) obj;
		return 
			Objects.equals(institute, other.institute)
			&& Objects.equals(id, other.id) 
			&& Objects.equals(fileLink, other.fileLink) 
			&& Objects.equals(fileName, other.fileName)
			&& Objects.equals(path, other.path);
	}

	public String getInstitute() {
		return institute == null ? "" : institute;
	}

	public CheckPosition setInstitute(String institute) {
		clearAll();
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

	public static CheckPosition getInstance() {
		return instance;
	}

	public static void setInstance(CheckPosition instance) {
		CheckPosition.instance = instance;
	}

	public String getId() {
		return id == null ? "" : id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFileLink() {
		return fileLink == null ? "" : fileLink;
	}

	public void setFileLink(String fileLink) {
		this.fileLink = fileLink;
	}

	public LocalDate getChangeDate() {
		return changeDate;
	}

	public void setChangeDate(LocalDate changeDate) {
		this.changeDate = changeDate;
	}
	
}
