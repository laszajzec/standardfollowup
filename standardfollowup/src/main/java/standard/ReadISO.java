package standard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import standard.CommonFunctions.DocumentEvent;

public class ReadISO implements RegulatorySource {

	private static final String UPDATE_LIST_URI = "https://www.iso.org/iso-update.html";
	private static final String DOWNLOAD_PREFIX = "https://www.iso.org/";
	private static final String[] PREFIXES = new String[]{"/TS", "/TR", "/PAS", "/IEC TR", "/IEC TS", "/IEC", "/ASTM", "/PRF", "/IEEE"};
	private static final Pattern ISO_DATE_PATTERN = Pattern.compile("ISOupdate[\\s-](\\w+)[\\s-](\\d+)\\..*");

	private final CommonFunctions common;
	private List<String> ignore = new ArrayList<>();
	private List<Deviation> changedStandards = new ArrayList<>();
	private List<Deviation> withdrawnedStandards = new ArrayList<>();
	private List<Path> fileList;
	
	public ReadISO() {
		common = CommonFunctions.get();
	}
	
	@Override
	public void collect() {
		try {
			fileList = downloadFiles();
		} catch (IOException | URISyntaxException e) {
			System.out.println("Exception in ReadISO collect: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	public void removeUnchanged() {
	}

	@Override
	public void evaluate() {
		try {
			checkFiles(fileList);
		} catch (IOException e) {
			System.out.println("Exception in ReadISO evaluate: " + e.getMessage());
			e.printStackTrace();
		}
		writeToProtocol(changedStandards, DocumentEvent.CHANGED);
		writeToProtocol(withdrawnedStandards, DocumentEvent.REVOKED);
		System.out.format("Result: %d standards changed, %d withdrawn%n", changedStandards.size(), withdrawnedStandards.size());
	}

	private List<Path> downloadFiles() throws IOException, URISyntaxException {
		List<Path> downloadedFiles = new ArrayList<>();
		List<String> pdfRefs = collectDocumentUri();
//		common.createSubdirectory("iso");
		for (String fileRefAsUri : pdfRefs) {
			if (newerAsLast(fileRefAsUri)) {
			File downloadedFile = common.downloadFileToSub(DOWNLOAD_PREFIX + fileRefAsUri, CommonFunctions.DownloadDir.SUMMARY, "iso");
			downloadedFiles.add(downloadedFile.toPath());
			}
		}
		return downloadedFiles;
	}
	
	private List<String> collectDocumentUri() throws IOException {
		Document doc = Jsoup.connect(UPDATE_LIST_URI).get();
		List<String> uriRefs = new ArrayList<>();
		Elements links = doc.select("a");
		for (Element e : links) {
			String linkAttr = e.attr("href");
			//			System.out.println(linkAttr);
			if (linkAttr.isEmpty() || ignore.stream().anyMatch(pattern -> linkAttr.matches(pattern))) {
				if (!linkAttr.isEmpty()) System.out.println("Ignore: " + linkAttr);
			} else {
				if (linkAttr.endsWith(".pdf") && isNewLink(e, common.getDateOfLastCheck())) {
					uriRefs.add(linkAttr);
				}
			}
		}
		return uriRefs;
	}
	
	private boolean isNewLink(Element pdfLink, LocalDate dateOflastCheck) {
		String title = pdfLink.attr("title");
		Matcher m = ISO_DATE_PATTERN.matcher(title);
		if (m.matches()) {
			String month = m.group(1);
			String year = m.group(2);
			LocalDate issueDate = LocalDate.of(Integer.parseInt(year), Month.valueOf(month.toUpperCase()), 1);
			return issueDate.isAfter(dateOflastCheck);
		} else {
			System.out.println("No date match on ISO " + title);
		}
		return false;
	}
	
	private void checkFiles(List<Path> downloadedFiles) throws FileNotFoundException, IOException {
		for (Path aFile : downloadedFiles) {
			checkPdf(aFile);
		}
	}
	
	private void checkPdf(Path inFile) throws FileNotFoundException, IOException {
		System.out.println("------- Checking " + inFile);
		String contentRaw = common.getPdfContent(inFile);
		List<String> content = Arrays.asList(contentRaw.split("\\R"));

		// Search start of capitel
		int startIndexChanged = findTextInPdf(content, "Standards published", 0);
		if (startIndexChanged < 0) {
			System.out.println("Capitel Standards published not found");
			return;
		}
		int endIndexChanged = findTextInPdf(content, "Standards confirmed", startIndexChanged);
		if (endIndexChanged < 0) {
			System.out.println("Capitel Standards confirmed not found");
			endIndexChanged = content.size() - 1;
		}
		int startIndexWithdrawn = findTextInPdf(content, "Standards withdrawn", endIndexChanged);
		if (startIndexWithdrawn < 0) {
			System.out.println("No document has been withdrawn");
		}
		int endIndexWithdrawn = content.size();
		
		System.out.format("Line number changed:   %d end: %d%n", startIndexChanged, endIndexChanged);
		System.out.format("Line number withdrawn: %d end: %d%n", startIndexWithdrawn, endIndexWithdrawn);
		
		selectIsoNumbers(startIndexChanged, endIndexChanged, content, changedStandards, inFile);
		selectIsoNumbers(startIndexWithdrawn, endIndexWithdrawn, content, withdrawnedStandards, inFile);
	}
	
	private void selectIsoNumbers(int startIndex, int endIndex, List<String> content, List<Deviation> result, Path inFile) {
		boolean continuationExpectedInNextLine = false;
		for (int i = startIndex; i < endIndex; i++) {
			String line = content.get(i).trim();
			if (continuationExpectedInNextLine && !line.isEmpty() && Character.isDigit(line.charAt(0))) {
				if (canBeIsoNr(line)) {
					result.add(new Deviation(line, inFile));
				}
				continuationExpectedInNextLine = false;
			} else if (line.startsWith("ISO") || line.startsWith("IWA") || line.startsWith("IEC")) {
				String rest = line.substring(3).trim();
				rest = removeDeliveryId(rest, PREFIXES);
				if (rest.isEmpty()) {
					continuationExpectedInNextLine = true;
				} else if (Character.isDigit(rest.charAt(0))) {
					if (i < endIndex - 2) {
						String nextLine = content.get(i+1).trim();
						if (!nextLine.isEmpty() && Character.isDigit(nextLine.charAt(0))) {
							rest += nextLine;
							i++;
						}
					}
					if (canBeIsoNr(rest)) {
						result.add(new Deviation(rest, inFile));
					}
					continuationExpectedInNextLine = false;
				}
			} else {
				if (!content.get(i).trim().isEmpty() && content.get(i).matches(".*\\d\\d\\d\\d.*")) {
					System.out.format("                  Unknown line: %4d %s%n ", i, content.get(i));
				}
			}
		}
	}
	
	private int findTextInPdf(List<String> content, String text, int startIndex) {
		while (startIndex < content.size()) {
			if (content.get(startIndex).contains(text)) {
				return startIndex;
			}
			startIndex++;
		}
		return -1;	
	}
	
	private boolean canBeIsoNr(String line) {
		int posOfColon = line.indexOf(":");
		String nr = (posOfColon >= 0) ? line.substring(0, posOfColon) : line;
		boolean isRelevant = common.getIsoStandards().contains(nr);
		System.out.println((isRelevant ? " + " : "   ") + nr);
		return isRelevant;
	}
	
	private String removeDeliveryId(String line, String[] prefixes) {
		String res = line;
		for (String pref : prefixes) {
			if (res.startsWith(pref)) res = res.substring(pref.length());
		}
		return res.trim();
	}
	
	private void writeToProtocol(List<Deviation> numbers, DocumentEvent reason) {
		for (Deviation dev : numbers) {
			common.appendProtocol(reason, dev.getIsoNumber(), dev.getDocumentName(), null);
		}
	}
	
	private LocalDate convertFileName(String orig) {
		// ISOupdate month-year.pdf
		String modified = orig.replace("ISOupdate", "").replace(".pdf", "");
		String year = modified.substring(modified.length() - 4);
		modified = modified.replaceFirst(year, "");
		modified = modified.substring(1, modified.length()-1).trim();
		Month month = Month.valueOf(modified.toUpperCase());
		return LocalDate.of(Integer.parseInt(year), month, 1);
	}
	
	private boolean newerAsLast(String filename) {
		Path aPath = Paths.get(filename);
		Path aFilename = aPath.getFileName();
		String toConvert = aFilename.toString().replace("%20", " ");
		LocalDate fileDate = convertFileName(toConvert);
		return fileDate.isAfter(common.getDateOfLastCheck());
	}
	
	private static class Deviation {
		private final String isoNumber;
		private final Path inDocument;
		public Deviation(String isoNumber, Path inDocument) {
			this.isoNumber = isoNumber;
			this.inDocument = inDocument;
		}
		public String getIsoNumber() {
			return isoNumber;
		}
		public String getDocumentName() {
			return inDocument.getFileName().toString();
		}
	};
}
