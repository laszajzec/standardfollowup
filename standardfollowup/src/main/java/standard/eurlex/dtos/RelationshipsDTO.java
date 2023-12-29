package standard.eurlex.dtos;

import org.json.simple.JSONObject;

public class RelationshipsDTO extends BaseDTO {
	private final String link;
	private final String relationship;
	private final String relation;

	public RelationshipsDTO(String id, JSONObject in) {
		super(id);
		link = (String)in.getOrDefault("link", null);
		id = (String)in.getOrDefault("id", null);
		relationship = (String)in.getOrDefault("relationship", null);
		relation = (String)in.getOrDefault("relation", null);
	}

}
