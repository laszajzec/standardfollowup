package standard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import standard.ec.publichealth.ReadPublicHealth;

/**
 * Collect documents of medical regulation
 * Compare with last versions and report differences
 * 
 * typ1: download all documents
 * typ2: download list of documents
 * typ3: save list of html
 * typ4: sepcial case
 */
@Command(name = "checksum", mixinStandardHelpOptions = true, version = "checksum 4.0", description = "Prints the checksum (SHA-256 by default) of a file to STDOUT.")
public class RegulatioryChanges implements Callable<Integer> {

	private CommonFunctions common;
	private Path oldDir;
	private Path newDir;
	private boolean doCompare = false;

	@Option(names = { "-b", "--base" }, defaultValue="C:/temp/Standards", description = "Base of all collected standard")
	Path baseDir;

	@Option(names = { "-o", "--old" }, description = "Directory to compare with")
	String oldDirArg;

	@Option(names = { "-n", "--new" }, description = "Directory to load current state")
	String newDirArg;

	@Option(names = { "-t", "--test" }, description = "Which test case should run", defaultValue="0123456789ABCDEF")
	String testCase;

	@Option(names = { "-l", "--lastcheck" }, description = "Date of last check as yyyy-mm-dd")
	String dateOfLastCheckString;

	@Option (names = { "-r", "--reducedir" }, defaultValue="false",  description = "Deletes identical old files")
	boolean reduceDir;
	
	/**
	 * @param Directory of collected documents or c:\temp\Standards if absent
	 */
	public static void main(String[] args) throws IOException, URISyntaxException {
		int exitCode = new CommandLine(new RegulatioryChanges()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {
		long start = System.currentTimeMillis();
		doIt();
		System.out.println(" --- Finished ---");
		printElapsedTime(start);
		return 0;
	}

	public RegulatioryChanges() throws IOException, URISyntaxException {
	}

	private void doIt() throws IOException, URISyntaxException {
		LocalDate dateOfLastCheck = null;
		if (dateOfLastCheckString != null) {
			try {
				dateOfLastCheck = LocalDate.parse(dateOfLastCheckString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			} catch (DateTimeParseException e) {
				System.out.println("Wrong last check date format, yuse yyyy-mm-dd");
				return;
			}
		}

		common = new CommonFunctions(baseDir, dateOfLastCheck);
		common.appendJournal("Seek for changes at : " + CommonFunctions.dateAndTimeFormat.format(new Date()) + System.lineSeparator());
		if (oldDirArg == null) {
			oldDir = common.getLastDir();
		} else {
			oldDir = common.getBaseDirPath().resolve(oldDirArg);
			if (oldDir != null && !Files.isDirectory(oldDir)) {
				System.out.println("Not correct directory: " + oldDirArg);
				throw new FileNotFoundException(oldDirArg);
			}
		}
		newDir = newDirArg == null ? null : common.getBaseDirPath().resolve(newDirArg);
		if (newDir != null && !Files.isDirectory(newDir)) {
			System.out.println("Not correct directory: " + newDirArg);
			throw new FileNotFoundException(oldDirArg);
		}
		
		common.createNewDir(newDirArg);
		if (newDirArg == null) {
			collect();
		}
		if (oldDirArg == null) {
			common.getLastDir();
		}
		evaluate();
		if (reduceDir) {
			common.reduceOldDir();
		}
		common.storeProtokoll();
	}

	private void collect() throws IOException, URISyntaxException {
		/*
		if (testCase.contains("0")) loadISO.get();
		if (testCase.contains("1")) loadIEC.get();
		if (testCase.contains("2")) loadEST.get();
		if (testCase.contains("3")) loadDin.get();
		if (testCase.contains("4")) loadEulex.get();        // + https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX:32017R0745
		if (testCase.contains("5")) loadDE_MedProodGesetz.get();
		if (testCase.contains("6")) loadUS_FedReg.get();
		if (testCase.contains("7")) loadBrHealth.get();
		if (testCase.contains("8")) loadCaHealth.get();     // + https://www.fda.gov/medical-devices/medical-device-single-audit-program-mdsap/mdsap-audit-procedures-and-forms
		if (testCase.contains("9")) loadAuHeath.get();      // + https://www.tga.gov.au/
		if (testCase.contains("A")) loadJpHealth.get();     // + https://www.pmda.go.jp/english/review-services/regulatory-info/0004.html
		if (testCase.contains("B")) loadChHealth.get();     // + https://www.fedlex.admin.ch/de/home?news_period=last_day&news_pageNb=1&news_order=desc&news_itemsPerPage=10
		if (testCase.contains("C")) loadImdrf.get();        // + https://www.imdrf.org/documents
		if (testCase.contains("D")) loadUkLex.get();        //   https://www.gov.uk/government/collections/regulatory-guidance-for-medical-devices
		if (testCase.contains("E")) loadEuHealth.get();    // + https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en
		if (testCase.contains("F")) loadHealthcare.get();   // - https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en
		*/
		
		check('0', loadISO);
		check('1', loadIEC);
		check('2', loadEST);
		check('3', loadDin);
		check('4', loadEulex);        // + https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX:32017R0745
		check('5', loadDE_MedProodGesetz);
		check('6', loadUS_FedReg);
		check('7', loadBrHealth);
		check('8', loadCaHealth);     // + https://www.fda.gov/medical-devices/medical-device-single-audit-program-mdsap/mdsap-audit-procedures-and-forms
		check('9', loadAuHeath);      // + https://www.tga.gov.au/
		check('A', loadJpHealth);     // + https://www.pmda.go.jp/english/review-services/regulatory-info/0004.html
		check('B', loadChHealth);     // + https://www.fedlex.admin.ch/de/home?news_period=last_day&news_pageNb=1&news_order=desc&news_itemsPerPage=10
		check('C', loadImdrf);        // + https://www.imdrf.org/documents
		check('D', loadUkLex);        //   https://www.gov.uk/government/collections/regulatory-guidance-for-medical-devices
		check('E', loadEuHealth );    // + https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en
		check('F', loadHealthcare);   // - https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en
	}
	
	private void check(char id, SupplierE<Boolean> checkFunc) {
		try {
			if (testCase.indexOf(id) >= 0) {
				System.out.format("%n-- executing %c%n", id);
				boolean ok = checkFunc.get();
				if (!ok) {
					System.err.format("Please check protocoll");
				}
			}
		} catch (Exception e) {
			System.err.format("!!! error in %c%n", id);
			e.printStackTrace(System.err);
			common.appendProtocolLn("!!! error in " + id);
		}
	}
	
	private SupplierE<Boolean> loadISO = () -> {
		/* ISO https://www.iso.org/home.html
		 * 10342 10343 10938 10993-1 10993-18 10993-5 10993-7 11135 11137-1 11607-1 11607-2 11737-1 12866 12867 13485 14971 15004-1 15223-1 15223-2
		 * 17664-1 17664-2 19011 19980 20417 22665 4180 8429 8596 8612 9022-3 9801  
		 */
		ReadISO obj = new ReadISO();
		obj.collect();
		obj.removeUnchanged();
		obj.evaluate();
		return obj.isOk();
	};

	private SupplierE<Boolean> loadIEC = () -> {
		/* https://www.iec.ch/homepage
		 * IEC 60601-1 60601-1-2 60601-1-6 60601-1-9 60825-1 62304 62366-1 62471 TR 62366-2
		 */
		
		// 60601-1
		new FetchHtml("https://webstore.iec.ch/publication/67497", "IEC 60601-1 2005 AMD1 2012 AMD2 2020")
		.select("span")
		.checkIfExitsElement("2020-08-20")
		.checkIfExitsElement("3.2")
		.checkIfExitsElement("2028")
		.isResultOK();

		//60601-1-2
		new FetchHtml("https://webstore.iec.ch/publication/67554", "IEC 60601-1-2 2014 AMD1 2020")
		.select("span")
		.checkIfExitsElement("2020-09-01")
		.checkIfExitsElement("4.1")
		.checkIfExitsElement("2028")
		.isResultOK();

		// 60601-1-6
		new FetchHtml("https://webstore.iec.ch/publication/67381", "IEC 60601-1-6 2010 AMD1 2013 AMD2 2020")
		.select("span")
		.checkIfExitsElement("2020-07-22")
		.checkIfExitsElement("3.2")
		.checkIfExitsElement("2028")
		.isResultOK();

		// 60601-1-9
		new FetchHtml("https://webstore.iec.ch/publication/67382", "IEC 60601-1-9 2007 AMD1 2013 AMD2 2020")
		.select("span")
		.checkIfExitsElement("2020-07-22")
		.checkIfExitsElement("1.2")
		.checkIfExitsElement("2028")
		.isResultOK();
		
		// 60825-1
		new FetchHtml("https://webstore.iec.ch/publication/3587", "IEC 60825-1 2014")
		.select("span")
		.checkIfExitsElement("2014-05-15")
		.checkIfExitsElement("3.0")
		.checkIfExitsElement("2025")
		.isResultOK();

		// 62304
		new FetchHtml("https://webstore.iec.ch/publication/6792", "IEC 62304 2006")
		.select("span")
		.checkIfExitsElement("2006-05-09")
		.checkIfExitsElement("1.0")
		.checkIfExitsElement("2026")
		.isResultOK();

		// 62366-1
		new FetchHtml("https://webstore.iec.ch/publication/67220", "IEC 62366-1 2015 AMD1 2020")
		.select("span")
		.checkIfExitsElement("2020-06-17")
		.checkIfExitsElement("1.1")
		.checkIfExitsElement("2026")
		.isResultOK();

		// 62366-2
		new FetchHtml("https://webstore.iec.ch/publication/24664", "IEC TR 62366-2 2016")
		.select("span")
		.checkIfExitsElement("2016-04-27")
		.checkIfExitsElement("1.0")
		.checkIfExitsElement("2026")
		.isResultOK();

		// 62471 TR
		new FetchHtml("https://webstore.iec.ch/publication/7076", "IEC 62471 2006")
		.select("span")
		.checkIfExitsElement("2006-07-26")
		.checkIfExitsElement("1.0")
		.checkIfExitsElement("2024")
		.isResultOK();
		return true;
		
	}; // https://www.iec.ch/homepage

	private SupplierE<Boolean> loadEST = () -> {
		/* https://www.evs.ee/en
		 * EN 556-1 EN ISO 15004-2 EN 22248
		 */
		boolean ok = true;
		ok &= new FetchHtml("https://www.evs.ee/en/evs-en-556-1-2002", "EN 556-1")
		.select("div")
		.checkIfExitsElement("EVS-EN 556-1:2002")
		.checkIfExitsElement("Valid from 06.05.2002")
		.select("i")
		.checkXPathTag("//*[@id=\"product-details-form\"]/div[1]/div[2]/div[7]/div/div/div[2]/div[2]/div[1]/div/i", "class", "fas fa-check-circle green")
		.isResultOK();
		
		ok &= new FetchHtml("https://www.evs.ee/en/evs-en-iso-15004-2-2007", "EN ISO 15004-2")
		.select("div")
		.checkIfExitsElement("EVS-EN ISO 15004-2:2007")
		.checkIfExitsElement("Valid from 05.04.2007")
		.select("i")
		.checkXPathTag("//*[@id=\"product-details-form\"]/div[1]/div[2]/div[7]/div/div/div[2]/div[2]/div[1]/div/i", "class", "fas fa-check-circle green")
		.isResultOK();
		
		ok &= new FetchHtml("https://www.evs.ee/en/evs-en-22248-2003", "EN 22248")
		.select("div")
		.checkIfExitsElement("EVS-EN 22248:2003")
		.checkIfExitsElement("Valid from 01.09.2003")
		.checkXPathTag("//*[@id=\"product-details-form\"]/div[1]/div[2]/div[7]/div/div/div[2]/div/div[1]/div/i", "class", "fas fa-check-circle green")
		.isResultOK();
		
		return ok;
	};

	private SupplierE<Boolean> loadDin = () -> {
		/* https://www.din.de/de
		 * DIN 58220-3
		 */
		boolean ok = new FetchHtml("https://www.din.de/de/mitwirken/normenausschuesse/nafuo/veroeffentlichungen/wdc-beuth:din21:332097693", "DIN 58220-3.html")
		.checkXPathTag("/html/body/div[4]/main/div[5]/div/div[1]/div[2]/div[1]/div[1]/span[2]", "", "2021-04")
		.checkXPathTag("/html/body/div[4]/main/div[4]/div/div/div/span/span", "", "[AKTUELL]")
		.isResultOK();
		return ok;
	};
	
	private SupplierE<Boolean> loadEulex = () -> {
		/* typ2: https://eur-lex.europa.eu/homepage.html?locale=de
		 * DIRECTIVE 2011/65/EU (RoHS)
		 * Regulation (EC) No 1907/2006 (REACH)
		 * Regulation (EU) 2017/745 (MDR)
		 * European Medical Device Nomenclature (EMDN)
		 * 
		 * 
		 * https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32017R0745 !!!!! no file
		 */
		File file1 = common.downloadFile("https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX:32011L0065", CommonFunctions.DownloadDir.CONTENT, 
				"EU Directive Electronic Equipment.pdf"); // DIRECTIVE 2011/65/EU (RoHS)
		File file2 = common.downloadFile("https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX:32017R0745", CommonFunctions.DownloadDir.CONTENT, 
				"EU Regulation No 1907-2006.pdf"); // Regulation (EC) No 1907/2006 (REACH)
		File file3 = common.downloadFile("https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX:32017R0745", CommonFunctions.DownloadDir.CONTENT, 
				"EU Medizinprodukte.pdf"); // Regulation (EU) 2017/745 (MDR)
		File file4 = common.downloadFile("https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX:32021R2078", CommonFunctions.DownloadDir.CONTENT, 
				"EU Medical Device Nomenclature.pdf"); // European Medical Device Nomenclature (EMDN)
		return common.checkFiles(new File[] {file1, file2, file3, file4}, "https://eur-lex.europa.eu");
	};
	
	private SupplierE<Boolean> loadDE_MedProodGesetz = () -> {
		/* https://www.gesetze-im-internet.de/mpdg/
		 * MPDG MPEUAnpG MPEUAnpV
		 */
		boolean ok = true;
		File file1 = common.downloadFile("https://www.bundesgesundheitsministerium.de/fileadmin/Dateien/3_Downloads/Gesetze_und_Verordnungen/GuV/R/Referentenentwurf_RISG.pdf",
				CommonFunctions.DownloadDir.CONTENT, "MPEUAnpG.pdf");
		File file2 = common.downloadFile("https://www.bundesgesundheitsministerium.de/fileadmin/Dateien/3_Downloads/Gesetze_und_Verordnungen/GuV/M/RefE_MPEUAnpV.pdf",
				CommonFunctions.DownloadDir.CONTENT, "MPEUAnpV.pdf");
		ok = common.checkFiles(new File[] {file1, file2}, "https://www.bundesgesundheitsministerium.de");
		
		ok &= new FetchHtml("https://dip.bundestag.de/vorgang/.../255346", "MPEUAnpG")
		.checkXPathTag("//*[@id=\"content-übersicht\"]/div/div[1]/ul[1]/li[5]/span/ul/li/text()[1]", "", "26.05.2020")
		.isResultOK();

		ok &= new FetchHtml("https://www.bundesgesundheitsministerium.de/service/gesetze-und-verordnungen/detail/medizinprodukte-eu-anpassungsverordnung-mpeuanpv.html", "MPEUAnpV")
		.checkXPathTag("//*[@id=\"article\"]/div[2]/div/div/div/div/div/span[2]", "", "26.05.2021")
		.isResultOK();
		
		ok &= new FetchHtml("https://www.bundesgesundheitsministerium.de/service/gesetze-und-verordnungen/detail/verordnung-zur-abgabe-von-medizinprodukten-und-zur-aenderung-medizinprodukterechtlicher-vorschriften.html", "MPDG")
		.checkXPathTag("//*[@id=\"article\"]/div[2]/div/div/div/div/div/span[2]", "", "25.07.2014")
		.isResultOK();
		return ok;
	};

	private SupplierE<Boolean> loadHealthcare = () -> {
		/* type1 https://health.ec.europa.eu
		 */
		ReadPublicHealth obj = new ReadPublicHealth();
		obj.collect();
		obj.evaluate();
		return obj.isOk();
	};
	
	private SupplierE<Boolean> loadUS_FedReg = () -> {
		/* https://www.ecfr.gov
		 * FDA 21 CFR part 803 
		 * FDA 21 CFR part 806 
		 * FDA 21 CFR part 807 
		 * FDA 21 CFR part 820 
		 * FDA 21 CFR Part 830 
		 */
		File file0 = common.downloadFile("https://www.ecfr.gov/api/versioner/v1/full/2024-03-26/title-21.xml?part=803", CommonFunctions.DownloadDir.HTML, "FDA 21 CFR part 803.xml");
		File file1 = common.downloadFile("https://www.govinfo.gov/link/cfr/21/803?link-type=pdf&year=mostrecent", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 803.pdf");
		File file2 = common.downloadFile("https://www.govinfo.gov/link/cfr/21/806?link-type=pdf&year=mostrecent", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 806.pdf");
		File file3 = common.downloadFile("https://www.govinfo.gov/link/cfr/21/807?link-type=pdf&year=mostrecent", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 807.pdf");
		File file4 = common.downloadFile("https://www.govinfo.gov/link/cfr/21/820?link-type=pdf&year=mostrecent", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 820.pdf");
		File file5 = common.downloadFile("https://www.govinfo.gov/link/cfr/21/830?link-type=pdf&year=mostrecent", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 830.pdf");
		return common.checkFiles(new File[] {file0, file1, file2, file3, file4, file5}, "https://www.ecfr.gov");
	};
	
	private SupplierE<Boolean> loadAuHeath = () -> {
		/* https://www.tga.gov.au/
		 * Therapeutic Goods Act 1989
		 * Therapeutic Goods (Medical Devices) Regulations 2002
		 */
		boolean ok = true;
		ok &= new FetchHtml("https://www.legislation.gov.au/C2004A03952/latest/text", "Therapeutic Goods Act 1989.html")
		.select("span")
		.checkIfExitsElement("Latest version")
		.checkIfExitsElement("In force")
		.checkIfExitsElement("21 September 2023")
		.isResultOK();

		ok &= new FetchHtml("https://www.legislation.gov.au/F2002B00237/latest/text", "Therapeutic Goods (Medical Devices) Regulations 2002.html")
		.select("span")
		.checkIfExitsElement("Latest version")
		.checkIfExitsElement("In force")
		.checkIfExitsElement("01 January 2024")
		.isResultOK();
		return ok;
	};
	
	private SupplierE<Boolean> loadJpHealth = () -> {
		/* https://www.pmda.go.jp/english/review-services/regulatory-info/0004.html
		 * MHLW Ministerial Ordinance No. 169
		 */
		return new FetchHtml("https://www.pmda.go.jp/english/review-services/regulatory-info/0004.html", "MHLW Ministerial Ordinance No 169.html")
		.checkXPathCond("//*[@id=\"contents\"]/div[2]/div/main/div/div/p[1]", e -> e.text().contains("2021: The second chapter of the ordinance was revised to align with ISO13485:2016."))
		.isResultOK();
	};
	
	private SupplierE<Boolean> loadChHealth = () -> {
		/* https://www.fedlex.admin.ch/de/home?news_period=last_day&news_pageNb=1&news_order=desc&news_itemsPerPage=10
		 * MepV SR 812.213
		 */
		String uri = "https://www.fedlex.admin.ch/eli/cc/2020/552/de";
		WebDriver driver = new ChromeDriver(); 
		driver.get(uri);
		 Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
		            .withTimeout(Duration.ofSeconds(10))
		            .pollingEvery(Duration.ofSeconds(1))
		            .ignoring(NoSuchElementException.class);
		 WebElement e = wait.until(new Function<WebDriver, WebElement>() {
		        public WebElement apply(WebDriver driver) {
		            return driver.findElement(By.xpath("//*[@id=\"preface\"]/p[2]"));
		        }
		    });
		boolean ok = e.getText().equals("vom 1. Juli 2020 (Stand am 1. November 2023)");
		if (!ok) {
			common.appendProtocol(CommonFunctions.DocumentEvent.CHANGED, "MepV SR 812.213", uri, null);
		}
		driver.quit();
		return ok;
	};
	
	private SupplierE<Boolean> loadImdrf = () -> {
		/* https://www.imdrf.org/documents
		 * IMDRF - Documents
		 * IMDRF – Consultations
		 */
		ReadImdrf imdrf = new ReadImdrf();
		imdrf.collect();
		imdrf.evaluate();
		return imdrf.isOk();
	};

	private SupplierE<Boolean> loadUkLex = () -> {
		/* https://www.gov.uk/government/collections/regulatory-guidance-for-medical-devices
		 * Guidance Regulating medical devices - Regulation of medical devices from January 2021 in Great Britain and Northern Ireland
		 */
		return new FetchHtml("https://www.gov.uk/guidance/regulating-medical-devices-in-the-uk", "UK Regulation of medical devices")
		.select("dl")
		.checkIfExitsElement("Last updated")
		.checkIfExitsElement("8 February 2024")
		.isResultOK();
	};

	private SupplierE<Boolean> loadEuHealth = () -> {
		/* https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en
		 * MDCG Documents
		 */
		String uri = "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en";
		File currentHml = common.downloadHtmlToFile(uri, "EU_Health.html");
		return common.checkFiles(new File[] {currentHml}, "https://eur-lex.europa.eu");
	};

	private SupplierE<Boolean> loadBrHealth = () -> {
		/* https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2022/rdc-665-de-2022
		 * RDC 67/2009 RDC 551/2021 RDC 665/2022
		 */
		boolean ok = true;
		File file1 = common.downloadFile("https://antigo.anvisa.gov.br/documents/10181/2718376/RDC_67_2009_COMP.pdf/e2c25cc1-8bd4-4cc3-8703-c6d16641f7a0?version=1.0", CommonFunctions.DownloadDir.CONTENT, 
				"RDC 67 2009");
		File file2 = common.downloadFile("https://antigo.anvisa.gov.br/documents/10181/6320029/RDC_551_2021_.pdf/463d94a1-c365-4abd-9600-005ae7c1e22c", CommonFunctions.DownloadDir.CONTENT,
				"RDC 551 2021"); // ??? https://www.in.gov.br/en/web/dou/-/resolucao-rdc-n-551-de-30-de-agosto-de-2021-341672897
		ok &= common.checkFiles(new File[] {file1, file2}, "https://www.gov.br");
		ok &= new FetchHtml("https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2022/rdc-665-de-2022", "RDC 665/2022")
		.checkXPathTag("//*[@id=\"plone-document-byline\"]/span[1]/span[2]", "", "19/05/2022 14h19")
		.checkXPathTag("//*[@id=\"plone-document-byline\"]/span[2]/span[2]", "", "01/11/2022 09h57")
		.isResultOK();

		ok &= new FetchHtml("https://antigo.anvisa.gov.br/legislacao/?inheritRedirect=true#/visualizar/28465", "RDC 67/2009")
		.checkXPathTag("//*[@id=\"printTela\"]/div[5]/div/div/div[1]/div/div[1]/p/label", "", " 23/12/2009 ")
		.isResultOK();
		return ok;
	};

	private SupplierE<Boolean> loadCaHealth = () -> {
		/* https://laws-lois.justice.gc.ca/
		 * SOR/98-282 SOR/2020-262 not found !!!
		 *
		 * https://www.fda.gov/media/166672/download?attachment
		 * MDSAP AU P0002.008
		 */
		String uri = "https://laws-lois.justice.gc.ca/eng/regulations/sor-98-282/";
		WebDriver driver = new ChromeDriver(); 
		driver.get(uri);
		WebElement SOR98_282 = driver.findElement(By.xpath("//*[@id='assentedDate']"));
		boolean ok1 = SOR98_282.getText().contains("Regulations are current to 2024-02-20");
		boolean ok2 = SOR98_282.getText().contains("on 2024-01-03");
		if (!ok1 || !ok2) {
			common.appendProtocol(CommonFunctions.DocumentEvent.CHANGED, "SOR/98-282", uri, null);
		}
		WebElement SOR2020_197_1 = driver.findElement(By.xpath("/html/body/div/div/main/div[2]/div/table/tbody/tr[8]/td[1]"));
		boolean ok3 = "SOR/2020-262".equals(SOR2020_197_1.getText());
		WebElement SOR2020_197_2 = driver.findElement(By.xpath("/html/body/div/div/main/div[2]/div/table/tbody/tr[8]/td[2]"));
		boolean ok4 = "2021-06-23".equals(SOR2020_197_2.getText());
		if (!ok3 || !ok4) {
			common.appendProtocol(CommonFunctions.DocumentEvent.CHANGED, "SOR/2020-262", uri, "Ammendment");
		}
		driver.close();
		return ok1 & ok2 & ok3 & ok4;
	};

	private void evaluate() throws IOException {
		if (doCompare) {
			Path oldFilesDir = oldDir == null ? common.getLastDir() : oldDir;
			Path newFilesDir = newDir == null ? common.getNewFilesDir() : newDir;
			compareFiles(oldFilesDir, newFilesDir);
		}
	}

	private void compareFiles(Path oldFilesDir, Path newFilesDir) throws IOException {

		List<String> newFiles = Arrays.asList(newFilesDir.toFile().listFiles()).stream().map(x -> x.getName()).collect(Collectors.toList());
		List<String> oldFiles = Arrays.asList(oldFilesDir.toFile().listFiles()).stream().map(x -> x.getName()).collect(Collectors.toList());
		if (newFiles.isEmpty()) {
			System.out.println("No files are downloded!");
			return;
		}
		Set<String> createdSince = new TreeSet<>(newFiles);
		createdSince.removeAll(oldFiles);
		Set<String> droppedSince = new TreeSet<>(oldFiles);
		droppedSince.removeAll(newFiles);
		droppedSince.remove(CommonFunctions.getProtocollTxt());
		Collection<String> keptSince = CollectionUtils.intersection(newFiles, oldFiles);
		List<String> differentFiles = new ArrayList<>();
		try (BufferedWriter logger =  Files.newBufferedWriter(newFilesDir.resolve(CommonFunctions.getProtocollTxt()))) {
			try (BufferedWriter comparator =  Files.newBufferedWriter(newFilesDir.resolve(CommonFunctions.getComparatorCmd()))) {
				comparator.append("@set comparator=\"c:\\Program Files\\PDF24\\pdf24-Toolbox.exe\" verb=compare ");
				comparator.newLine();
				for (String keptFile : keptSince) {
					Path oldFilePath = oldFilesDir.resolve(keptFile);
					File oldFile = oldFilePath.toFile();
					File newFile = newFilesDir.resolve(keptFile).toFile();
					if (common.areFilesIdentical(oldFile, newFile)) {
						if (oldDirArg == null && newDirArg == null) {
							Files.delete(oldFilePath);
						}
					} else {
						differentFiles.add(keptFile);
						logger.append("cmp ");
						logger.append(oldFile.toString());
						logger.append(" ");
						logger.append(newFile.toString());
						comparator.append(String.format("%%comparator%% \"%s\" \"%s\"%n", oldFile.toString(), newFile.toString()));
						comparator.append("pause");
						comparator.newLine();
					}
				}
				logger.append(String.format("REM Comparing %s with %s%n", newFilesDir, oldFilesDir));
				printList(droppedSince, "Dropped", "No standard dropped!", logger);
				printList(createdSince, "New standard", "No new standard!", logger);
				printList(differentFiles, "Changed", "No changes!", logger);
			}
		}
		System.out.format("Compared %s with %s%n", newFilesDir, oldFilesDir);
	}
	

	private void printList(Collection<String> listOfFiles, String prefix, String emptyText, 
			BufferedWriter logger) throws IOException {
		int counter = 1;
		if (listOfFiles.isEmpty()) {
			System.out.println(emptyText);
			logger.append(emptyText);
			logger.newLine();
		} else {
			for (String name : listOfFiles) {
				String text = String.format("%3d %s: %s%n", counter, prefix, name);
				System.out.print(text);
				logger.append(text);
				counter++;
			}
		}
		System.out.println("");
	}

	private void printElapsedTime(long start) {
		long end = System.currentTimeMillis();
		double diff = (double) ((end - start + 500) / 1000);
		System.out.format("%.1f%n", diff/60);
	}
	
}


