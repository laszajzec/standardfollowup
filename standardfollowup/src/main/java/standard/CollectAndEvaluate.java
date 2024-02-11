package standard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import standard.ec.publichealth.ReadPublicHealth;

/**
 * Collect documents of medical regulation
 * Compare with last verions and report differences
 */
@Command(name = "checksum", mixinStandardHelpOptions = true, version = "checksum 4.0", description = "Prints the checksum (SHA-256 by default) of a file to STDOUT.")
public class CollectAndEvaluate implements Callable<Integer> {

	private static final String PROTOCOLL_TXT = "protocoll.txt";
	private static final String COMPARATOR_CMD = "compare.cmd";
	private CommonFunctions common;
	private Path oldDir;
	private Path newDir;

	@Option(names = { "-b", "--base" }, defaultValue="C:/temp/Standards", description = "Base of all collected standardsthe archive file")
	Path baseDir;

	@Option(names = { "-o", "--old" }, description = "Directory to compare with")
	String oldDirArg;

	@Option(names = { "-n", "--new" }, description = "Directory to compare with")
	String newDirArg;

	/**
	 * @param Directory of collected documents or c:\temp\Standards if absent
	 */
	public static void main(String[] args) throws IOException, URISyntaxException {
		int exitCode = new CommandLine(new CollectAndEvaluate()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {
		doIt();
		return 0;
	}

	public CollectAndEvaluate() throws IOException, URISyntaxException {
	}

	private void doIt() throws IOException, URISyntaxException {
		common = new CommonFunctions("PublicHealth", "https://health.ec.europa.eu", baseDir);
		oldDir = oldDirArg == null ? null : common.pool.resolve(oldDirArg);
		if (oldDir != null && !Files.isDirectory(oldDir)) {
			System.out.println("Not correct directory: " + oldDirArg);
			throw new FileNotFoundException(oldDirArg);
		}
		newDir = newDirArg == null ? null : common.pool.resolve(newDirArg);
		if (newDir != null && !Files.isDirectory(newDir)) {
			System.out.println("Not correct directory: " + newDirArg);
			throw new FileNotFoundException(oldDirArg);
		}

		if (newDirArg == null) {
			common.createNewDir();
			collect();
		}
		evaluate();
	}

	private void collect() throws IOException, URISyntaxException {
		loadEulex();
		loadHealthcare();
		loadEVS();

	}

	private void loadEulex() throws IOException, URISyntaxException {
		//		common.downloadFile("https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX:32017R0745");
		URI uri = new URI("https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX:32017R0745");
		Path targetFile = common.getNewFilesDir().resolve("EU-Medizinprodukte.pdf");
		FileUtils.copyURLToFile(uri.toURL(), targetFile.toFile());
	}

	private void loadHealthcare() throws IOException, URISyntaxException {
		ReadPublicHealth obj = new ReadPublicHealth();
		obj.downloadFiles();
	}

	private void loadEVS() {

	}

	private void evaluate() throws IOException {
		Path oldFilesDir = oldDir == null ? common.getLastButOneDir() : oldDir;
		Path newFilesDir = newDir == null ? common.newFilesDir : newDir;
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
		droppedSince.remove(PROTOCOLL_TXT);
		Collection<String> keptSince = CollectionUtils.intersection(newFiles, oldFiles);
		List<String> differentFiles = new ArrayList<>();
		try (BufferedWriter logger =  Files.newBufferedWriter(newFilesDir.resolve(PROTOCOLL_TXT))) {
			try (BufferedWriter comparator =  Files.newBufferedWriter(newFilesDir.resolve(COMPARATOR_CMD))) {
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


}
