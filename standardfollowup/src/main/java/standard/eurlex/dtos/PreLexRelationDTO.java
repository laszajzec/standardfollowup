package standard.eurlex.dtos;

import org.json.simple.JSONObject;

public class PreLexRelationDTO extends BaseDTO {
	private final String prelex_doc_id;
	private final String prelex_api_url;

	public PreLexRelationDTO(String id, JSONObject in) {
		super(id);
		prelex_doc_id = (String) in.getOrDefault("prelex_doc_id", null);
		prelex_api_url = (String) in.getOrDefault("prelex_api_url", null);
	}
}
