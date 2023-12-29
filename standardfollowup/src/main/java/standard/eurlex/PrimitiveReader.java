package standard.eurlex;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import standard.eurlex.dtos.DtoHelper;
import standard.eurlex.dtos.EurLexDTO;


public class PrimitiveReader {
	
	private static final boolean fromUrl = false;
	private static final boolean createClasses = false;
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final String uriString = "http://api.epdb.eu/eurlex/document/";
	private Map<String, EurLexDTO> data;
	private Map<String, EurLexDTO> reducedData;
	private String currentDate = df.format(new Date());

	public static void main(String[] args) throws URISyntaxException, ParseException, IOException, java.text.ParseException {
		String jsonString;
		PrimitiveReader r = new PrimitiveReader();
		if (fromUrl) {
			jsonString = DtoHelper.read(uriString);
		} else {
			List<String> lines = Files.readAllLines(Paths.get("C:\\temp\\eurlex.json"));
			jsonString = lines.get(0);
		}
		JSONObject root = r.convertFromJson(jsonString);
		if (createClasses) {
			DtoHelper dh = DtoHelper.get();
			dh.createClasses(root);
			dh.printClasses();
		} else {
			r.readData(root);
			r.reduceData();
			r.printAllTitles();
//			r.evaluate();
			r.selectHit();
		}
	}
	
	private JSONObject convertFromJson(String json) throws ParseException {
		JSONParser parser = new JSONParser(); 
		JSONObject jsonO = (JSONObject) parser.parse(json);
//		System.out.println(jsonO.toString());
//		jsonO.forEach((Object k, Object v) -> {
//			try {
//				decodeLevel1(k, v);
//			} catch (java.text.ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		});
		return jsonO;
	}
	
	private void readData(JSONObject root) {
		data = new HashMap<>();
		for (Object o :  root.entrySet()) {
			Map.Entry<String, Object> e = (Map.Entry<String, Object>)o;
			if (!"next".equals(e.getKey())) {
				EurLexDTO dto = new EurLexDTO(e.getKey(), (JSONObject)e.getValue());
				data.put(e.getKey(), dto);
			}
		}
	}
	
	private void reduceData() {
		reducedData = data.entrySet().stream()
				.filter(x -> !x.getValue().getText_url().isEmpty())
//				.filter(x -> currentDate.compareTo(x.getValue().getEnd_validity()) <= 0)
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
	}
	
	private void evaluate() throws java.text.ParseException, URISyntaxException {
		System.out.println("Data read: " + data.size());
		System.out.println("Data reduced: " + reducedData.size());
		for (Map.Entry<String, EurLexDTO> item : reducedData.entrySet()) {
			EurLexDTO dto = item.getValue();
			String textUri = dto.getText_url();
//			String referredText = DtoHelper.read(textUri);
			System.out.println(dto.getTitle());
//			System.out.println(referredText);
		}
	}
	
	private void printAllTitles() {
		Set<String> titles = new TreeSet<>();
		data.values().stream().map(x -> x.getTitle()).forEach(x -> titles.add(x));
		titles.stream().forEach(x -> System.out.println(x));
	}
	
	private void selectHit() {
		 List<EurLexDTO> hits = data.values().stream().filter(x -> x.getEurlex_perma_url().contains("3A32017R0745")).collect(Collectors.toList());
		 System.out.println("Found " + hits.size());
	}
}
