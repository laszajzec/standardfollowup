package standard;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import standard.CommonFunctions.DownloadDir;

public class ReadImdrf implements RegulatorySource {
	
	private List<String> references = new ArrayList<>();
	private CommonFunctions common;
	private boolean ok = true;

	public ReadImdrf() {
		common = CommonFunctions.get();
	}
	
	public void collect() {
		try {
			collectLinks();
			checkDocuments(references);
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}


	public void evaluate() throws IOException {
		Path newFilesDir1 = common.getDestination(DownloadDir.CONTENT, "imdrf");
		Path oldFilesDir1 = common.getOldEquivalent(newFilesDir1);
		ok = common.compareDirectories(oldFilesDir1, newFilesDir1);

//		Path oldFilesDir = common.getLastDir();
//		Path newFilesDir = common.getNewFilesDir();
//		ok = common.compareDirectories(oldFilesDir, newFilesDir);
	}
	
	private void collectLinks() throws IOException, URISyntaxException {
		collectLinksFromUri("https://www.imdrf.org/documents/library?f%5B0%5D=type%3Atechnical_document", x -> x.contains("/documents/"));
		collectLinksFromUri("https://www.imdrf.org/documents/ghtf-final-documents", x -> x.contains("/ghtf/"));
		collectPrimaryLinksFromUri("https://www.imdrf.org/consultations/closed-consultations", x -> x.contains("/consultations/"));
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
