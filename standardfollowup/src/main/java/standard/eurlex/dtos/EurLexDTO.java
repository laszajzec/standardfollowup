package standard.eurlex.dtos;

import java.util.List;

import org.json.simple.JSONObject;

public class EurLexDTO extends BaseDTO {
	private final List<EuroVocDescriptorDTO> eurovoc_descriptors;
	private final String of_effect;
	private final String eurlex_perma_url;
	private final String api_url;
	private final List<LegalBasisDTO> legal_basis;
	private final List<DirectoryCodeDTO> directory_codes;
	private final String title;
	private final String doc_id;
	private final String oj_date;
	private final String end_validity;
	private final String text_url;
	private final String internal_ref;
	private final List<RelationshipsDTO> relationships;
	private final String form;
	private final String addressee;
	private final String additional_info;
	private final String date_document;
	private final List<PreLexRelationDTO> prelex_relation;
	private final List<SubjectMatterDTO> subject_matter;

	public EurLexDTO(String id, JSONObject in) {
		super(id);
		eurovoc_descriptors = getCollection(EuroVocDescriptorDTO.class, in.getOrDefault("eurovoc_descriptors", null));
		of_effect = (String)in.getOrDefault("of_effect", null);
		eurlex_perma_url = (String)in.getOrDefault("eurlex_perma_url", null);
		api_url = (String)in.getOrDefault("api_url", null);
		legal_basis = getCollection(LegalBasisDTO.class, in.getOrDefault("legal_basis", null));
		directory_codes = getCollection(DirectoryCodeDTO.class, in.getOrDefault("directory_codes", null));
		title = (String)in.getOrDefault("title", null);
		doc_id = (String)in.getOrDefault("doc_id", null);
		oj_date = (String)in.getOrDefault("oj_date", null);
		end_validity = (String)in.getOrDefault("end_validity", null);
		text_url = (String)in.getOrDefault("text_url", null);
		internal_ref = (String)in.getOrDefault("internal_ref", null);
		relationships = getCollection(RelationshipsDTO.class, in.getOrDefault("relationships", null));
		form = (String)in.getOrDefault("form", null);
		addressee = (String)in.getOrDefault("addressee", null);
		additional_info = (String)in.getOrDefault("additional_info", null);
		date_document = (String)in.getOrDefault("date_document", null);
		prelex_relation = getCollection(PreLexRelationDTO.class, in.getOrDefault("prelex_relation", null));
		subject_matter = getCollection(SubjectMatterDTO.class, in.getOrDefault("subject_matter", null));
		id = (String)in.getOrDefault("id", null);
	}

	public List<EuroVocDescriptorDTO> getEurovoc_descriptors() {
		return eurovoc_descriptors;
	}

	public String getOf_effect() {
		return of_effect;
	}

	public String getEurlex_perma_url() {
		return eurlex_perma_url;
	}

	public String getApi_url() {
		return api_url;
	}

	public List<LegalBasisDTO> getLegal_basis() {
		return legal_basis;
	}

	public List<DirectoryCodeDTO> getDirectory_codes() {
		return directory_codes;
	}

	public String getTitle() {
		return title;
	}

	public String getDoc_id() {
		return doc_id;
	}

	public String getOj_date() {
		return oj_date;
	}

	public String getEnd_validity() {
		return end_validity;
	}

	public String getText_url() {
		return text_url;
	}

	public String getInternal_ref() {
		return internal_ref;
	}

	public List<RelationshipsDTO> getRelationships() {
		return relationships;
	}

	public String getForm() {
		return form;
	}

	public String getAddressee() {
		return addressee;
	}

	public String getAdditional_info() {
		return additional_info;
	}

	public String getDate_document() {
		return date_document;
	}

	public List<PreLexRelationDTO> getPrelex_relation() {
		return prelex_relation;
	}

	public List<SubjectMatterDTO> getSubject_matter() {
		return subject_matter;
	}

}
