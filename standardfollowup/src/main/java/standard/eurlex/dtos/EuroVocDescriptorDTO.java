package standard.eurlex.dtos;

import org.json.simple.JSONObject;

public class EuroVocDescriptorDTO extends BaseDTO {
	
	private final String eurovoc_descriptor;
	
	public EuroVocDescriptorDTO(String id, JSONObject in) {
		super(id);
		eurovoc_descriptor = (String)in.getOrDefault("eurovoc_descriptor", null);
	}

	public String getEurovoc_descriptor() {
		return eurovoc_descriptor;
	}

}
