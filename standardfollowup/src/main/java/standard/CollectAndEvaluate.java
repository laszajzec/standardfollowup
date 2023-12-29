package standard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import standard.ec.publichealth.ReadPublicHealth;

public class CollectAndEvaluate {

	private final CommonFunctions common;
	
	public static void main(String[] args) throws IOException, URISyntaxException {
		new CollectAndEvaluate(args);
	}
	
	public CollectAndEvaluate(String[] args) throws IOException, URISyntaxException {
		common = new CommonFunctions("PublicHealth", "https://health.ec.europa.eu");
		collect();
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
		compareFiles();
	}
	
	private void compareFiles() throws IOException {
		List<File> subdirs = new ArrayList<>(Arrays.asList(common.getPool().toFile().listFiles()));
		Collections.sort(subdirs);
		Path oldFilesDir = subdirs.get(subdirs.size()-2).toPath();
		Path newFilesDir = subdirs.get(subdirs.size()-1).toPath();

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
		Collection<String> keptSince = CollectionUtils.intersection(newFiles, oldFiles);
		List<String> differentFiles = new ArrayList<>();
		try (BufferedWriter writer =  Files.newBufferedWriter(newFilesDir.resolve("protocoll.txt"))) {
			for (String keptFile : keptSince) {
				File oldFile = oldFilesDir.resolve(keptFile).toFile();
				File newFile = newFilesDir.resolve(keptFile).toFile();
				if (common.areFilesIdentical(oldFile, newFile)) {
					//				Files.delete(oldFilesDir);
				} else {
					differentFiles.add(keptFile);
					writer.append("cmp ");
					writer.append(oldFile.toString());
					writer.append(" ");
					writer.append(newFile.toString());
				}
			}
			writer.append(String.format("REM Comparing %s with %s%n", newFilesDir, oldFilesDir));
			printList(droppedSince, "Dropped", "No standard dropped!", writer);
			printList(createdSince, "New standard", "No new standard!", writer);
			printList(differentFiles, "Changed", "No changes!", writer);
		}
		System.out.format("Comparing %s with %s%n", newFilesDir, oldFilesDir);

	}
	
	private void printList(Collection<String> list, String prefix, String emptyText, BufferedWriter writer) throws IOException {
		int counter = 1;
		if (list.isEmpty()) {
			System.out.println(emptyText);
			writer.append(emptyText);
			writer.newLine();
		} else {
			for (String name : list) {
				String text = String.format("%3d %s: %s%n", counter, prefix, name);
				System.out.print(text);
				writer.append(text);
				counter++;
			}
		}
		System.out.println("");
	}


}
