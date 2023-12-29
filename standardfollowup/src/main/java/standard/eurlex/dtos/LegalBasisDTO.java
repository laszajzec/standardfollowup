package standard.eurlex.dtos;

import org.json.simple.JSONObject;

public class LegalBasisDTO extends BaseDTO {
	private final String legal_basis;

	public LegalBasisDTO(String id, JSONObject in) {
		super(id);
		legal_basis = (String)in.getOrDefault("legal_basis", null);
	}

}
