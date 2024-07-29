package standard;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

public class Selenium implements Closeable {

	private final WebDriver driver;
	private final boolean withTest = true;
	private String lastPath;
	private CheckPosition firstDifference;
	private boolean resultOK = true;

	public Selenium(String uri) {
		ChromeOptions option = new ChromeOptions();
		option.addArguments("headless");
		driver = new ChromeDriver(option);
		if (uri != null && !uri.isEmpty()) {
			driver.get(uri);
		}
	}
	
	public void setUri(String uri) {
		driver.get(uri);
	}

	public WebElement findElementByXpath(String xPath) {
		lastPath = xPath;
		return driver.findElement(By.xpath(xPath));
	}

	public List<WebElement> findElements(String xPath) {
		return driver.findElements(By.xpath(xPath));
	}

	
	public WebElement findElementByXpathwait(String xPath) {
		lastPath = xPath;
		Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
				.withTimeout(Duration.ofSeconds(10))
				.pollingEvery(Duration.ofSeconds(1))
				.ignoring(NoSuchElementException.class);
		WebElement e = wait.until(new Function<WebDriver, WebElement>() {
			public WebElement apply(WebDriver driver) {
				return driver.findElement(By.xpath(xPath));
			}
		});
		return e;
	}
	
	public boolean equalText(WebElement e, String expectedText) {
		String eString = e.getText();
		if (eString.equals(expectedText)) {
			return true;
		} else {
			detectedDiff(expectedText, CheckPosition.Reason.DIFFERENT).setCurrentValue(eString);
			resultOK = false;
			if (withTest) {
				System.out.format("Selenium in %s found >%s< instead of >%s<%n", lastPath, eString, expectedText);
			}
			return false;
		}
	}

	public boolean containsText(WebElement e, String expectedText) {
		String eString = e.getText();
		if (eString.contains(expectedText)) {
			return true;
		} else {
			detectedDiff(expectedText, CheckPosition.Reason.DIFFERENT).setCurrentValue(eString);
			resultOK = false;
			if (withTest) {
				System.out.format("Selenium in %s found >%s< does not contain >%s<%n", lastPath, eString, expectedText);
			}
			return false;
		}
	}
	
	private CheckPosition detectedDiff(String expectedValue, CheckPosition.Reason reason) {
		if (firstDifference == null) {
			firstDifference = (CheckPosition.get().clone());
			firstDifference.setExpectedValue(expectedValue);
		}
		return firstDifference;
	}

	public boolean isResultOK() {
		if (!resultOK) {
			CommonFunctions.get().appendProtocol(firstDifference);
		}
		return resultOK;
	}

	@Override
	public void close() throws IOException {
		if (driver != null) {
			driver.quit();
		}
	}

}
