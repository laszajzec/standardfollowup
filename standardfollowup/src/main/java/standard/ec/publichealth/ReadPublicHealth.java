package standard.ec.publichealth;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import standard.CheckPosition;
import standard.CommonFunctions;
import standard.RegulatorySource;

public class ReadPublicHealth implements RegulatorySource {

	private static final String blogUrl = "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en";
	private static final String prefix = "https://health.ec.europa.eu";

	private final CommonFunctions common;
	private final CheckPosition pos;
	private List<File> files = new ArrayList<>();
	private boolean ok = true;
	private Set<CheckPosition> foundElements = new HashSet<>();

	List<String> ignore = Arrays.asList(new String[] {"#main-content", "#MainContent", "https://commission.europa.eu/index_\\w\\w", 
			"https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_.*",
			"/index_\\w\\w", "/medical-devices-sector_en", "/medical-devices-sector/new-regulations_\\w\\w",
			".*covid-19.*"
			}); 
	
	public ReadPublicHealth() {
		common = CommonFunctions.get();
		pos = CheckPosition.get();
		pos.setReason(CheckPosition.Reason.DIFFERENT);
	}

	@Override
	public void collect() {
		try {
			Document doc = Jsoup.connect(blogUrl).get();
			findTableLinks(doc);
//			recurseLink(doc);
		} catch (IOException /*| URISyntaxException */ e) {
			System.out.println("Exception in ReadPublicHealth collect: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void removeUnchanged() {
	}

	@Override
	public void evaluate() throws IOException {
		ok = common.checkFiles(files.toArray(new File[0]), blogUrl);
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
			String corrected = fileRef.startsWith("http") ? fileRef : prefix + fileRef;
			files.add(common.downloadFileToSub(corrected, CommonFunctions.DownloadDir.CONTENT, "pubhealth"));
		}
	}

	@Override
	public boolean isOk() {
		return ok;
	}
	
	private final Set<String> excludedLinks = Set.of("Arabic", "Chinese", "Japanese", "Russian");
	private boolean toExclude(String text) {
		return excludedLinks.contains(text.trim());
	}
	
	private void findTableLinks(Document doc) {
		for (Element e : doc.select("a[href]")) {
			if (isWordReference(e)) {
//				System.out.format("Ignored, word version:  %s%n", e);
				continue;
			}
			Element tdParent = upToParent(e, "td");
			if (tdParent == null) {
//				System.out.format("Ignored, no td:  %s%n", e);
				continue;
			}
			Element trParent = upToParent(tdParent, "tr");
			if (trParent == null) {
//				System.out.format("Ignored, no tr:  %s%n", e);
				continue;
			}
			Elements links = tdParent.select("a[href]");
			links = new Elements(links.stream().filter(l -> !isWordReference(l)).collect(Collectors.toList()));
			if (links.size() == 1) {
				// normal case
				Element pub = (Element)trParent.lastChild();
				LocalDate issued = common.dateFromMonthName(pub.text());
				if (issued != null && common.isWithin(issued) && !toExclude(e.text())) {
					System.out.format("+ %s %s (%s)%n", e.text(), e.attr("href"), pub == null ? "?" : pub.text());
					pos.setId(e.text());
					pos.setUri(e.attr("href"));
					pos.setChangeDate(issued);
					if (!foundElements.contains(pos)) {
						foundElements.add(pos.clone());
						common.appendProtocol(pos);
					}
				};
			} else {
				checkMoreLines(trParent, links);
			}
		}
	}

	private void checkMoreLines(Element row, Elements links) {
		Element dates = row.lastElementChild();
		List<String> datesText = new ArrayList<>();
		List<Node> datesNodes = dates.childNodes().stream().filter(n -> !"<br>".equals(n.toString())).toList();
		for (int i = 0; i < datesNodes.size(); i++) {
			Node aNode = datesNodes.get(i);
			if (aNode instanceof Element) datesText.add(((Element)aNode).text());
			else if (aNode instanceof TextNode) datesText.add(((TextNode)aNode).text());
			else System.out.println("Unknown node type: " + aNode.getClass().getName());
		}
		boolean oneDate = datesText.size() == 1;
		if (links.size() == datesText.size() || oneDate) {
			for (int i = 0; i < links.size(); i++) {
				String aDate = oneDate ? datesText.get(0) : datesText.get(i);
				Element e = links.get(i);
				handleLinkAndDate(e, aDate);
			}
		} else {
			System.out.format("Struct err %s %s (%s)%n", links.first().text(), links.first().attr("href"), "?");
			handleLinkAndDate(links.first(), datesText.get(0));
		}
	}
	
	private void handleLinkAndDate(Element linkElement, String aDate) {
		LocalDate issued = common.dateFromMonthName(aDate);
		if (issued != null && common.isWithin(issued) && !toExclude(linkElement.text())) { 
			System.out.format("+ %s %s (%s)%n", linkElement.text(), linkElement.attr("href"), issued == null ? "?" : aDate);
			pos.setId(linkElement.text());
			pos.setUri(linkElement.attr("href"));
			pos.setChangeDate(issued);
			if (!foundElements.contains(pos)) {
				foundElements.add(pos.clone());
				common.appendProtocol(pos);
			}
		}
	}
	
	private boolean isWordReference(Element l) {
		String referredFile = l.attr("href");
		String text = l.text();
		return text.startsWith("Word version") && referredFile.endsWith(".docx");
	}
	
	private Element upToParent(Element e, String tag) {
		if (e == null || tag.equals(e.tagName())) return e;
		return upToParent(e.parent(), tag);
	}

}
