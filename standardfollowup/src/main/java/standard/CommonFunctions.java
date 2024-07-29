package standard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CommonFunctions {
	
	public enum DownloadDir {CONTENT, SUMMARY, HTML, XML}
//	public enum DocumentEvent {NEW, CHANGED, REVOKED, NOT_FOUND}
	public static final SimpleDateFormat dateAndTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	
	private static CommonFunctions INSTANCE;
	private static final String PROTOCOLL_TXT = "%s/protocol-%s.csv";
	private static final String JOURNAL_TXT = "journal.txt";
	private static final String COMPARATOR_CMD = "compare.cmd";
	private static final String DOWNLOAD_CONTENT_DIR = "files";
	private static final String DOWNLOAD_SUMMARY_DIR = "summary";
	private static final String DOWNLOAD_HTML_DIR = "html";
	private static final String DOWNLOAD_XML_DIR = "xml";
	
	private final Path baseDirPath;
	private boolean isDirectoryFreshCreated = false;
	private String checkingDate;
	private StringBuilder protocol = new StringBuilder();
	private String newDirName;
	private Path newDirPath;
	private String compareDirName;
	private Path compareDirPath;
	private final LocalDate dateOfCheckFrom;
	private final LocalDate dateOfCheckUntil;
	private boolean protocolHeadMissing = true;
	
	public static CommonFunctions get() {
		return INSTANCE;
	}
	
	public CommonFunctions(Path baseDir, LocalDate dateOfCheckFrom, LocalDate dateOfCheckUntil) throws IOException {
		this.baseDirPath = baseDir.resolve("PublicHealth");
		this.dateOfCheckFrom = dateOfCheckFrom == null ? dateOfCheckFrom = readLastJournalDate() : dateOfCheckFrom;
		this.dateOfCheckUntil = dateOfCheckUntil;
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
			newDirPath = baseDirPath.resolve(newDirName);
			Files.createDirectory(newDirPath);
			isDirectoryFreshCreated = true;
		} else {
			newDirPath = baseDirPath.resolve(existingDir);
			isDirectoryFreshCreated = false;
		}
	}
	
	public Path createSubdirectory(String dirName) throws IOException {
		Path subdir = newDirPath.resolve(dirName);
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
			compareDirName = fileElements[fileElements.length - 1];
			return oldDirPath;
		} else { 
			return null;
		}
	}

	public void setCompareDir(String oldDirArg) throws FileNotFoundException {
		compareDirName = oldDirArg;
		compareDirPath = getBaseDirPath().resolve(oldDirArg);
		if (!Files.isDirectory(compareDirPath)) {
			System.out.println("Not correct directory: " + oldDirArg);
			throw new FileNotFoundException(oldDirArg);
		}
	}
	
	public boolean isWithin(LocalDate aDate) {
		return !aDate.isBefore(dateOfCheckFrom) && !aDate.isAfter(dateOfCheckUntil);
	}
	
	public static LocalDate toDate(String aDate, String aFormat) {
		try {
			return LocalDate.parse(aDate, DateTimeFormatter.ofPattern(aFormat));
		} catch (DateTimeParseException e) {
			System.out.format("Wrong date format %s, use %s%n", aDate, aFormat);
			return null;
		}
	}

	public boolean areFilesIdentical(File file1, File file2) throws IOException {
		if (!file1.exists() || !file2.exists()) {
			return false;
		}
		if (file1.toString().endsWith(".pdf")) {
			/*
			String content1 = getPdfContent(file1.toPath());
			String content2 = getPdfContent(file2.toPath());
			if (content1.length() != content2.length()) 
				return false;
			if (!content1.equals(content2)) { 
				return false;
			}
			return true;
			*/
			return FileUtils.contentEquals(file1, file2);
		} else {
			return FileUtils.contentEquals(file1, file2);
		}
	}

	public boolean compareDirectories(Path oldFilesDir, Path newFilesDir) throws IOException {

//		List<String> newFiles = Arrays.asList(newFilesDir.toFile().listFiles()).stream().map(x -> x.getName()).collect(Collectors.toList());
//		List<String> oldFiles = Arrays.asList(oldFilesDir.toFile().listFiles()).stream().map(x -> x.getName()).collect(Collectors.toList());
		
		List<String> newFiles = Files.walk(newFilesDir).map(x -> x.toFile().getName()).collect(Collectors.toList());
		List<String> oldFiles = Files.walk(oldFilesDir).map(x -> x.toFile().getName()).collect(Collectors.toList());
		
		boolean ok = true;
		if (newFiles.isEmpty()) {
			System.out.println("No files are downloded!");
			return ok;
		}
		Set<String> addedNewDocuments = new TreeSet<>(newFiles);
		addedNewDocuments.removeAll(oldFiles);
		Set<String> droppedDocuments = new TreeSet<>(oldFiles);
		droppedDocuments.removeAll(newFiles);
		droppedDocuments.remove(CommonFunctions.getProtocollTxt());
		Collection<String> keptSince = CollectionUtils.intersection(newFiles, oldFiles);
		List<String> differentFiles = new ArrayList<>();
		try (BufferedWriter comparator =  Files.newBufferedWriter(newFilesDir.resolve(CommonFunctions.getComparatorCmd()))) {
			comparator.append("@set comparator=\"c:\\Program Files\\PDF24\\pdf24-Toolbox.exe\" verb=compare ");
			comparator.newLine();
			for (String keptFile : keptSince) {
				Path oldFilePath = oldFilesDir.resolve(keptFile);
				File oldFile = oldFilePath.toFile();
				File newFile = newFilesDir.resolve(keptFile).toFile();
				if (!areFilesIdentical(oldFile, newFile)) {
					differentFiles.add(keptFile);
					appendProtocol("cmp ");
					appendProtocol(oldFile.toString());
					appendProtocol(" ");
					appendProtocolLn(newFile.toString());
					comparator.append(String.format("%%comparator%% \"%s\" \"%s\"%n", oldFile.toString(), newFile.toString()));
					comparator.append("pause");
					comparator.newLine();
					ok = false;
				}
			}
//			logger.append(String.format("REM Comparing %s with %s%n", newFilesDir, oldFilesDir));
			printList(droppedDocuments, "Dropped", "No standard dropped!");
			printList(addedNewDocuments, "New standard", "No new standard!");
			printList(differentFiles, "Changed", "No changes!");
		}
		//		System.out.format("Compared %s with %s%n", newFilesDir, oldFilesDir);
		return ok;
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
		return Paths.get(aFile.toString().replace(newDirName, compareDirName));
	}

	public Path getOldEquivalent(Path aFile) {
		return Paths.get(compareDirName);
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
				.resolve(newDirPath)
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
		while (true) {
			boolean found = false;
			try {
				found = Files.exists(targetFile);
			} catch (Exception ex) {
				// do nothing
			}
			if (!found) break;
			fileNamePath = Paths.get(String.format("%s_%03d", fileName, counter));
			targetFile = baseDirPath.resolve(newDirPath).resolve(fileNamePath);
			counter++;
		}
		return targetFile;
	}
	
	private String getRelDir(DownloadDir dir) {
		if (DownloadDir.CONTENT == dir) return DOWNLOAD_CONTENT_DIR;
		if (DownloadDir.SUMMARY  == dir) return DOWNLOAD_SUMMARY_DIR;
		if (DownloadDir.HTML  == dir) return DOWNLOAD_HTML_DIR;
		if (DownloadDir.XML  == dir) return DOWNLOAD_XML_DIR;
		return null;
	}
	
	public Path getFilePath(String fileName, DownloadDir dir) {
		return getDestinationIntern(fileName, getRelDir(dir));
	}

	public File downloadHtmlToFile(String uri, String fileName) throws IOException, URISyntaxException {
		return downloadHtmlToFile(uri, getFilePath(fileName, DownloadDir.HTML));
	}

	public static File downloadHtmlToFile(String uri, Path filePath) throws IOException, URISyntaxException {
		File file = filePath.toFile();
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
		try {
			FileUtils.copyURLToFile(uri.toURL(), targetFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		return newDirPath;
	}
	
	public void setNewFilesDir(Path newFilesDir) {
		this.newDirPath = newFilesDir;
	}

	public Path getBaseDirPath() {
		return baseDirPath;
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
	
	public void appendProtocol(CheckPosition.Reason err, String id, String uri, String remark) {
		appendProtocol(String.format("%10s %10s %s", err, id, uri));
		appendProtocolLn(remark == null ? "" : remark);
	}

	public void appendProtocol(CheckPosition.Reason err, String id, String uri, String what, String expectedValue, String currentValue) {
		appendProtocol(String.format("%10s %10s %s %s: (%s <-> %s)", err, id, uri, what, expectedValue, currentValue));
	}
	
	public void appendProtocol(CheckPosition pos) {
		if (protocolHeadMissing) {
			appendProtocolLn("Institute;Id;Date;File;URI;Reason;Tag;Path;What;Expected;Found");
			protocolHeadMissing = false;
		}
		StringBuilder txt = new StringBuilder();
		txt.append("\"");
		txt.append(pos.getInstitute());
		txt.append("\";\"");
		txt.append(pos.getId());
		txt.append("\";");
		txt.append(pos.getChangeDate());
		txt.append(";\"");
		txt.append(pos.getFileName());
		txt.append("\";\"");
		txt.append(pos.getUri());
		txt.append("\";\"");
		txt.append(pos.getReason());
		txt.append("\";\"");
		txt.append(pos.getTag());
		txt.append("\";\"");
		txt.append(pos.getPath());
		txt.append("\";\"");
		txt.append(pos.getWhat());
		txt.append("\";\"");
		txt.append(pos.getExpectedValue());
		txt.append("\";\"");
		txt.append(pos.getCurrentValue());
		txt.append("\"");
		appendProtocolLn(txt.toString());
	}

	public void storeProtokoll() throws IOException {
		File protokollFile = new File(String.format(PROTOCOLL_TXT, baseDirPath, checkingDate));
		FileUtils.writeStringToFile(protokollFile, protocol.toString(), Charset.forName("UTF-8"));
	}

	public LocalDate getDateOfLastCheck() {
		return dateOfCheckFrom;
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

	public boolean checkFiles(File[] files, String uri) throws IOException {
		boolean ok = true;
		for (File newFile : files) {
			Path oldFile = getOldEquivalent(newFile);
			if (Files.exists(oldFile)) {
				if (!areFilesIdentical(newFile, oldFile.toFile())) {
//					CommonFunctions.get().appendProtocol(CommonFunctions.DocumentEvent.CHANGED, newFile.getName(), uri, null);
					CheckPosition pos = CheckPosition.get();
					pos.setReason(CheckPosition.Reason.DIFFERENT);
					pos.setFileName(newFile.toString());
					CommonFunctions.get().appendProtocol(pos);
					ok = false;
				}
			} else {
//				CommonFunctions.get().appendProtocol(CommonFunctions.DocumentEvent.NEW, newFile.getName(), uri, null);
				CheckPosition pos = CheckPosition.get();
				pos.setReason(CheckPosition.Reason.NEW);
				pos.setFileName(newFile.toString());
				CommonFunctions.get().appendProtocol(pos);
				ok = false;
			}
		}
		return ok;
	}

	public void reduceOldDir() throws IOException {
		Files.walkFileTree(newDirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            	Path oldFile = getOldEquivalent(file);
            	if (oldFile != null && areFilesIdentical(file.toFile(), oldFile.toFile())) {
            		Files.delete(oldFile);
            	}
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                System.err.println(file + "\terror: " + exc);
                return FileVisitResult.CONTINUE;
            }
		});
	}
	
	public static List<String> buildClientList(String clientString) {
		String uniform = clientString.replace(";", ",");
		uniform = clientString.replace(" ", ",");
		String[] tokens = uniform.split(",");
		return Arrays.asList(tokens);
	}
	
	private final Pattern dateErr1 = Pattern.compile(".*\\w\\d\\d\\d\\d");
	public LocalDate dateFromMonthName(String dateValue) {
		try {
			String modifiedDateValue = dateValue.trim();
			if (dateErr1.matcher(modifiedDateValue).matches()) {
				modifiedDateValue = modifiedDateValue.substring(0, modifiedDateValue.length() - 4) + " " + modifiedDateValue.substring(modifiedDateValue.length() - 4);
			}
			modifiedDateValue = Character.isDigit(modifiedDateValue.charAt(0)) ? modifiedDateValue : "1 " + modifiedDateValue;
			LocalDate convertedDate = LocalDate.parse(modifiedDateValue, DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(Locale.ENGLISH).withZone(ZoneId.systemDefault()));
			return convertedDate;
		} catch (Exception e) {
			System.out.format("Illegal date format %e for %s", e, dateValue);
			return null;
		}
	}
	
	public static List<Element> getElements(Node aNode) {
		NodeList children = aNode.getChildNodes();
		List<Element> filtered = new ArrayList<>();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				filtered.add((Element)child);
			}
		}
		return filtered;
	}

	
}
