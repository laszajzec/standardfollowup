package standard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class CommonFunctions {
	
	private static CommonFunctions INSTANCE;
	
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	
	protected final Path pool;
	protected final String uriPrefix;
	protected Path newFilesDir;
	
	public static CommonFunctions get() {
		return INSTANCE;
	}
	
	public CommonFunctions(String referenceArt, String uriPrefix, Path baseDir) throws IOException {
		this.pool = baseDir.resolve(referenceArt);
		if (!Files.isDirectory(pool)) {
			System.out.println("Not correct directory: " + pool.toString());
			throw new FileNotFoundException(pool.toString());
		}
		this.uriPrefix = uriPrefix;
		INSTANCE = this;
	}
	
	public void createNewDir() throws IOException {
		String newDirName = df.format(new Date());
		newFilesDir = pool.resolve(newDirName);
		Files.createDirectory(newFilesDir);
	}
	
	public Path getLastButOneDir() {
		List<File> subdirs = new ArrayList<>(Arrays.asList(getPool().toFile().listFiles()));
		Collections.sort(subdirs);
		return subdirs.size() > 1 ? subdirs.get(subdirs.size()-2).toPath() : null;
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
