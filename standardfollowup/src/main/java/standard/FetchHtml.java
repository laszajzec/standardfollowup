package standard;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FetchHtml {
	
	private org.jsoup.nodes.Document doc;
	private org.jsoup.select.Elements selectedElements;
	private boolean resultOK = true;
	private CheckPosition firstDifference;
	
	public FetchHtml(String uri, String fileName) throws IOException, URISyntaxException {
		String fileNameWithType = fileName.contains(".") ? fileName : fileName + ".html";
		try {
			doc = CommonFunctions.get().downloadFileLink(uri, null, CommonFunctions.DownloadDir.HTML, fileNameWithType);
		} catch (org.jsoup.HttpStatusException e) {
			resultOK = false;
			e.printStackTrace();
		}
	}
	
	public FetchHtml select(String tag) {
		if (resultOK) {
			selectedElements = doc.select(tag);
		}
		return this;
	}
	
	private CheckPosition detectedDiff(String expectedValue, CheckPosition.Reason reason) {
		if (firstDifference == null) {
			firstDifference = CheckPosition.get().clone();
			firstDifference.setExpectedValue(expectedValue);
		}
		return firstDifference;
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
			if (!success) { detectedDiff(value, CheckPosition.Reason.DIFFERENT); }
		}
		return this;
	}
	
	public FetchHtml checkTagEquals(String tag, String value) {
		if (resultOK) {
			boolean success = false;
			String referredValue = null;
			for (int i = 0; i < selectedElements.size(); i++) {
				Element e = selectedElements.get(i);
				referredValue = (tag == null || tag.isEmpty()) ? e.text() : e.attr(tag);
				if (referredValue.equals(value)) {
					success = true;
					break;
				}
			}
			resultOK = success;
			if (!success) { 
				detectedDiff(value, CheckPosition.Reason.DIFFERENT)
				.setTag(tag)
				.setCurrentValue(referredValue);
			}
		}
		return this;
	}

	public FetchHtml checkTagContains(String tag, String value) {
		if (resultOK) {
			boolean success = false;
			String referredValue = null;
			for (int i = 0; i < selectedElements.size(); i++) {
				Element e = selectedElements.get(i);
				referredValue = (tag == null || tag.isEmpty()) ? e.text() : e.attr(tag);
				if (referredValue.contains(value)) {
					success = true;
					break;
				}
			}
			resultOK = success;
			if (!success) { 
				detectedDiff(value, CheckPosition.Reason.DIFFERENT)
				.setTag(tag)
				.setCurrentValue(referredValue);
			}
		}
		return this;
	}

	public FetchHtml checkXPathEquals(String xPath, String tag, String value, Predicate<String> toCheck) {
		if (resultOK) {
			Elements es = doc.selectXpath(xPath);
			if (es.size() == 1) {
				Element e = es.getFirst();
				String referredValue = (tag == null || tag.isEmpty()) ? e.text() : e.attr(tag);
				resultOK = toCheck.test(referredValue); // referredValue.equals(value);
				if (!resultOK) { 
					detectedDiff(value, CheckPosition.Reason.DIFFERENT)
					.setPath(xPath)
					.setTag(tag)
					.setCurrentValue(referredValue);
				}
			} else {
				System.out.println("Path not unique: " + xPath);
			}
		}
		return this;
	}
	
	public FetchHtml checkXPathDate(String xPath, String tag, String substringRegex, String format) {
		if (resultOK) {
			Elements es = doc.selectXpath(xPath);
			if (es.isEmpty()) {
				System.out.println("Path not found: " + xPath);
			} else if (es.size() == 1) {
				Element e = es.getFirst();
				String referredValue = (tag == null || tag.isEmpty()) ? e.text() : e.attr(tag);
				if (substringRegex != null && !substringRegex.isEmpty()) {
					Pattern p = Pattern.compile(substringRegex);
					Matcher m = p.matcher(referredValue);
					if (m.matches()) {
						referredValue = m.group(1);
					}
				}
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withLocale(Locale.ENGLISH).withZone(ZoneId.systemDefault());
					LocalDate d = LocalDate.parse(referredValue, formatter);
					resultOK = !CommonFunctions.get().isWithin(d);
				} catch (Exception ex) {
					System.out.format("Cannot convert  %s to date format: ", referredValue, format);
					resultOK = false;
				}
				if (!resultOK) { 
					detectedDiff(null, CheckPosition.Reason.DIFFERENT)
					.setPath(xPath)
					.setTag(tag)
					.setCurrentValue(referredValue);
				}
			} else {
				System.out.println("Path not unique: " + xPath);
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
				if (!resultOK) { 
					detectedDiff(value, CheckPosition.Reason.DIFFERENT)
					.setPath(xPath)
					.setTag(tag)
					.setCurrentValue(referredValue);
				}
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
	
	public void reportError() {
		if (!resultOK) {
//			CommonFunctions.get().appendProtocol(getProblemReason(), fileName, uri, null);
			CommonFunctions.get().appendProtocol(firstDifference);
		}
	}
	
}
