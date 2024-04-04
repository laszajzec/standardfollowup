package standard;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FetchHtml {
	
	private org.jsoup.nodes.Document doc;
	private org.jsoup.select.Elements selectedElements;
	private boolean resultOK = true;
	private boolean linkNotValid = false;
	private final String uri;
	private final String fileName;
	
	public FetchHtml(String uri, String fileName) throws IOException, URISyntaxException {
//		doc = Jsoup.connect("https://www.din.de/de/mitwirken/normenausschuesse/nafuo/veroeffentlichungen/wdc-beuth:din21:332097693").get();
		this.uri = uri;
		this.fileName = fileName;
		String fileNameWithType = fileName.contains(".") ? fileName : fileName + ".html";
		try {
			doc = CommonFunctions.get().downloadFileLink(uri, null, CommonFunctions.DownloadDir.HTML, fileNameWithType);
		} catch (org.jsoup.HttpStatusException e) {
			resultOK = false;
			linkNotValid = true;
			e.printStackTrace();
		}
	}
	
	public FetchHtml select(String tag) {
		if (resultOK) {
			selectedElements = doc.select(tag);
		}
		return this;
	}

	public FetchHtml checkIfExitsElement(String value) {
		if (resultOK) {
			boolean success = false;
			for (int i = 0; i < selectedElements.size(); i++) {
				Element e = selectedElements.get(i);
				if (value.equals(e.text())) {
					success = true;
					break;
				}
			}
			resultOK = success;
		}
		return this;
	}
	
	public FetchHtml checkTagEquals(String tag, String value) {
		if (resultOK) {
			boolean success = false;
			for (int i = 0; i < selectedElements.size(); i++) {
				Element e = selectedElements.get(i);
				String referredValue = (tag == null || tag.isEmpty()) ? e.text() : e.attr(tag);
				if (referredValue.equals(value)) {
					success = true;
					break;
				}
			}
			resultOK = success;
		}
		return this;
	}

	public FetchHtml checkTagContains(String tag, String value) {
		if (resultOK) {
			boolean success = false;
			for (int i = 0; i < selectedElements.size(); i++) {
				Element e = selectedElements.get(i);
				String referredValue = (tag == null || tag.isEmpty()) ? e.text() : e.attr(tag);
				if (referredValue.contains(value)) {
					success = true;
					break;
				}
			}
			resultOK = success;
		}
		return this;
	}

	public FetchHtml checkXPathEquals(String xPath, String tag, String value) {
		if (resultOK) {
			Elements es = doc.selectXpath(xPath);
			if (es.size() == 1) {
				Element e = es.getFirst();
				String referredValue = (tag == null || tag.isEmpty()) ? e.text() : e.attr(tag);
				resultOK = referredValue.equals(value);
			}
		}
		return this;
	}

	public FetchHtml checkXPathCond(String xPath, Predicate<Element> cond) {
		if (resultOK) {
			Elements es = doc.selectXpath(xPath);
			if (es.size() == 1) {
				Element e = es.getFirst();
				resultOK = cond.test(e);
			}
		}
		return this;
	}

	public FetchHtml checkXPathContains(String xPath, String tag, String value) {
		if (resultOK) {
			Elements es = doc.selectXpath(xPath);
			if (es.size() == 1) {
				Element e = es.getFirst();
				String referredValue = (tag == null || tag.isEmpty()) ? e.text() : e.attr(tag);
				resultOK = referredValue.contains(value);
			}
		}
		return this;
	}

	
	public boolean select(Predicate<org.jsoup.nodes.Element> select, BiFunction<Integer, org.jsoup.nodes.Element, Boolean> transform) {
		Stream<Boolean> selected = selectedElements.stream()
				.filter(x -> select.test(x))
				.map(x -> transform.apply(0, x));
		return selected.allMatch(x -> x == true); 
	}

	public boolean isResultOK() {
		reportError();
		return resultOK;
	}
	
	private CommonFunctions.DocumentEvent getProblemReason() {
		if (linkNotValid) return CommonFunctions.DocumentEvent.NOT_FOUND;
		else if (resultOK) return null;
		else return CommonFunctions.DocumentEvent.CHANGED;
	}
	
	public void reportError() {
		if (!resultOK) {
			CommonFunctions.get().appendProtocol(getProblemReason(), fileName, uri, null);
		}
	}
	
}
