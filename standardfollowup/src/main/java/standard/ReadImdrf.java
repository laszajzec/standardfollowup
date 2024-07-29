package standard;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import standard.CheckPosition.Reason;
import standard.CommonFunctions.DownloadDir;

public class ReadImdrf implements RegulatorySource {
	
	private List<String> references = new ArrayList<>();
	private CommonFunctions common;
	private CheckPosition pos;
	private boolean ok = true;

	public ReadImdrf() {
		common = CommonFunctions.get();
		pos = CheckPosition.get();
	}
	
	public void collect() {
		try {
			collectLinks();
//			checkDocuments(references);
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}


	public void evaluate() throws IOException {
//		Path newFilesDir1 = common.getDestination(DownloadDir.CONTENT, "imdrf");
//		Path oldFilesDir1 = common.getOldEquivalent(newFilesDir1).resolve("files").resolve("imdrf");
//		ok = common.compareDirectories(oldFilesDir1, newFilesDir1);

//		Path oldFilesDir = common.getLastDir();
//		Path newFilesDir = common.getNewFilesDir();
//		ok = common.compareDirectories(oldFilesDir, newFilesDir);
	}
	
	private void collectLinks() throws IOException, URISyntaxException {
		for (String anUri : collectSubpages("https://www.imdrf.org/documents/library?f%5B0%5D=type%3Atechnical_document")) {
			checkHtml1(anUri);
		}
		checkHtml2("https://www.imdrf.org/documents/ghtf-final-documents/ghtf-media-releases");
		checkHtml1("https://www.imdrf.org/consultations/closed-consultations");
		/*
		collectPrimaryLinksFromUri("https://www.imdrf.org/documents/library?f%5B0%5D=type%3Atechnical_document", x -> x.contains("/documents/"));
		collectPrimaryLinksFromUri("https://www.imdrf.org/documents/ghtf-final-documents", x -> x.contains("/ghtf/"));
		collectPrimaryLinksFromUri("https://www.imdrf.org/consultations/closed-consultations", x -> x.contains("/consultations/"));
		*/
	}
	
	private void collectPrimaryLinksFromUri(String anUri, Predicate<String> aFilter) throws IOException, URISyntaxException {
		List<String> uriRefs = collectLinksFromUri(anUri, aFilter);
		checkDocuments(uriRefs);
	}
	
	
	private List<String> collectLinksFromUri(String anUri, Predicate<String> aFilter) throws IOException {
		List<String> uriRefs = new ArrayList<>();
		final Document doc = Jsoup.connect(anUri).get();
		for (Element e : doc.select("a")) {
			String linkAttr = e.attr("href");
			if (!linkAttr.isEmpty()) {
				if (aFilter.test(linkAttr)) {
//					System.out.println("Accepted " + linkAttr);
					uriRefs.add(linkAttr);
				} else {
					if (linkAttr.contains(".")) {
						// ignore
//						System.out.println("Rejected " + linkAttr);
					}
				}
			}
		}
		return uriRefs;
	}
	
	private void checkHtml1(String anUri) throws IOException {
		final Document doc = Jsoup.connect(anUri).get();
		String dateValue;
		for (Element e : doc.select("time")) {
			dateValue = e.attr("datetime").trim();
			if (!dateValue.isEmpty()) {
				LocalDate htmlDate = LocalDate.parse(dateValue, DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()));
				if (common.isWithin(htmlDate)) {
					Element titleElement = e.parent().parent().parent().parent().previousElementSiblings().get(0).select("a").get(0);
					String title = titleElement.text();
					System.out.format("IMDRF date: %s doc: %s%n", dateValue, title);
					pos.setFileName(title);
					pos.setUri("https://www.imdrf.org" + titleElement.attr("href"));
					pos.setReason(Reason.DIFFERENT);
					pos.setChangeDate(htmlDate);
					common.appendProtocol(pos);
				}
			}
		}
	}
	
	private Collection<String> collectSubpages(String anUri) throws IOException {
		final Set<String> subpages = new LinkedHashSet<>();
		final String uriBase = anUri.substring(0, anUri.indexOf("?"));
		final Document doc = Jsoup.connect(anUri).get();
		for (Element e : doc.select("[class=\"page-link\"]")) {
			String ref = e.attr("href");
			int pos = ref.indexOf("page=");
			if (pos > 0) {
				subpages.add(uriBase + ref);
			}
		}
		return subpages;
	}

	private void checkHtml2(String anUri) throws IOException {
		final Document doc = Jsoup.connect(anUri).get();
		String dateValue;
		for (Element e : doc.select("div")) {
			dateValue = e.ownText().trim();
			if (dateValue.startsWith("Date posted")) {
				dateValue = dateValue.replace("Date posted:", "").trim();
				dateValue = Character.isDigit(dateValue.charAt(0)) ? dateValue : "1 " + dateValue;
				LocalDate issueDate = common.dateFromMonthName(dateValue);
				if (common.isWithin(issueDate)) {
					System.out.format("GHTF %s %s%n", issueDate, e.parent().select("[class=\"file-collection__header\"]").first().text());
					common.appendProtocol(String.format("GHTF %s %s%n", issueDate, e.parent().select("[class=\"file-collection__header\"]").first().text()));
				}
			}
		}
	}

	private void checkDocuments(List<String> references) throws IOException, URISyntaxException {
		for (String anUri : references) {
			checkDocument(anUri);
		}
	}
	
	Predicate<String> acceptedfileTypes = refP -> {
		String ref = refP.toLowerCase();
		return ref.endsWith(".docx") 
				|| ref.endsWith(".xlsx")
				|| ref.endsWith(".doc")
				|| ref.endsWith(".pdf");
	};
	
	private void checkDocument(String anUri) throws IOException, URISyntaxException {
		List<String> fileRefs = collectLinksFromUri("https://www.imdrf.org/" + anUri, acceptedfileTypes);
		for (String fileRef : fileRefs) {
			common.downloadFileToSub(fileRef, DownloadDir.CONTENT, "imdrf");
		}
	}

	@Override
	public void removeUnchanged() throws Exception {
	}

	@Override
	public boolean isOk() {
		return ok;
	}
	
}
