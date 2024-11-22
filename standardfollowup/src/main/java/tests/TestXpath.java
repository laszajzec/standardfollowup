package tests;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import standard.CommonFunctions;
import standard.Selenium;

public class TestXpath {
	

	public static void main(String[] args) throws IOException, URISyntaxException {
		new TestXpath().test();
	}
	
	public void test() throws IOException, URISyntaxException {
		/*
		final Document doc = Jsoup.parse(Paths.get("c:/Temp/Standards/PublicHealth/2024-07-24-07-18-13/html/EN ISO 15004-2.html").toFile());
		// /html/body/div[1]/main/div[3]/div/div[1]/section[3]/div/div[1]/div[2]/table[1]/tbody/tr[2]/td[2]
		final String origPath = "//*[@class=\"iec-table\"][1]/tbody/tr[2]/td[2]";
		Elements found = testPath(doc, origPath);
//		final String[] pathElements = origPath.split("/");
//		StringBuilder currentPath = new StringBuilder();
//		for (String e : pathElements) {
//			currentPath.append("/");
//			currentPath.append(e);
//			int count = testPath(doc, currentPath.toString()).size();
////			if (count == 0) break;
//		}
//		System.out.println("-------------");
//		Elements els = testPath(doc, "//*[@class=\"iec-table\"]");
//		if (!els.isEmpty()) {
//			Element el = els.first();
//			List<Node> tabContent = el.childNodes();
//			Element body = (Element)tabContent.get(1);
//			List<Node> rows = body.childNodes();
//			Node secondRow = rows.get(2);
//			List<Node> tds = secondRow.childNodes();
//			Element td = (Element)tds.get(1);
//			String content = td.text();
//			System.out.println("Found: " + content);
//		}
 */
/////////////////////// ESV
//		final Document doc = Jsoup.parse(Paths.get("https;//www.evs.ee/en/search").toFile());
		/*		
		final Document doc = Jsoup.parse(URI.create(destination).toURL(), 5000);
		final String searchPath = "//a[@href]";
		Elements els = testPath(doc, searchPath);
		if (!els.isEmpty()) {
			for (Element el : els) {
				String href = el.attr("href");
				String text = el.text();
				if (text.startsWith("EVS-EN")) {
					System.out.format("+ %s           %s%n", text, href);
				} else {
					System.out.format("- %s%n", text);
				}
			}
		}
*/
		/* EVS */
		final String destinationPattern = "https://www.evs.ee/en/search?query=&languages=41&organisations=1&organisations=2&organisations=3&statuses=2&page=%d&filtertype=filter&committeeoption=1&onlysuggestedproducts=false&onlyNewEstonianLanguageProduct=false";
		for (int i = 999; true; i++) {
			String destination = String.format(destinationPattern, i);
			System.out.format("--- Page %d%n", i);
			if (readPageAndStop(destination)) break;
		}
	}
	
	private boolean readPageAndStop(String uri) throws IOException {
		Selenium selenium = Selenium.get();
		selenium.setUri(uri);
		List<WebElement> refs = selenium.findElementsByXpeth("//a[@href]");
		for (WebElement el : refs) {
			if (el.getText().startsWith("EVS")) {
				System.out.format("+ %s           %s%n", el.getText(), el.getAttribute("href"));
				WebElement parent1 = el.findElement(By.xpath(".."));
				WebElement parent = parent1.findElement(By.xpath(".."));
				//					System.out.format("Parent: %s >%s<%n", parent.getTagName(), parent.getText());
				List<WebElement> subElements = parent.findElements(By.cssSelector(".badge.product-info-badge"));
				//					System.out.format("Found %d subelements%n", subElements.size());
				for (WebElement sub : subElements) {
					//						System.out.format("Sub: %s%n", sub.getText());
					if (sub.getText().startsWith("Valid")) {
						System.out.format("VALID %s           %s%n", el.getText(), sub.getText());
					}
				}
			}
		}
		WebElement next = selenium.findElementByXpath("//a[@class=\"page-link\" and text()=\"Next\"]");
		System.out.format("Next: %s%n", next.getText());
		WebElement nextParent = next.findElement(By.xpath(".."));
		String classes = nextParent.getAttribute("class");
		System.out.format("Class: >%s<%n", classes);
		return classes.contains("disabled");
	}
	
	private Elements testPath (Document doc, String path) {
		Elements els = doc.selectXpath(path);
		System.out.format("Selection size = %3d %s%n", els.size(), path);
		return els;
	}

}
