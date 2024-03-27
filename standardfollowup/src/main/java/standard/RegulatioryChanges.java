package standard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
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
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

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

	@Option(names = { "-b", "--base" }, defaultValue="C:/temp/Standards", description = "Base of all collected standardsthe archive file")
	Path baseDir;

	@Option(names = { "-o", "--old" }, description = "Directory to compare with")
	String oldDirArg;

	@Option(names = { "-n", "--new" }, description = "Directory to load current state")
	String newDirArg;

	@Option(names = { "-t", "--test" }, description = "Which test shoult run", defaultValue="0123456789ABCDEF")
	String testCase;

	@Option(names = { "-l", "--lastcheck" }, description = "Date of last check as yyyymmdd")
	String dateOfLastCheckString;

	
	/**
	 * @param Directory of collected documents or c:\temp\Standards if absent
	 */
	public static void main(String[] args) throws IOException, URISyntaxException {
		int exitCode = new CommandLine(new RegulatioryChanges()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {
		doIt();
		System.out.println(" --- Finished ---");
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
//		evaluate();
		common.storeProtokoll();
	}

	private void collect() throws IOException, URISyntaxException {
		if (testCase.contains("0")) loadISO();          // + https://www.iso.org/home.html
		if (testCase.contains("1")) loadIEC();          // + https://www.iec.ch/homepage
		if (testCase.contains("2")) loadESHealthcare(); // + https://www.evs.ee/en
		if (testCase.contains("3")) loadDin();          // + https://www.din.de/de
		if (testCase.contains("4")) loadEulex();        // + https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX:32017R0745
		if (testCase.contains("5")) loadMpdg();         // + https://www.gesetze-im-internet.de/mpdg/
		if (testCase.contains("6")) loadEcfr();         // - https://www.ecfr.gov/
		if (testCase.contains("7")) loadBrHealth();     // ? https://assets.website-files.com/5eb4aa0e35e969e895a4212c/61589d59cf0e420947e1394e_RESOLUTION%20RDC%20N%20551_Field%20actions.pdf, ...
		if (testCase.contains("8")) loadCaHealth();     // + https://www.fda.gov/medical-devices/medical-device-single-audit-program-mdsap/mdsap-audit-procedures-and-forms
		if (testCase.contains("9")) loadAuHeath();      // + https://www.tga.gov.au/
		if (testCase.contains("A")) loadJpHealth();     // + https://www.pmda.go.jp/english/review-services/regulatory-info/0004.html
		if (testCase.contains("B")) loadChHealth();     // + https://www.fedlex.admin.ch/de/home?news_period=last_day&news_pageNb=1&news_order=desc&news_itemsPerPage=10
		if (testCase.contains("C")) loadImdrf();        // + https://www.imdrf.org/documents
		if (testCase.contains("D")) loadUkLex();        //   https://www.gov.uk/government/collections/regulatory-guidance-for-medical-devices
		if (testCase.contains("E")) loadEuHealth ();    // + https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en
		if (testCase.contains("F")) loadHealthcare();   // - https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en
	}
	
	private void loadISO() throws IOException, URISyntaxException {
		/* ISO https://www.iso.org/home.html
		 * 10342 10343 10938 10993-1 10993-18 10993-5 10993-7 11135 11137-1 11607-1 11607-2 11737-1 12866 12867 13485 14971 15004-1 15223-1 15223-2
		 * 17664-1 17664-2 19011 19980 20417 22665 4180 8429 8596 8612 9022-3 9801  
		 */
		ReadISO obj = new ReadISO();
		obj.collect();
		obj.removeUnchanged();
		obj.evaluate();
	}

	private void loadIEC() throws IOException, URISyntaxException {
		/* https://www.iec.ch/homepage
		 * IEC 60601-1 60601-1-2 60601-1-6 60601-1-9 60825-1 62304 62366-1 62471 TR 62366-2
		 */
		
		// 60601-1
		new FetchHtml("https://webstore.iec.ch/publication/2606", "IEC 60601-1 2005")
		.select("span")
		.checkIfExitsElement("2005-12-15")
		.checkIfExitsElement("3.0")
		.checkIfExitsElement("2028")
		.isResultOK();

		//60601-1-2
		new FetchHtml("https://webstore.iec.ch/publication/2590", "IEC 60601-1-2 2014")
		.select("span")
		.checkIfExitsElement("2014-02-25")
		.checkIfExitsElement("4.0")
		.checkIfExitsElement("2028")
		.isResultOK();

		new FetchHtml("https://webstore.iec.ch/publication/59644", "IEC 60601-1-1-2 2014-AMD1")
		.select("span")
		.checkIfExitsElement("2020-09-01")
		.checkIfExitsElement("4.0")
		.checkIfExitsElement("2028")
		.isResultOK();

		new FetchHtml("https://webstore.iec.ch/publication/59634", "IEC 60601-1 2005-AMD2")
		.select("span")
		.checkIfExitsElement("2020-08-20")
		.checkIfExitsElement("3.0")
		.checkIfExitsElement("2028")
		.isResultOK();
 		
		// 60601-1-9
		new FetchHtml("https://webstore.iec.ch/publication/2601", "IEC 60601-1-9 2007")
		.select("span")
		.checkIfExitsElement("2007-07-10")
		.checkIfExitsElement("1.0")
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
		new FetchHtml("https://webstore.iec.ch/publication/21863", "IEC 62366-1 2015")
		.select("span")
		.checkIfExitsElement("2015-02-24")
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

		// 62366-2
		new FetchHtml("https://webstore.iec.ch/publication/24664", "IEC TR 62366-2 2016")
		.select("span")
		.checkIfExitsElement("2016-04-27")
		.checkIfExitsElement("1.0")
		.checkIfExitsElement("2026")
		.isResultOK();
		
	}; // https://www.iec.ch/homepage

	private void loadESHealthcare() throws IOException, URISyntaxException {
		/* https://www.evs.ee/en
		 * EN 556-1 EN ISO 15004-2 EN 22248
		 */
		new FetchHtml("https://www.evs.ee/en/evs-en-556-1-2002", "EN 556-1")
		.select("div")
		.checkIfExitsElement("EVS-EN 556-1:2002")
		.checkIfExitsElement("Valid from 06.05.2002")
		.isResultOK();
		
		new FetchHtml("https://www.evs.ee/en/evs-en-iso-15004-2-2007", "EN ISO 15004-2")
		.select("div")
		.checkIfExitsElement("EVS-EN ISO 15004-2:2007")
		.checkIfExitsElement("Valid from 05.04.2007")
		.isResultOK();
		
		new FetchHtml("https://www.evs.ee/en/evs-en-22248-2003", "EN 22248")
		.select("div")
		.checkIfExitsElement("EVS-EN 22248:2003")
		.checkIfExitsElement("Valid from 01.09.2003")
		.isResultOK();
	}

	private void loadDin() throws IOException, URISyntaxException {
		/* https://www.din.de/de
		 * DIN 58220-3
		 */
		new FetchHtml("https://www.din.de/de/mitwirken/normenausschuesse/nafuo/veroeffentlichungen/wdc-beuth:din21:332097693", "DIN 58220-3.html")
		.select("span")
		.checkNextElement("Ausgabe", 1, "2021-04")
		.checkIfExitsElement("[AKTUELL]")
		.isResultOK();
	};
	
	private void loadEulex() throws IOException, URISyntaxException {
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
		checkFiles(new File[] {file1, file2, file3, file4}, "https://eur-lex.europa.eu");
	}
	
	private void loadMpdg() throws IOException, URISyntaxException {
		/* https://www.gesetze-im-internet.de/mpdg/
		 * MPDG MPEUAnpG MPEUAnpV
		 */
		File file1 = common.downloadFile("https://www.bundesgesundheitsministerium.de/fileadmin/Dateien/3_Downloads/Gesetze_und_Verordnungen/GuV/R/Referentenentwurf_RISG.pdf",
				CommonFunctions.DownloadDir.CONTENT, "MPEUAnpG.pdf");
		File file2 = common.downloadFile("https://www.bundesgesundheitsministerium.de/fileadmin/Dateien/3_Downloads/Gesetze_und_Verordnungen/GuV/M/RefE_MPEUAnpV.pdf",
				CommonFunctions.DownloadDir.CONTENT, "MPEUAnpV.pdf");
		checkFiles(new File[] {file1, file2}, "https://www.bundesgesundheitsministerium.de");
		//"https://www.bundesgesundheitsministerium.de/service/gesetze-und-verordnungen/detail/medizinprodukte-eu-anpassungsgesetz-mpeuanpg.html"
	};

	private void loadHealthcare() throws IOException, URISyntaxException {
		/* type1 https://health.ec.europa.eu
		 */
		ReadPublicHealth obj = new ReadPublicHealth();
		obj.collect();
	}
	
	private void loadEcfr() throws IOException, URISyntaxException {
		//TODO Blocked IP address
		/* https://www.ecfr.gov
		 * FDA 21 CFR part 803 FDA 21 CFR part 806 FDA 21 CFR part 807 FDA 21 CFR part 820 FDA 21 CFR Part 830 
		 */
//		File downloadedFile1 = common.downloadFile("https://www.govinfo.gov/content/pkg/FR-2014-02-14/pdf/2014-03279.pdf", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 803.pdf");
//		File downloadedFile0 = common.downloadFile("https://www.govinfo.gov/link/cfr/21/803?link-type=pdf&year=mostrecent", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 803-2.pdf");
//		File downloadedFile00 = common.downloadFile("https://www.govinfo.gov/content/pkg/CFR-2023-title21-vol8/pdf/CFR-2023-title21-vol8-part803.pdf", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 803-3.pdf");
//		File downloadedFile2 = common.downloadFile("https://www.govinfo.gov/link/cfr/21/806?link-type=pdf&year=mostrecent", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 806.pdf");
//		File downloadedFile3 = common.downloadFile("https://www.govinfo.gov/link/cfr/21/807?link-type=pdf&year=mostrecent", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 807.pdf");
//		File downloadedFile4 = common.downloadFile("https://www.govinfo.gov/link/cfr/21/820?link-type=pdf&year=mostrecent", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 820.pdf");
//		File downloadedFile5 = common.downloadFile("https://www.govinfo.gov/link/cfr/21/830?link-type=pdf&year=mostrecent", CommonFunctions.DownloadDir.CONTENT, "FDA 21 CFR part 830.pdf");
		//TODO not working
		new FetchHtml("https://www.ecfr.gov/current/title-21/chapter-I/subchapter-H/part-803", "FDA 21 CFR part 803")
// !!! Feb. 14, 2014
		.isResultOK();
	};
	
	private void loadAuHeath() throws IOException, URISyntaxException {
		/* https://www.tga.gov.au/
		 * Therapeutic Goods Act 1989
		 * Therapeutic Goods (Medical Devices) Regulations 2002
		 */
		new FetchHtml("https://www.legislation.gov.au/C2004A03952/latest/text", "Therapeutic Goods Act 1989.html")
		.select("span")
		.checkIfExitsElement("Latest version")
		.checkIfExitsElement("In force")
		.checkIfExitsElement("21 September 2023")
		.isResultOK();

		new FetchHtml("https://www.legislation.gov.au/F2002B00237/latest/text", "Therapeutic Goods (Medical Devices) Regulations 2002.html")
		.select("span")
		.checkIfExitsElement("Latest version")
		.checkIfExitsElement("In force")
		.checkIfExitsElement("01 January 2024")
		.isResultOK();
	};
	private void loadJpHealth() throws IOException, URISyntaxException {
		/* https://www.pmda.go.jp/english/review-services/regulatory-info/0004.html
		 * MHLW Ministerial Ordinance No. 169
		 */
		new FetchHtml("https://www.pmda.go.jp/english/review-services/regulatory-info/0004.html", "MHLW Ministerial Ordinance No 169.html")
		.select("td")
		.checkComplex("2021: The second chapter of the ordinance was revised to align with ISO13485:2016.", x -> x.lastElementSibling() == x)
		.isResultOK();
	}; // https://www.pmda.go.jp/english/review-services/regulatory-info/0004.html
	private void loadChHealth() throws IOException, URISyntaxException {
		/* https://www.fedlex.admin.ch/de/home?news_period=last_day&news_pageNb=1&news_order=desc&news_itemsPerPage=10
		 * MepV SR 812.213
		 */
		String uri = "https://www.fedlex.admin.ch/eli/cc/2020/552/de";
		WebDriver driver = new ChromeDriver(); 
		driver.get(uri);
		WebElement div = driver.findElement(By.id("preface"));
		List<WebElement> ps = div.findElements(By.tagName("p"));
		boolean ok = !ps.isEmpty() && "vom 1. Juli 2020 (Stand am 1. November 2023)".equals(ps.getLast().getText());
		if (!ok) {
			common.appendProtocol(CommonFunctions.DocumentEvent.CHANGED, "MepV SR 812.213", uri, null);
		}
		driver.quit();
	}; // https://www.fedlex.admin.ch/de/home?news_period=last_day&news_pageNb=1&news_order=desc&news_itemsPerPage=10
	private void loadImdrf() throws IOException, URISyntaxException {
		/* https://www.imdrf.org/documents
		 * IMDRF - Documents
		 * IMDRF – Consultations
		 */
		ReadImdrf imdrf = new ReadImdrf();
		imdrf.collect();
		imdrf.evaluate();
	}; // https://www.imdrf.org/documents
	private void loadUkLex() throws IOException, URISyntaxException {
		/* https://www.gov.uk/government/collections/regulatory-guidance-for-medical-devices
		 * Guidance Regulating medical devices - Regulation of medical devices from January 2021 in Great Britain and Northern Ireland
		 */
		new FetchHtml("https://www.gov.uk/guidance/regulating-medical-devices-in-the-uk", "UK Regulation of medical devices")
		.select("dl")
		.checkIfExitsElement("Last updated")
		.checkIfExitsElement("8 February 2024")
		.isResultOK();
	}; // https://www.gov.uk/government/collections/regulatory-guidance-for-medical-devices
	private void loadEuHealth() throws IOException, URISyntaxException {
		/* https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en
		 * MDCG Documents
		 */
		String uri = "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en";
		File currentHml = common.downloadHtmlToFile(uri, "EU_Health.html");
		checkFiles(new File[] {currentHml}, "https://eur-lex.europa.eu");
		
	}; // https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en

	private void loadBrHealth() throws IOException, URISyntaxException {
		/* https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2022/rdc-665-de-2022
		 * RDC 67/2009 RDC 551/2021 RDC 665/2022
		 */
		File file1 = common.downloadFile("https://antigo.anvisa.gov.br/documents/10181/2718376/RDC_67_2009_COMP.pdf/e2c25cc1-8bd4-4cc3-8703-c6d16641f7a0?version=1.0", CommonFunctions.DownloadDir.CONTENT, 
				"RDC 67 2009");
		File file2 = common.downloadFile("https://antigo.anvisa.gov.br/documents/10181/6320029/RDC_551_2021_.pdf/463d94a1-c365-4abd-9600-005ae7c1e22c", CommonFunctions.DownloadDir.CONTENT,
				"RDC 551 2021"); // ??? https://www.in.gov.br/en/web/dou/-/resolucao-rdc-n-551-de-30-de-agosto-de-2021-341672897
		checkFiles(new File[] {file1, file2}, "https://eur-lex.europa.eu");
		new FetchHtml("https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2022/rdc-665-de-2022", "RDC 665/2022")
		.select("span")
		.checkIfExitsElement("01/11/2022")
		.isResultOK();

		new FetchHtml("https://antigo.anvisa.gov.br/legislacao/?inheritRedirect=true#/visualizar/28465", "RDC 67/2009")
		.select("span")
		.checkIfExitsElement("23/12/2009")
		.checkIfExitsElement("Vigente com alteração")
		.isResultOK();

	}; // https://assets.website-files.com/5eb4aa0e35e969e895a4212c/61589d59cf0e420947e1394e_RESOLUTION%20RDC%20N%20551_Field%20actions.pdf, ...
	private void loadCaHealth() throws IOException, URISyntaxException {
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

		WebElement SOR2020_197_1 = driver.findElement(By.xpath("/html/body/div/div/main/div[2]/div/table/tbody/tr[8]/td[1]"));
		boolean ok3 = "SOR/2020-262".equals(SOR2020_197_1.getText());
		WebElement SOR2020_197_2 = driver.findElement(By.xpath("/html/body/div/div/main/div[2]/div/table/tbody/tr[8]/td[2]"));
		boolean ok4 = "2021-06-23".equals(SOR2020_197_2.getText());
		if (!ok1 || !ok2) {
			common.appendProtocol(CommonFunctions.DocumentEvent.CHANGED, "SOR/98-282", uri, null);
		} else if (!ok3 || !ok4) {
			common.appendProtocol(CommonFunctions.DocumentEvent.CHANGED, "SOR/2020-262", uri, "Ammendment");
		}
		driver.close();
		
	}; //https://www.fda.gov/medical-devices/medical-device-single-audit-program-mdsap/mdsap-audit-procedures-and-forms

	
	
	private void evaluate() throws IOException {
		Path oldFilesDir = oldDir == null ? common.getLastDir() : oldDir;
		Path newFilesDir = newDir == null ? common.getNewFilesDir() : newDir;
		compareFiles(oldFilesDir, newFilesDir);
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
	
	private void checkFiles(File[] files, String uri) throws IOException {
		for (File newFile : files) {
			Path oldFile = common.getOldEquivalent(newFile);
			if (Files.exists(oldFile)) {
				if (!common.areFilesIdentical(newFile, oldFile.toFile())) {
					CommonFunctions.get().appendProtocol(CommonFunctions.DocumentEvent.CHANGED, newFile.getName(), uri, null);
				}
			} else {
				CommonFunctions.get().appendProtocol(CommonFunctions.DocumentEvent.NEW, newFile.getName(), uri, null);
			}
		}
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

}
