package standard;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import standard.CheckPosition.Reason;

public class ReadBgmDe implements RegulatorySource {

	private final String destinationPattern = "https://www.bundesgesundheitsministerium.de/service/gesetze-und-verordnungen.html";
	private final String inputDateFrom = "//*[@id=\"formfield-date-range-from\"]";
	private final String inputDateTo = "//*[@id=\"formfield-date-range-to\"]";
	private final String filternButton = "//*[@id=\"c-simple-collapse-content-1\"]/div/div[2]/span/button";
	private final Selenium selenium;
	private final DateTimeFormatter dateForm = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private final CommonFunctions comm = CommonFunctions.get();
	private final CheckPosition pos = CheckPosition.get();
	private boolean wasChange = false;
	/*
	 * https://www.bundesgesundheitsministerium.de/service/gesetze-und-verordnungen/detail/medizinprodukte-eu-anpassungsgesetz-mpeuanpg.html
	 * https://www.bundesgesundheitsministerium.de/service/gesetze-und-verordnungen/detail/medizinprodukte-eu-anpassungsverordnung-mpeuanpv.html
	 */
	

	public ReadBgmDe() {
		selenium = Selenium.get();
		executeQuery();
	}
	
	@Override
	public void collect() {
	}

	@Override
	public void removeUnchanged() {
	}

	@Override
	public void evaluate() {
		
	}

	@Override
	public boolean isOk() {
		return wasChange;
	}
	
	private void executeQuery() {
		loadSite(destinationPattern);
		WebElement from = selenium.findElementByXpath(inputDateFrom);
		WebElement to = selenium.findElementByXpath(inputDateTo);
		WebElement filter = selenium.findElementByXpath(filternButton);
//		String fromDate = dateForm.format(comm.getDateOfCheckFrom());
		String fromDate = "01.01.2023";
		String untilDate = dateForm.format(comm.getDateOfCheckUntil());
		selenium.execute(from, "arguments[0].type='visible';");
		selenium.execute(from, String.format("arguments[0].value='%s';", fromDate));
//		from.sendKeys(vonDate);
		selenium.execute(to, String.format("arguments[0].value='%s';", untilDate));
//		to.sendKeys(dateForm.format(comm.getDateOfCheckUntil()));
		filter.submit();
//		List<WebElement> refs = selenium.findElementsByTag("a");
//		List<WebElement> subpages = collectSubpages(refs);
		while (true) {
			checkPage();
			String nextUri = findNextPageUri();
			if (nextUri == null) {
				break;
			}
			loadSite(nextUri);
		}
//		for (int i = 0; i < subpages.size(); i++) {
//			loadAndCheckSubpage(subpages.get(i));
//		}
	}
	
	private void loadSite(String uri) {
		System.out.format(" -- Load page: %s%n", uri);
		selenium.setUri(uri);
	}
	
	private String findNextPageUri() {
		List<WebElement> refs = selenium.findElementsByTag("a");
		for (WebElement e : refs) {
			if (isNextPageReference(e)) {
				return e.getAttribute("href");
			}
		}
		return null;
	}

	private boolean isNextPageReference(WebElement e) {
		if (e.getAttribute("href") == null) return false;
		if (!e.getDomProperty("className").contains("c-pagination__link")) return false;
		String text = e.getText();
		return "Nächste Seite".equals(text);
	}

	private boolean isDocumentList(WebElement e) {
		String classNames = e.getDomProperty("className");
		return classNames.contains("c-teaser-list--multicolumns");
	}
	
	private void checkPage() {
		List<WebElement> tableLines = selenium.findElementsByTag("ul");
		for (WebElement e : tableLines) {
			if (isDocumentList(e)) {
				checkReferences(e);
				break;
			}
		}
	}

	private void checkReferences(WebElement e) {
		List<WebElement> lines = e.findElements(By.tagName("li"));
		for (WebElement line : lines) {
			identifyDocumentRef(line);
		}
	}
	
	private String findDate(WebElement line) {
//		WebElement dateElement = e.findElements(By.xpath("./div")).stream().filter(d -> d.getDomProperty("className").contains("c-teaser-date-left") && d.getText() != null && !d.getText().isEmpty()).findAny().get();
		List<WebElement> divs = line.findElements(By.tagName("div"));
		for (WebElement aDiv : divs) {
			String classNames = aDiv.getDomProperty("className");
			if (!classNames.contains("c-teaser__date-left")) continue;
			String dateText = aDiv.getText();
			if (dateText != null && !dateText.isEmpty()) {
				return dateText;
			}
		}
		return "";
	}

	private String findCategory(WebElement line) {
		List<WebElement> spans = line.findElements(By.tagName("span"));
		for (WebElement aSpan : spans) {
			String classNames = aSpan.getDomProperty("className");
			if (!classNames.contains("c-category-title")) continue;
			String categoryText = aSpan.getText();
			if (categoryText != null && !categoryText.isEmpty()) {
				return categoryText;
			}
		}
		return "";
	}

	private void identifyDocumentRef(WebElement e) {
		String dateString = findDate(e);
		WebElement ref = e.findElement(By.tagName("a"));
		String href = ref.getAttribute("href");
		String title = ref.getAttribute("title");
		String category = findCategory(e);
		System.out.format("Found %s (%s) %s (%s)%n", dateString, category, title, href);
		pos.setUri(href);
		pos.setId(title);
		pos.setReason(Reason.DIFFERENT);
		pos.setChangeDate(LocalDate.parse(dateString, dateForm));
		comm.appendProtocol(pos);
		wasChange = true;
	}
}
