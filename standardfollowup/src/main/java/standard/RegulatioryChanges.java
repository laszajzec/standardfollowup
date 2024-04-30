package standard;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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

	@Option(names = { "-s", "--settings" }, description = "XML settings file")
	String settingFileArg;

	/**
	 * @param Directory of collected documents or c:\temp\Standards if absent
	 */
	public static void main(String[] args) throws IOException, URISyntaxException {
		int exitCode = new CommandLine(new RegulatioryChanges()).execute(args);
		System.exit(exitCode);
		new RegulatioryChanges();  // For UCD only
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

	private void doIt() throws IOException, URISyntaxException, ParserConfigurationException, SAXException {
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
		if (oldDirArg == null) {
			common.getLastDir();
		}
		doitNew();
		common.storeProtokoll();
	}
	

	private void doitNew() throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
		InterpretCommands interpreter = new InterpretCommands(testCase);
		interpreter.execute(Paths.get(settingFileArg));
	}
	
	
	private void printElapsedTime(long start) {
		long end = System.currentTimeMillis();
		double diff = (double) ((end - start + 500) / 1000);
		System.out.format("%.1f%n", diff/60);
	}
	
}


