package standard.ec.publichealth;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import standard.CommonFunctions;

public class ReadPublicHealth {

	private Set<String> uriAlreadyDownloaded = new HashSet<>();
	private CommonFunctions common;

	List<String> ignore = Arrays.asList(new String[] {"#main-content", "#MainContent", "https://commission.europa.eu/index_\\w\\w", 
			"https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_.*",
			"/index_\\w\\w", "/medical-devices-sector_en", "/medical-devices-sector/new-regulations_\\w\\w",
			""}); 
	
	public void downloadFiles() throws IOException, URISyntaxException {
		String blogUrl = "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en";
		common = CommonFunctions.get();
		Document doc = Jsoup.connect(blogUrl).get();
		recurseLink(doc);
	}
	
	private void recurseLink(Document doc) throws IOException, URISyntaxException {
		List<String> fileRefs = new ArrayList<>();
		List<String> uriRefs = new ArrayList<>();
		Elements links = doc.select("a");
		for (Element e : links) {
			String linkAttr = e.attr("href");
			if (linkAttr.isEmpty() || ignore.stream().anyMatch(pattern -> linkAttr.matches(pattern))) {
				if (!linkAttr.isEmpty()) System.out.println("Ignore: " + linkAttr);
			} else  if (linkAttr.contains("/document/download/")) {
				fileRefs.add(linkAttr);
			} else {
				uriRefs.add(linkAttr);
			}
		}
		downloadFiles(fileRefs);
//		downloadLinks(uriRefs);
	}
	
	private void downloadFiles(List<String> fileRefs) throws IOException, URISyntaxException {
		for (String fileRef : fileRefs) {
			common.downloadFile(fileRef);
		}
	}

	private void downloadLinks(List<String> uriRefs) throws IOException, URISyntaxException {
		for (String uriRef : uriRefs) {
			if (!uriAlreadyDownloaded.contains(uriRef)) {
				uriAlreadyDownloaded.add(uriRef);
				final Document doc = common.downloadLink(uriRef);
				recurseLink(doc);
			}
		}
	}

}
