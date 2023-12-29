package standard.eurlex.dtos;

import org.json.simple.JSONObject;

public class DirectoryCodeDTO extends BaseDTO {
	private final String directory_code;

	public DirectoryCodeDTO(String id, JSONObject in) {
		super(id);
		directory_code = (String)in.getOrDefault("directory_code", null);
	}

}
