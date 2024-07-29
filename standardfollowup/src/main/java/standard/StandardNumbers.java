package standard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class StandardNumbers {

	private StandardNumbers() {
		standardNumbers = new ArrayList<>();
	}
	
	private static StandardNumbers INSTANCE;
	
	private List<Map.Entry<String, List<String>>> standardNumbers;
	
	public static void create(Node rootNode) {
		if (INSTANCE != null) {
			throw new IllegalArgumentException("StandardNumbers already initialized!");
		}
		INSTANCE = new StandardNumbers();
		for (Element numberNode : CommonFunctions.getElements(rootNode)) {
			Map.Entry<String, List<String>> isoNum = new SimpleEntry<>(numberNode.getAttribute("num"), CommonFunctions.buildClientList(numberNode.getAttribute("clients")));
			INSTANCE.standardNumbers.add(isoNum);
		}
	}
	
	public static boolean isOnList(String standardNumber) {
		return INSTANCE.isOnListIntern(standardNumber);
	}
	
	private boolean isOnListIntern(String standardNumber) {
		return standardNumbers.stream().anyMatch(n -> n.getKey().equals(standardNumber));
	}
}
