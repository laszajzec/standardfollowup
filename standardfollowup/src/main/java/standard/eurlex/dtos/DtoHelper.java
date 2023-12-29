package standard.eurlex.dtos;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DtoHelper {

	private static DtoHelper instance;
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy.mm.dd");
	private Map<String, EmbeddedClass> classMap;
	private static int level = 1;

	public static DtoHelper get() {
		if (instance == null) {
			instance = new DtoHelper();
		}
		return instance;
	}
	
	public static String read(String uriString) throws ParseException, URISyntaxException {
		final URI uri = new URI(uriString);
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
		      .uri(uri)
		      .GET()
		      .build();
		String response = client.sendAsync(request, BodyHandlers.ofString())
		      .thenApply(HttpResponse::body)
		      .join();		
		return response;
	}


	public static Date toDate(Object aDate) throws ParseException {
		return df.parse((String)aDate);
	}

	public void createClasses(JSONObject root) {
		classMap = new HashMap<>();
		for (Object o :  root.entrySet()) {
			Map.Entry<String, Object> e = (Map.Entry)o;
			if (!"next".equals(e.getKey())) {
				createClass("EurLex", e.getValue());
			}
		}
		Set keys = root.keySet();
	}

	private void createClass(String classNameP, Object inP) {
		String className = classNameP + "DTO";
		EmbeddedClass ec = new EmbeddedClass(className);
		ec.create(inP);
		if (classMap.containsKey(className)) {
			EmbeddedClass oldClass = classMap.get(className);
			ec.check(oldClass);
		} else {
			classMap.put(className, ec);
		}
	}

	public static int getNextLevel() {
		level++;
		return level;
	}
	public void printClasses() {
		for (Map.Entry<String, EmbeddedClass> e : classMap.entrySet()) {
			e.getValue().printClass();
		}
	}

	private static class EmbeddedClass {
		String name;
		Map<String, JsonElement> attrs;
		public EmbeddedClass(String name) {
			this.name = name;
			this.attrs = new HashMap<>();
		}

		public void create(Object o) {
			if (o instanceof JSONObject) {
				attrs.put("id", new JsonElement("id", ""));
				((JSONObject)o).forEach((Object k, Object v) -> createVar((String)k, v));
			} else if (o instanceof JSONArray) {
				((JSONArray)o).forEach((Object ao) -> arrayElement(ao));
			} else {
				System.out.format("Unknown %s in %s%n", o, name);
			}
		}

		private void arrayElement(Object o) {
			String typ = o.getClass().getName();
			System.out.format("Array: %s%n", o);
		}

		public void check(EmbeddedClass oldClass) {
			if (!attrsAreIdentical(oldClass.attrs, this.attrs)) {
				System.out.println("Diff in elements");
			}
		}

		private boolean attrsAreIdentical(Map<String, JsonElement> oldAttrs, Map<String, JsonElement> newAttrs) {
			Collection<String> nameDiff = CollectionUtils.disjunction(oldAttrs.keySet(), newAttrs.keySet());
			return nameDiff.isEmpty();
		}

		private void createVar(String k, Object v) {
			String varName = (String)k;
			String typ = v.getClass().getName();
			JsonElement e = new JsonElement(varName, v);
			if (attrs.containsKey(varName)) {
				System.out.format("Attr redefined! Class: %s attr: %s ", name, varName);
			} else {
				attrs.put(varName, e);
			}
		}

		public void printClass() {
			System.out.format("\tclass %s {%n", name);
			for (Map.Entry<String, JsonElement> e : attrs.entrySet()) {
				e.getValue().printDecl();
			}
			System.out.println("");
			for (Map.Entry<String, JsonElement> e : attrs.entrySet()) {
				e.getValue().printRef();
			}
			System.out.format("\t}%n");
		}
	}

	private static class JsonElement {
		String name;
		String typ;
		String decl;
		String ref;
		boolean isPrimitive;
		boolean isCollection;

		public JsonElement(String name, Object content) {
			this.name = name;
			typ = content.getClass().getName();
			checkType(name, content);
			decl = "  private final " + typ + " " + name + ";";
			if (isCollection) {
				ref = "  " + name + " = (" + typ + ")getCollection((in.getOrDefault(\"" + name + "\", null));";
			} else {
				ref = "  " + name + " = (" + typ + ")in.getOrDefault(\"" + name + "\", null);";
			}
		}

		private void checkType(String name, Object content) {
			switch (typ) {
			case "java.lang.String": 
				typ = "String";
				isPrimitive = true;
				break;
			case "org.json.simple.JSONArray":
				JSONArray jsonArray = (JSONArray)content;
				int nextLevel = DtoHelper.getNextLevel();
				String newName = name.toUpperCase();
				typ = newName + "[]";
				isPrimitive = false;
				isCollection = true;
				if (!jsonArray.isEmpty()) {
					DtoHelper.get().createClass(newName, jsonArray.getFirst());
				}
			}
		}

		public void printDecl() {
			System.out.format("\t\t%s%n", decl);
		}

		public void printRef() {
			System.out.format("\t\t%s%n", ref);
		}
	}

}
