package standard;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import standard.CheckPosition.Reason;

public class ReadEvs implements RegulatorySource {

	final String destinationPattern = "https://www.evs.ee/en/search?query=&languages=41&organisations=1&organisations=2&organisations=3&statuses=2&page=%d&filtertype=filter&committeeoption=1&onlysuggestedproducts=false&onlyNewEstonianLanguageProduct=false";
	final static Set<String> ISO_PREFIXES = Set.of("EVS", "CEN", "CWA", "CLC");
	final static Set<String> IGNORE_PREFIXES = Set.of("View", "Subscribe", "Register", "Show details", "Powered by", "Sign in");
	final Pattern isoPattern = Pattern.compile("(\\D*)(\\d*)(.*)");
	final Selenium selenium;
	final DateTimeFormatter dateForm = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	final CommonFunctions comm = CommonFunctions.get();
	final CheckPosition pos = CheckPosition.get();

	public ReadEvs() {
		selenium = Selenium.get();
	}


	@Override
	public void collect() {
		// Nothing
	}

	@Override
	public void removeUnchanged() {
		// Nothing
	}

	@Override
	public void evaluate() throws IOException {
		for (int i = 1; true; i++) {
			String destination = String.format(destinationPattern, i);
			System.out.format("--- Page %d%n", i);
			if (readPageAndReturnStop(destination)) break;
		}
	}

	private boolean readPageAndReturnStop(String uri) {
		try {
			selenium.setUri(uri);
			pos.setUri(uri);
			List<WebElement> refs = selenium.findElementsByXpeth("//a[@href]");
			for (WebElement el : refs) {
				String content = el.getText();
				if (hasPrefix(ISO_PREFIXES, content)) {
					System.out.format("+ %s           %s%n", content, el.getAttribute("href"));
					if (isOnList(content)) {
						checkDate(el);
					}
				} else {
					if (!content.trim().isEmpty() && !hasPrefix(IGNORE_PREFIXES, content)) {
						System.out.format("- %s%n", content);
					}
				}
			}
			return checkLastPage();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private void checkDate(WebElement el) {
		WebElement parent1 = el.findElement(By.xpath(".."));
		WebElement parent = parent1.findElement(By.xpath(".."));
		//		System.out.format("Parent: %s >%s<%n", parent.getTagName(), parent.getText());
		List<WebElement> subElements = parent.findElements(By.cssSelector(".badge.product-info-badge"));
		//		System.out.format("Found %d subelements%n", subElements.size());
		for (WebElement sub : subElements) {
			//			System.out.format("Sub: %s%n", sub.getText());
			if (sub.getText().startsWith("Valid")) {
				System.out.format("VALID %s           %s%n", el.getText(), sub.getText());
				String validityString = sub.getText().replace("Valid from ", "").trim();
				LocalDate validFrom = LocalDate.parse(validityString, dateForm);
				if (comm.isWithin(validFrom)) {
					pos.setId(el.getText());
					pos.setReason(Reason.DIFFERENT);
					pos.setChangeDate(validFrom);
					comm.appendProtocol(pos);
					System.out.format("!!!Found %s%n", el.getText());
				}
			}
		}
	}
	
	private boolean hasPrefix(Set<String> prefixes, String value) {
		for (String aPrefix : prefixes) {
			if (value.startsWith(aPrefix)) return true;
		}
		return false;
	}

	private boolean checkLastPage() {
		WebElement next = selenium.findElementByXpath("//a[@class=\"page-link\" and text()=\"Next\"]");
//		System.out.format("Next: %s%n", next.getText());
		WebElement nextParent = next.findElement(By.xpath(".."));
		String classes = nextParent.getAttribute("class");
//		System.out.format("Class: >%s<%n", classes);
		return classes.contains("disabled");
	}

	private boolean isOnList(String isoRef) {
		Matcher m = isoPattern.matcher(isoRef);
		if (m.matches()) {
			String isoNum = m.group(2);
			return StandardNumbers.isOnList(isoNum);
		} else {
			System.out.format("Illegal ISO format: s%n", isoRef);
			return false;
		}
	}

	@Override
	public boolean isOk() {
		return false;
	}

}
