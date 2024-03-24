package standard.ec.publichealth;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import standard.CommonFunctions;
import standard.RegulatorySource;

public class ReadPublicHealth implements RegulatorySource {

	private static final String blogUrl = "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en";
	private static final String prefix = "https://health.ec.europa.eu";

	private CommonFunctions common;

	List<String> ignore = Arrays.asList(new String[] {"#main-content", "#MainContent", "https://commission.europa.eu/index_\\w\\w", 
			"https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_.*",
			"/index_\\w\\w", "/medical-devices-sector_en", "/medical-devices-sector/new-regulations_\\w\\w",
			".*covid-19.*"
			}); 
	

	@Override
	public void collect() {
		common = CommonFunctions.get();
		Document doc;
		try {
			doc = Jsoup.connect(blogUrl).get();
			recurseLink(doc);
		} catch (IOException | URISyntaxException e) {
			System.out.println("Exception in ReadPublicHealth collect: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void removeUnchanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void evaluate() {
		// TODO Auto-generated method stub
		
	}
	private void recurseLink(Document doc) throws IOException, URISyntaxException {
		List<String> fileRefs = new ArrayList<>();
		List<String> uriRefs = new ArrayList<>();
		Elements links = doc.select("a");
		for (Element e : links) {
			String linkAttr = e.attr("href");
			System.out.println(linkAttr);
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
//		common.createSubdirectory("pubhealth");
		for (String fileRef : fileRefs) {
			String corrected = fileRef.startsWith("http") ? fileRef : prefix + fileRef;
			common.downloadFileToSub(corrected, CommonFunctions.DownloadDir.CONTENT, "pubhealth");
		}
	}

}
