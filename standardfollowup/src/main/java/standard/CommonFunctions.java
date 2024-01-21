package standard;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class CommonFunctions {
	
	private static CommonFunctions INSTANCE;
	
	private static final String DEFAULT_DIR = "C:/temp/Standards";
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	
	private final Path STANDARDS_DIR;
	protected final Path pool;
	protected final String uriPrefix;
	protected final Path newFilesDir;
	
	public static CommonFunctions get() {
		return INSTANCE;
	}
	
	public CommonFunctions(String referenceArt, String uriPrefix, String[] args) throws IOException {
		STANDARDS_DIR = Paths.get(args.length == 0 ? DEFAULT_DIR : args[0]);
		this.pool = STANDARDS_DIR.resolve(referenceArt);
		this.uriPrefix = uriPrefix;
		String newDirName = df.format(new Date());
		newFilesDir = pool.resolve(newDirName);
		INSTANCE = this;
		Files.createDirectory(newFilesDir);
	}

	public boolean areFilesIdentical(File file1, File file2) throws IOException {
		return FileUtils.contentEquals(file1, file2);
	}

	protected String getFileName(URI uri) {
		String query = uri.getQuery();
		int posEqu = query.indexOf('=');
		return posEqu > 0 ? query.substring(posEqu + 1) : query ;
	}

	public void downloadFile(String fileUri)
			throws IOException, URISyntaxException {
		URI uri = new URI(fileUri.startsWith("http") ? fileUri : uriPrefix + fileUri);
		Path targetFile = getDestination(getFileName(uri));
		System.out.format("Download: %s to %s%n", fileUri, targetFile);
		FileUtils.copyURLToFile(uri.toURL(), targetFile.toFile());
	}
	
	protected String convertUriToFileName(String uriString) {
		return uriString
			.replace("://", "_")
			.replace('\\', '_')
			.replace('/', '_')
			.replace('?', '_')
			.replace('=', '_')
			.replace(':', '_')
			.replace(' ', '_')
			.replace("__", "_") + ".html";
	}
	
	protected Path getDestination(String fileName) {
		int counter = 0;
		Path path = Paths.get(fileName);
		Path targetFile = pool.resolve(newFilesDir).resolve(path);
		while (Files.exists(targetFile)) {
			path = Paths.get(String.format("%s_%03d", fileName, counter));
			targetFile = pool.resolve(newFilesDir).resolve(path);
			counter++;
		}
		return targetFile;
	}

	public Document downloadLink(String uriStringP) throws IOException {
		String uriString = uriStringP.startsWith("http") ? uriStringP : uriPrefix + uriStringP;
		final Document doc = Jsoup.connect(uriString).get();
	    String content = doc.text();
	    String fileName = convertUriToFileName(uriString);
	    Path destination = getDestination(fileName);
		System.out.format("Lookup: %s to %s%n", uriString, destination);
	    Files.write(destination, content.getBytes());
	    return doc;
	}

	public Path getNewFilesDir() {
		return newFilesDir;
	}

	public Path getPool() {
		return pool;
	}

}
