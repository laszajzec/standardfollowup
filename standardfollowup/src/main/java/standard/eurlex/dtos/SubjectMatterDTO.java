package standard.eurlex.dtos;

import org.json.simple.JSONObject;

public class SubjectMatterDTO extends BaseDTO {
	private final String subject_matter;

	public SubjectMatterDTO(String id, JSONObject in) {
		super(id);
		subject_matter = (String)in.getOrDefault("subject_matter", null);
		id = (String)in.getOrDefault("id", null);
	}

}
