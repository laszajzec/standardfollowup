package standard.eurlex.dtos;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public abstract class BaseDTO {
	
	private final String id;
	
	public BaseDTO(String id) {
		this.id = id;
	}

	protected <T> List<T> getCollection(Class<T> klass, Object o) {
		JSONArray a = (JSONArray)o;
		List<T> retList = new ArrayList<>(a.size());
		for (int i = 0; i < a.size(); i++) {
			try {
				T xx = klass.getConstructor(String.class, JSONObject.class).newInstance("" + i, a.get(i));
				retList.add(xx);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return retList;
	}

	public String getId() {
		return id;
	}

}
