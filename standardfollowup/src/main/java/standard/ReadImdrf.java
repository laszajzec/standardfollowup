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

public class ReadImdrf {
	
	private List<String> references = new ArrayList<>();
	private CommonFunctions common;

	public void collect() {
		common = CommonFunctions.get();
		try {
			collectLinks();
			checkDocuments(references);
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}


	public void evaluate() throws IOException {
		common.getDestination(DownloadDir.CONTENT, "imdrf");
		common.compareDirectories(common.getNewFilesDir(), common.getLastDir());

		Path oldFilesDir = common.getLastDir();
		Path newFilesDir = common.getNewFilesDir();
		common.compareDirectories(oldFilesDir, newFilesDir);
		//TODO not finished yet
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
					System.out.println("Accepted " + linkAttr);
					uriRefs.add(linkAttr);
				} else {
					if (linkAttr.contains(".")) {
						System.out.println("Rejected " + linkAttr);
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

}
