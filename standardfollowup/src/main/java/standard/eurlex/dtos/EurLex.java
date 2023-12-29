package standard.eurlex.dtos;

import java.text.ParseException;

import org.json.simple.JSONObject;

public class EurLex {
	
	
	  private final org.json.simple.JSONArray eurovoc_descriptors;
	  private final java.lang.String of_effect;
	  private final java.lang.String eurlex_perma_url;
	  private final java.lang.String api_url;
	  private final org.json.simple.JSONArray legal_basis;
	  private final org.json.simple.JSONArray directory_codes;
	  private final java.lang.String title;
	  private final java.lang.String doc_id;
	  private final java.lang.String oj_date;
	  private final java.lang.String end_validity;
	  private final java.lang.String text_url;
	  private final java.lang.String internal_ref;
	  private final org.json.simple.JSONArray relationships;
	  private final java.lang.String form;
	  private final java.lang.String addressee;
	  private final java.lang.String additional_info;
	  private final java.lang.String date_document;
	  private final org.json.simple.JSONArray prelex_relation;
	  private final org.json.simple.JSONArray subject_matter;

	  public EurLex(Object k, Object inP) throws ParseException {
		  JSONObject in = (JSONObject)inP;
//		  createClass(k, in);
		  eurovoc_descriptors = (org.json.simple.JSONArray)in.getOrDefault("eurovoc_descriptors", null);
		  of_effect = (java.lang.String)in.getOrDefault("of_effect", null);
		  eurlex_perma_url = (java.lang.String)in.getOrDefault("eurlex_perma_url", null);
		  api_url = (java.lang.String)in.getOrDefault("api_url", null);
		  legal_basis = (org.json.simple.JSONArray)in.getOrDefault("legal_basis", null);
		  directory_codes = (org.json.simple.JSONArray)in.getOrDefault("directory_codes", null);
		  title = (java.lang.String)in.getOrDefault("title", null);
		  doc_id = (java.lang.String)in.getOrDefault("doc_id", null);
		  oj_date = (java.lang.String)in.getOrDefault("oj_date", null);
		  end_validity = (java.lang.String)in.getOrDefault("end_validity", null);
		  text_url = (java.lang.String)in.getOrDefault("text_url", null);
		  internal_ref = (java.lang.String)in.getOrDefault("internal_ref", null);
		  relationships = (org.json.simple.JSONArray)in.getOrDefault("relationships", null);
		  form = (java.lang.String)in.getOrDefault("form", null);
		  addressee = (java.lang.String)in.getOrDefault("addressee", null);
		  additional_info = (java.lang.String)in.getOrDefault("additional_info", null);
		  date_document = (java.lang.String)in.getOrDefault("date_document", null);
		  prelex_relation = (org.json.simple.JSONArray)in.getOrDefault("prelex_relation", null);
		  subject_matter = (org.json.simple.JSONArray)in.getOrDefault("subject_matter", null);
	  }
	

	public org.json.simple.JSONArray getEurovoc_descriptors() {
		return eurovoc_descriptors;
	}

	public java.lang.String getOf_effect() {
		return of_effect;
	}

	public java.lang.String getEurlex_perma_url() {
		return eurlex_perma_url;
	}

	public java.lang.String getApi_url() {
		return api_url;
	}

	public org.json.simple.JSONArray getLegal_basis() {
		return legal_basis;
	}

	public org.json.simple.JSONArray getDirectory_codes() {
		return directory_codes;
	}

	public java.lang.String getTitle() {
		return title;
	}

	public java.lang.String getDoc_id() {
		return doc_id;
	}

	public java.lang.String getOj_date() {
		return oj_date;
	}

	public java.lang.String getEnd_validity() {
		return end_validity;
	}

	public java.lang.String getText_url() {
		return text_url;
	}

	public java.lang.String getInternal_ref() {
		return internal_ref;
	}

	public org.json.simple.JSONArray getRelationships() {
		return relationships;
	}

	public java.lang.String getForm() {
		return form;
	}

	public java.lang.String getAddressee() {
		return addressee;
	}

	public java.lang.String getAdditional_info() {
		return additional_info;
	}

	public java.lang.String getDate_document() {
		return date_document;
	}

	public org.json.simple.JSONArray getPrelex_relation() {
		return prelex_relation;
	}

	public org.json.simple.JSONArray getSubject_matter() {
		return subject_matter;
	}

}
