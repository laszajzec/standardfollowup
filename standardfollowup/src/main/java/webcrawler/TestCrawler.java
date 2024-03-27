package webcrawler;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.htmlunit.BrowserVersion;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import standard.CommonFunctions;

public class TestCrawler {

	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException, InterruptedException {
//		new TestCrawler().checkSite("https://webstore.iec.ch/publication/24664");
		new TestCrawler().checkSite("https://www.fedlex.admin.ch/eli/cc/2020/552/de");
//		new TestCrawler().selenium();

	}

	public void test1() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		//		System.setProperty("webdriver.chrome.driver", "src/main/resources/progs");
		//		WebDriver driver = new ChromeDriver();
		//		
		//		
		//		try (final WebClient webClient = new WebClient()) {
		//		    // now you have a running browser, and you can start doing real things
		//		    // like going to a web page
		//		    final HtmlPage page = webClient.getPage("https://www.htmlunit.org/");
		//		    page.cleanUp();
		//		}

		try (final WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
			webClient.getOptions().setJavaScriptEnabled(true);
			final HtmlPage page = webClient.getPage("https://www.fedlex.admin.ch/eli/cc/2020/552/de");
			//		        final HtmlPage page = webClient.getPage("https://www.htmlunit.org/");
			recurse(page.getChildNodes(), "0");
//			Iterator<DomNode> iter = page.getChildren().iterator();
//			iter.next();
//			DomNode dn = iter.next();
//			System.out.println("dn " + dn.getChildNodes().size());
//			DomNodeList<DomElement> elements = page.getElementsByTagName("div");
////			System.out.println("Found: " + elements.size());
//			boolean success = elements.stream().anyMatch(x -> "Dieser Text ist in Kraft".equals(x.getTextContent()));
//			System.out.println("Success: " + success);
			//		        Assert.assertEquals("HtmlUnit – Welcome to HtmlUnit", page.getTitleText());

//			final String pageAsXml = page.asXml();
			//		        Assert.assertTrue(pageAsXml.contains("<body class=\"topBarDisabled\">"));

			final String pageAsText = page.asNormalizedText();
			//		        Assert.assertTrue(pageAsText.contains("Support for the HTTP and HTTPS protocols"));
//			System.out.println(pageAsText);
		}
	}
	
	// Selenium
	public void test2() throws InterruptedException, FileNotFoundException {
		System.setProperty("webdriver.chrome.driver", "C:/Progs/gits/standardfollowup/standardfollowup/src/main/resources/progs/chromedriver.exe");
		WebDriver driver = new ChromeDriver(); 
//		driver.get("http://www.google.com/");    
		driver.get("https://www.fedlex.admin.ch/eli/cc/2020/552/de");
		List<WebElement> tags = driver.findElements(By.className("bordered"));
		recurseW(tags, "0");
//		String src = driver.getPageSource();
//		try (PrintWriter out = new PrintWriter("c:/temp/ch.txt")) {
//		    out.println(src);
//		}
//		boolean found = src.contains("Dieser Text ist in Kraft");
		Thread.sleep(5000);  // Let the user actually see something!     
//		WebElement searchBox = driver.findElement(By.name("q"));
//		searchBox.sendKeys("ChromeDriver");     
//		searchBox.submit();    
//		Thread.sleep(5000);  // Let the user actually see something!     
		driver.quit();  
//		System.out.println("Found: " + found);
	}
	
	public void test3() {
        String url = "https://www.fedlex.admin.ch/eli/cc/2020/552/de";
        String outputFile = "c:/Temp/Standards/PublicHealth/test3.txt ";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String content = response.body();

            FileWriter writer = new FileWriter(outputFile);
            writer.write(content);
            writer.close();

            System.out.println("Website content saved successfully.");
        } catch (IOException | InterruptedException e) {
            System.err.println("Error saving website content: " + e.getMessage());
        }
	}

	private void recurse(DomNodeList<DomNode> nodes, String path) {
		for (int i = 0; i < nodes.size(); i++) {
			DomNode node = nodes.get(i);
			System.out.format("%10s %s -> %s%n", path, node.getClass().getName(), node.getTextContent());
			recurse(node.getChildNodes(), path + i);
		}
	}

	private void recurseW(List<WebElement> nodes, String path) {
		for (int i = 0; i < nodes.size(); i++) {
			WebElement node = nodes.get(i);
			System.out.format("%10s %s -> %s%n", path, node.getClass().getName(), node.getText());
			recurseW(node.findElements(By.tagName("div")), path + i);
		}
	}
	
	private void selenium() throws IOException {
		CommonFunctions comm = new CommonFunctions(Paths.get("C:/temp/Standards"), null);
		/*
		new FetchHtml("https://webstore.iec.ch/publication/2606", "IEC 60601-1 2005")
		.select("span")
		.checkIfExitsElement("2005-12-15")
		.checkIfExitsElement("3.0")
		.checkIfExitsElement("2028")
		.isResultOK();
		 */
		WebDriver driver = new ChromeDriver(); 
		driver.get("https://webstore.iec.ch/publication/2606");
		
		boolean b1 = comm.checkWithId(driver, "view:inputText3", "2005-12-15");
		boolean b2 = comm.checkWithId(driver, "view:inputText1", "3.0");
		boolean b3 = comm.checkWithId(driver, "view:computedField10", "2028");
		System.out.println((b1 & b2 & b3) ? "OK" : "Bad");
		
		/*
		List<WebElement> tags = driver.findElements(By.id("view:inputText3"));
		System.out.format("Found %d elements%n", tags.size());
		tags.stream().forEach(x -> System.out.format("value: %s%n", x.getText()));
		
		WebElement dateElemXpath = driver.findElement(By.xpath("//span[@itemprop='releaseDate']"));
		System.out.format("Found date xpath >%s<   %s%n", dateElemXpath.getText(), dateElemXpath);
		 */
		driver.quit();
	}
	
	private int counter;
	private void checkSite(String uri) {
		counter = 0;
		WebDriver driver = new ChromeDriver();
		driver.get(uri);
		Set<WebElement> allElements = getAllElements(driver.findElements(By.xpath("*")));
//		for (WebElement e : allElements) {
//			System.out.format("%s %s %s %n", e.getTagName(), e.getAttribute("id"), e.getAttribute("class"), e.getText());
//		}
		driver.quit();
	}
	
	private void getAllElements(WebElement element, Set<WebElement> ret, int level) {
		show(element, level);
	    ret.add(element);
	    try {
	    	List<WebElement> childs = element.findElements(By.xpath("*"));
	    	for(WebElement e: childs) getAllElements(e,ret, level+1);
	    } catch (Exception ex) {
	    	System.out.println("Excepion for children: " + element.getTagName());
	    };
	}
	private Set<WebElement> getAllElements(List<WebElement> roots) {
		Set<WebElement> allElements = new LinkedHashSet<>();
		for (WebElement e : roots) {
			show(e, 0);
			getAllElements(e, allElements, 1);
		}
		return allElements;
	}
	
	Set<String> shortenedTags = Set.of("script", "html", "footer", "body");
	private void show(WebElement e, int level) {
		String tagName;
		String id;
		String clazz;
		String name;
		String text;
		String textContent;
		String value;

		try {tagName = e.getTagName();} catch (Exception ex) {tagName = "--";};
		if (shortenedTags.contains(tagName)) {
			System.out.format("%4d %2d t:%7s ...%n", counter, level, tagName);
		} else {
			try {id = e.getAttribute("id");} catch (Exception ex) {id = "--";};
			try {clazz = e.getAttribute("class");} catch (Exception ex) {clazz = "--";};
			try {name = e.getAttribute("name");} catch (Exception ex) {name = "--";};
			try {text = e.getText();} catch (Exception ex) {text = "--";};
			try {textContent = e.getAttribute("textContent");} catch (Exception ex) {textContent = "--";};
			try {value = e.getAttribute("value");} catch (Exception ex) {value = "--";};
			String textRes = getFirstValuable(text, textContent, value);
			if (name == null || "null".equals(name)) name = "";
			System.out.format("!! %4d %2d t:%7s id:<%8s> c:<%s> nam: <%s> text:%s%n", counter, level, tagName, id, clazz, name, textRes);
		}
		counter++;
	}
	
	private String getFirstValuable(String text, String content, String value) {
		String[] res = new String[3];
		res[0] = (text != null && !text.isEmpty()) ? text : "";
		res[1] = (content != null && !content.isEmpty() && !res[0].equals(content)) ? content : "";
		res[2] = (value != null && !value.isEmpty() && !res[0].equals(value) && !res[1].equals(value)) ? value : "";
		StringBuilder ret = new StringBuilder();
		if (!res[0].isEmpty()) ret.append("t:<" + res[0] + ">");
		if (!res[1].isEmpty()) ret.append("c:<" + res[1] + ">");
		if (!res[2].isEmpty()) ret.append("v:<" + res[2] + ">");
		return (ret.length() > 100) ? ret.substring(0, 100) : ret.toString();
	}
}
