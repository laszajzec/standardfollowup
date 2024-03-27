package standard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CommonFunctions {
	
	public enum DownloadDir {CONTENT, SUMMARY, HTML}
	public enum DocumentEvent {NEW, CHANGED, REVOKED, NOT_FOUND}
	public static final SimpleDateFormat dateAndTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	
	private static CommonFunctions INSTANCE;
	private static final String PROTOCOLL_TXT = "%s/protocoll-%s.txt";
	private static final String JOURNAL_TXT = "journal.txt";
	private static final String COMPARATOR_CMD = "compare.cmd";
	private static final String DOWNLOAD_CONTENT_DIR = "files";
	private static final String DOWNLOAD_SUMMARY_DIR = "summary";
	private static final String DOWNLOAD_HTML_DIR = "html";
	
	
	private static final List<String> ISO_STANDARDS = Arrays.asList(new String[]
			{"4180","8429","8596","8612","9022-3","9801","10342","10343","10938","10993-1",
			"10993-5","10993-7","10993-18","11135","11137-1","11607-1","11607-2","11737-1","12866","12867",
			"13485","14971","15004-1","15223-1","15223-2","17664-1","17664-2","19011","19980","20417",
			"22665"
			,"15928-4"});
	
	
	private final Path baseDirPath;
	private Path newFilesDir;
	private boolean isDirectoryFreshCreated = false;
	private String checkingDate;
	private StringBuilder protocol = new StringBuilder();
	private String newDirName;
	private String oldDirName;
	private final LocalDate dateOfLastCheck;
	
	public static CommonFunctions get() {
		return INSTANCE;
	}
	
	public CommonFunctions(Path baseDir, LocalDate lastCheckDate) throws IOException {
		this.baseDirPath = baseDir.resolve("PublicHealth");
		if (lastCheckDate == null) {
			dateOfLastCheck = readLastJournalDate();
		} else {
			dateOfLastCheck = lastCheckDate;
		}
		checkingDate = dateAndTimeFormat.format(new Date());
		if (!Files.isDirectory(baseDirPath)) {
			System.out.println("Not correct directory: " + baseDirPath.toString());
			throw new FileNotFoundException(baseDirPath.toString());
		}
		INSTANCE = this;
	}
	
	public void createNewDir(String existingDir) throws IOException {
		if (existingDir == null) {
			newDirName = checkingDate;
			newFilesDir = baseDirPath.resolve(newDirName);
			Files.createDirectory(newFilesDir);
			isDirectoryFreshCreated = true;
		} else {
			newFilesDir = baseDirPath.resolve(existingDir);
			isDirectoryFreshCreated = false;
		}
	}
	
	public Path createSubdirectory(String dirName) throws IOException {
		Path subdir = newFilesDir.resolve(dirName);
		Files.createDirectory(subdir);
		return subdir;
	}
	
	public Path getLastDir() {
		List<File> subdirs = new ArrayList<>(Arrays.asList(getBaseDirPath().toFile().listFiles(f -> f.isDirectory())));
		Collections.sort(subdirs);
		if (!subdirs.isEmpty()) { 
			File latestDir = subdirs.getLast();
			Path oldDirPath = latestDir.toPath();
			String separator = File.separatorChar == '\\' ? "\\\\" : File.separator;
			String[] fileElements = latestDir.toString().split(separator);
			oldDirName = fileElements[fileElements.length - 1];
			return oldDirPath;
		} else { 
			return null;
		}
	}

	public boolean areFilesIdentical(File file1, File file2) throws IOException {
		if (file1.toString().endsWith(".pdf")) {
			String content1 = getPdfContent(file1.toPath());
			String content2 = getPdfContent(file2.toPath());
			if (content1.length() != content2.length()) 
				return false;
			if (!content1.equals(content2)) { 
				return false;
			}
			return true;
		} else {
			return FileUtils.contentEquals(file1, file2);
		}
	}

	public void compareDirectories(Path oldFilesDir, Path newFilesDir) throws IOException {

		List<String> newFiles = Arrays.asList(newFilesDir.toFile().listFiles()).stream().map(x -> x.getName()).collect(Collectors.toList());
		List<String> oldFiles = Arrays.asList(oldFilesDir.toFile().listFiles()).stream().map(x -> x.getName()).collect(Collectors.toList());
		if (newFiles.isEmpty()) {
			System.out.println("No files are downloded!");
			return;
		}
		Set<String> createdSince = new TreeSet<>(newFiles);
		createdSince.removeAll(oldFiles);
		Set<String> droppedSince = new TreeSet<>(oldFiles);
		droppedSince.removeAll(newFiles);
		droppedSince.remove(CommonFunctions.getProtocollTxt());
		Collection<String> keptSince = CollectionUtils.intersection(newFiles, oldFiles);
		List<String> differentFiles = new ArrayList<>();
		try (BufferedWriter logger =  Files.newBufferedWriter(newFilesDir.resolve(CommonFunctions.getProtocollTxt()))) {
			try (BufferedWriter comparator =  Files.newBufferedWriter(newFilesDir.resolve(CommonFunctions.getComparatorCmd()))) {
				comparator.append("@set comparator=\"c:\\Program Files\\PDF24\\pdf24-Toolbox.exe\" verb=compare ");
				comparator.newLine();
				for (String keptFile : keptSince) {
					Path oldFilePath = oldFilesDir.resolve(keptFile);
					File oldFile = oldFilePath.toFile();
					File newFile = newFilesDir.resolve(keptFile).toFile();
					if (areFilesIdentical(oldFile, newFile)) {
//						if (oldDirArg == null && newDirArg == null) {
//							Files.delete(oldFilePath);
//						}
					} else {
						differentFiles.add(keptFile);
						logger.append("cmp ");
						logger.append(oldFile.toString());
						logger.append(" ");
						logger.append(newFile.toString());
						comparator.append(String.format("%%comparator%% \"%s\" \"%s\"%n", oldFile.toString(), newFile.toString()));
						comparator.append("pause");
						comparator.newLine();
					}
				}
				logger.append(String.format("REM Comparing %s with %s%n", newFilesDir, oldFilesDir));
				printList(droppedSince, "Dropped", "No standard dropped!");
				printList(createdSince, "New standard", "No new standard!");
				printList(differentFiles, "Changed", "No changes!");
			}
		}
//		System.out.format("Compared %s with %s%n", newFilesDir, oldFilesDir);
	}

	private void printList(Collection<String> listOfFiles, String prefix, String emptyText) throws IOException {
		int counter = 1;
		for (String name : listOfFiles) {
			String text = String.format("%3d %s: %s%n", counter, prefix, name);
			appendProtocol(text);
			counter++;
		}
		System.out.println("");
	}
	
	public Path getOldEquivalent(File aFile) {
		return Paths.get(aFile.toString().replace(newDirName, oldDirName));
	}
	
	private String getFileName(String uriString) throws URISyntaxException {
		return getFileName(new URI(uriString));
	}

	private String getFileName(URI uri) {
		String query = uri.getQuery();
		String fileName;
		if (query == null) {
			String[] tokens = uri.toString().split("/");
			fileName = tokens[tokens.length - 1];
		} else {
			int posEqu = query.indexOf('=');
			fileName = posEqu > 0 ? query.substring(posEqu + 1) : query ;
		}
		return fileName.replace("%20", " ");
	}

//	private String convertUriToFileName(String uriString) {
//		return uriString
//			.replace("://", "_")
//			.replace('\\', '_')
//			.replace('/', '_')
//			.replace('?', '_')
//			.replace('=', '_')
//			.replace(':', '_')
//			.replace(' ', '_')
//			.replace("__", "_") + ".html";
//	}
	
	public Path getDestination(DownloadDir dir, String... subdirs) {
		return getDestination(getRelDir(dir), subdirs);
	}
	
	public Path getDestination(String dir, String... subdirs) {
		Path targetFile = baseDirPath
				.resolve(newFilesDir)
				.resolve(dir);
		for (String aSubdir : subdirs) {
			targetFile = targetFile.resolve(aSubdir);
		}
		return targetFile;
	}
	
	private Path getDestinationIntern(String fileName, String dir, String... subdirs) {
		int counter = 0;
		Path fileNamePath = Paths.get(fileName.replace("%20", " "));
		Path targetFile = getDestination(dir, subdirs).resolve(fileNamePath);
		while (Files.exists(targetFile)) {
			fileNamePath = Paths.get(String.format("%s_%03d", fileName, counter));
			targetFile = baseDirPath.resolve(newFilesDir).resolve(fileNamePath);
			counter++;
		}
		return targetFile;
	}
	
	private String getRelDir(DownloadDir dir) {
		if (DownloadDir.CONTENT == dir) return DOWNLOAD_CONTENT_DIR;
		if (DownloadDir.SUMMARY  == dir) return DOWNLOAD_SUMMARY_DIR;
		if (DownloadDir.HTML  == dir) return DOWNLOAD_HTML_DIR;
		return null;
	}
	
	public Path getFilePath(String fileName, DownloadDir dir) {
		return getDestinationIntern(fileName, getRelDir(dir));
	}

	public File downloadHtmlToFile(String uri, String fileName) throws IOException, URISyntaxException {
	    Path destination = getFilePath(fileName, DownloadDir.HTML);
		File file = destination.toFile();
		System.out.format("Download html: %s to %s%n", uri, file);
		FileUtils.copyURLToFile(new URI(uri).toURL(), file);
		return file;
	}
	
	public Document downloadFileLink(String uriStringP, String prefix, DownloadDir dir, String fileName) throws IOException, URISyntaxException {
		String uriString = uriStringP.startsWith("http") ? uriStringP : prefix + uriStringP;
	    Path destination = getFilePath(fileName, dir);
		System.out.format("Lookup: %s to %s%n", uriString, destination);
		final Document doc = Jsoup.connect(uriString).get();
		FileUtils.copyURLToFile(new URI(uriString).toURL(), destination.toFile());
	    return doc;
	}
	
	public File downloadFile(String fileUri, DownloadDir dir, String fileName) throws IOException, URISyntaxException {
		return downloadFileToSubs(fileUri, dir, fileName);
	}

	public File downloadFileToSubs(String fileUri, DownloadDir dir, String fileName, String... subdir) throws IOException, URISyntaxException {
		URI uri = new URI(fileUri);
		Path targetFileString = getDestinationIntern(fileName, getRelDir(dir), subdir);
		File targetFile = targetFileString.toFile();
		System.out.format("Download: %s to %s%n", fileUri, targetFile);
		FileUtils.copyURLToFile(uri.toURL(), targetFile);
		return targetFile;
	}

	public File downloadFileToSub(String fileUri, DownloadDir dir, String... subdirs) throws IOException, URISyntaxException {
		return downloadFileToSubs(fileUri, dir, getFileName(fileUri), subdirs);
//		URI uri = new URI(fileUri);
//		Path targetFileString = getDestination(getFileName(uri), getRelDir(dir));
//		File targetFile = targetFileString.toFile();
//		System.out.format("Download: %s to %s%n", fileUri, targetFile);
//		FileUtils.copyURLToFile(uri.toURL(), targetFile);
//		return targetFile;
	}


	public Path getNewFilesDir() {
		return newFilesDir;
	}
	
	public void setNewFilesDir(Path newFilesDir) {
		this.newFilesDir = newFilesDir;
	}

	public Path getBaseDirPath() {
		return baseDirPath;
	}
	
	public List<String> getIsoStandards() {
		return ISO_STANDARDS;
	}

	public boolean isDirectoryFreshCreated() {
		return isDirectoryFreshCreated;
	}

	public static String getProtocollTxt() {
		return PROTOCOLL_TXT;
	}

	public static String getJournalTxt() {
		return JOURNAL_TXT;
	}

	public static String getComparatorCmd() {
		return COMPARATOR_CMD;
	}
	
	public void appendJournal(String text) throws IOException {
		Files.writeString(baseDirPath.resolve(JOURNAL_TXT), text, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}
	
	public LocalDate readLastJournalDate() {
		List<String> journalLines;
		try {
			journalLines = Files.readAllLines(baseDirPath.resolve(JOURNAL_TXT));
		} catch (IOException e) {
			System.out.println(JOURNAL_TXT + " not found");
			return LocalDate.of(2022, 1, 1);
		}
		String lastLine = journalLines.get(journalLines.size() - 1);
		LocalDate lastDate = LocalDate.parse(lastLine.substring(22, 32), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		return lastDate;
	}
	
	
	public void appendProtocol(String text) {
		protocol.append(text);
	}

	public void appendProtocolLn(String text) {
		protocol.append(text);
		protocol.append(System.lineSeparator());
	}
	
	public void appendProtocol(DocumentEvent err, String id, String uri, String remark) {
		appendProtocol(String.format("%10s %10s %s", err, id, uri));
		appendProtocolLn(remark == null ? "" : remark);
	}
	
	public void storeProtokoll() throws IOException {
		File protokollFile = new File(String.format(PROTOCOLL_TXT, baseDirPath, checkingDate));
		FileUtils.writeStringToFile(protokollFile, protocol.toString(), Charset.forName("UTF-8"));
	}

	public LocalDate getDateOfLastCheck() {
		return dateOfLastCheck;
	}

	public String getPdfContent(Path inFilePath) throws FileNotFoundException, IOException {
		File pdfFile = inFilePath.toFile();
		try (PDDocument document = Loader.loadPDF(pdfFile))
		{
			PDFTextStripper pdfStripper = new PDFTextStripper();
			String text = pdfStripper.getText(document);
			return text;
		}
	}
	
	/*
	private String[] getPdfContentOld(Path inFilePath) throws FileNotFoundException, IOException {
		PDFTextStripper pdfStripper = null;
		PDDocument pdDoc = null;
		COSDocument cosDoc = null;
		File pdfFile = inFilePath.toFile();
		PDFParser parser = new PDFParser(new RandomAccessReadBufferedFile(pdfFile.toString()));
		parser.parse();
		cosDoc = parser.getDocument();
		pdfStripper = new PDFTextStripper();
		pdDoc = new PDDocument(cosDoc);
		String parsedText = pdfStripper.getText(pdDoc);
		pdDoc.close();
		return parsedText.split(pdfStripper.getLineSeparator());
	}
	*/

	public boolean checkWithId(WebDriver driver, String id, String value) {
		List<WebElement> elementsWithId = driver.findElements(By.id(id));
		if (elementsWithId.size() != 1) return false;
		return value.equals(elementsWithId.getFirst().getText());
	}

	public boolean checkWithClass(WebDriver driver, String className, String value) {
		List<WebElement> elementsWithClassName = driver.findElements(By.className(className));
		System.out.println("Size = " + elementsWithClassName.size());
		for (WebElement element : elementsWithClassName) {
			if (value.equals(element.getText())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean checkDocument(WebDriver driver, Function<String, By> searchCriteria, String selectionValue, String value) {
		List<WebElement> elements = driver.findElements(searchCriteria.apply(selectionValue));
		System.out.println("Size = " + elements.size());
		for (WebElement element : elements) {
			System.out.format(">%s< >%s< >%s<%n", element.getTagName(), element.getAttribute("id"), element.getText());
			if (value.equals(element.getText())) {
				return true;
			}
		}
		return false;
	}

	public boolean checkWithXpath(WebDriver driver, String xPath, String value) {
		List<WebElement> elementsWithXpath = driver.findElements(By.xpath(xPath));
		for (WebElement element : elementsWithXpath) {
			if (value.equals(element.getText())) {
				return true;
			}
		}
		return false;
	}

}
