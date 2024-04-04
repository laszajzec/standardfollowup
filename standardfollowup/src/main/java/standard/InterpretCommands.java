package standard;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openqa.selenium.WebElement;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import standard.ec.publichealth.ReadPublicHealth;

public class InterpretCommands {
	
	private boolean ok = true; 
	private CheckPosition pos;
	private final String testCases;
	private final CommonFunctions common;
	
	public InterpretCommands(String testCases) {
		common = CommonFunctions.get();
		this.testCases = testCases;
	}

	public boolean execute(Path commandFile) throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
		Document document = loadCommands(commandFile);
		executeCommands(document);
		return ok;
	}
	
	private Document loadCommands(Path commandFile) throws ParserConfigurationException, SAXException, IOException {
		pos = CheckPosition.get();
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setIgnoringComments(true);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		return documentBuilder.parse(commandFile.toFile());
	}
	
	private  List<Element> getElements(Node aNode) {
		NodeList children = aNode.getChildNodes();
		List<Element> filtered = new ArrayList<>();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				filtered.add((Element)child);
			}
		}
		return filtered;
	}
	
	private void executeCommands(Document document) throws IOException, URISyntaxException {
		Element root = document.getDocumentElement();
//		echo(root);
//		NodeList regulatoryInstitutes = root.getChildNodes();
//		for (int i = 0; i < regulatoryInstitutes.getLength(); i++) {
//			Node regulatoryInstitute = regulatoryInstitutes.item(i);
//			if (regulatoryInstitute.getNodeType() == Node.ELEMENT_NODE) {
//				handleInstitute((Element)regulatoryInstitute);
//			}
//		}
		
		for (Element regulatoryInstitute : getElements(root)) {
			System.out.format("--- %s %s%n", regulatoryInstitute.getAttribute("testid"), regulatoryInstitute.getAttribute("name"))
;			handleInstitute(regulatoryInstitute);
		}
	}
	
	private void handleInstitute(Element institute) throws IOException, URISyntaxException {
		String instituteName = institute.getAttribute("name");
		String testId = institute.getAttribute("testid");
		if (testCases == null || testCases.isEmpty() || testCases.contains(testId)) {
			pos.institute = instituteName;

			NodeList sources = institute.getChildNodes();
			for (int i = 0; i < sources.getLength(); i++) {
				Node source = sources.item(i);
				switch (source.getNodeName()) {
				case "iso-reference":
					handleISO(source);
					break;
				case "imdrf-reference":
					handleImdrf();
					break;
				case "html-reference":
					handleHtml(source);
					break;
				case "file-reference":
					handleFiles(source);
					break;
				case "selenium-reference":
					handleSelenium(source);
					break;
				case "eu-health":
					handleEUHealth();
					break;
				case "#text":
					if (!source.getTextContent().trim().isEmpty()) {
						System.out.println("Ignored text: " + source.getTextContent());
					}
					break;
				default:
					System.out.println("Illegal XML tag: " + source.getNodeName());
					break;
				}
			}
		}
	}
	
	private void handleISO(Node isoNode) {
		List<String> numbers = new ArrayList<>();
		for (Element numberNode : getElements(isoNode)) {
			numbers.add(numberNode.getTextContent());
		}
//		NodeList numberNodes = isoNode.getChildNodes();
//		for (int i = 0; i < numberNodes.getLength(); i++) {
//			Node numberNode = numberNodes.item(i);
//			numbers.add(numberNode.getTextContent());
//		}
		ReadISO obj = new ReadISO();
		obj.setIsoNumbers(numbers);
		obj.collect();
		obj.removeUnchanged();
		obj.evaluate();
		ok &= obj.isOk();
	}
	
	private void handleImdrf() throws IOException {
		ReadImdrf imdrf = new ReadImdrf();
		imdrf.collect();
		imdrf.evaluate();
		ok &= imdrf.isOk();
	}
	
	private void handleEUHealth() throws IOException {
		/* type1 https://health.ec.europa.eu
		 */
		ReadPublicHealth obj = new ReadPublicHealth();
		obj.collect();
		obj.evaluate();
		ok &= obj.isOk();
	}
	
	private void handleFiles(Node sourceN) throws IOException, URISyntaxException {
		Element sourceE = (Element)sourceN;
		String uri = sourceE.getAttribute("uri");
		String fileName = sourceE.getAttribute("filename");
		pos.uri = uri;
		pos.fileName = fileName;
		CommonFunctions.DownloadDir target = fileName.endsWith(".html") || fileName.endsWith(".XML") ? CommonFunctions.DownloadDir.HTML : CommonFunctions.DownloadDir.CONTENT;
		File file = common.downloadFile(uri, target, fileName);
		ok &= common.checkFiles(new File[] {file}, uri);
	}
	
	private void handleHtml(Node sourceN) throws IOException, URISyntaxException {
//		Element sourceE = (Element)sourceN;
//		NodeList uriRefs = sourceE.getChildNodes();
//		for (int i = 0; i < uriRefs.getLength(); i++) {
//			Node uriRef = uriRefs.item(i);
//			String uri = ((Element)uriRef).getAttribute("uri");
//			pos.uri = uri;
//			handleHtmlTag(uriRef);
//		}
		for (Element uriRef : getElements(sourceN)) {
			String uri = ((Element)uriRef).getAttribute("uri");
			String fileName = uriRef.getAttribute("filename");
			pos.uri = uri;
			pos.fileName = fileName;
			handleHtmlStruct(uriRef);
		}
	}
	
	private void handleHtmlStruct(Node uriRef) throws IOException, URISyntaxException {
//		NodeList tagRefs = uriRef.getChildNodes();
//		for (int i = 0; i < tagRefs.getLength(); i++) {
//			Node tagRef = tagRefs.item(i);
//			String tagName = ((Element)tagRef).getAttribute("tagname");
//			pos.tag = tagName;
//			FetchHtml html = new FetchHtml(pos.uri, pos.fileName);
//			html.select(tagName);
//			handleTagConditions(tagRef, html);
//			ok &= html.isResultOK();
//		}		
		FetchHtml html = new FetchHtml(pos.uri, pos.fileName);
		for (Element uriStructElement : getElements(uriRef)) {
			switch (uriStructElement.getNodeName()) {
			case "select-tag":
				handleSelectedTags(uriStructElement, html);
				break;
			case "hpathequals":
				handlePathConditions(uriStructElement, html, true);
				break;
			case "hpathcontains":
				handlePathConditions(uriStructElement, html, false);
				break;
			default:
				System.out.println("Illegal XML tag: " + uriStructElement.getNodeName());
				break;
			}
			ok &= html.isResultOK();
		}
	}
	
	private void handleSelectedTags(Element tagRef, FetchHtml html) {
//		NodeList conditionRefs = tagRef.getChildNodes();
//		for (int i = 0; i < conditionRefs.getLength(); i++) {
//			Node conditionRefN = conditionRefs.item(i);
//			Element conditionRef = (Element)conditionRefN;
//			String conditionValue = conditionRef.getAttribute("value");
//			String conditionWhat = conditionRef.getAttribute("what");
//			
//			pos.what = conditionWhat;
//			String path = "";
//			String attr = "";
//			switch (conditionRef.getNodeName()) {
//			case "hexists":
//				html.checkIfExitsElement(conditionValue);
//				break;
//			case "hequals":
//				path = conditionRef.getAttribute("path");
//				attr = conditionRef.getAttribute("attr");
//				html.checkXPathTag(path, attr, conditionValue);
//				break;
//			case "hcontains":
//				path = conditionRef.getAttribute("path");
//				attr = conditionRef.getAttribute("attr");
//				html.checkXPathContains(path, conditionWhat, conditionValue);
//				break;
//			default:
//				System.out.println("Illegal XML tag: " + conditionRef.getNodeName());
//				break;
//			}
//		}		
		String tagName = tagRef.getAttribute("tagname");
		pos.tag = tagName;
		html.select(tagName);
		for (Element conditionRef : getElements(tagRef)) {
			String conditionValue = conditionRef.getAttribute("value");
			String conditionWhat = conditionRef.getAttribute("what");
			pos.what = conditionWhat;
			String attr = conditionRef.getAttribute("attr");
			switch (conditionRef.getNodeName()) {
			case "hexists":
				html.checkIfExitsElement(conditionValue);
				break;
			case "hequals":
				attr = conditionRef.getAttribute("attr");
				html.checkTagEquals(attr, conditionValue);
				break;
			case "hcontains":
				attr = conditionRef.getAttribute("attr");
				html.checkTagContains(attr, conditionValue);
				break;
			default:
				System.out.println("Illegal XML tag: " + conditionRef.getNodeName());
				break;
			}
		}
	}
	
	private void handlePathConditions(Element conditionRef, FetchHtml html, boolean equlity) {
		String path = conditionRef.getAttribute("path");
		String attr = conditionRef.getAttribute("attr");
		String conditionValue = conditionRef.getAttribute("value");
		String conditionWhat = conditionRef.getAttribute("what");
		pos.path = path;
		pos.tag = attr;
		pos.what = conditionWhat;
		if (equlity) {
			html.checkXPathEquals(path, attr, conditionValue);
		} else {
			html.checkXPathContains(path, attr, conditionValue);
		}
	}
	
	private void handleSelenium(Node sourceN) throws IOException {
		String uri = ((Element)sourceN).getAttribute("uri");
		try (Selenium selenium = new Selenium(uri)) {
//			NodeList webElements = sourceN.getChildNodes();
//			for (int i = 0; i < webElements.getLength(); i++) {
//				Node webElementN = webElements.item(i);
//				String path = ((Element)webElementN).getAttribute("path");
//				pos.path = path;
//				WebElement e = selenium.findElementByXpathwait(path);
//				handleSeleniumConditions(selenium, webElementN, e);
//			}
			for (Element webElement : getElements(sourceN)) {
				String path = webElement.getAttribute("path");
				pos.path = path;
				WebElement e = selenium.findElementByXpathwait(path);
				handleSeleniumConditions(selenium, webElement, e);
			}
		}
	}
	
	private void handleSeleniumConditions(Selenium selenium, Element webElement, WebElement e) {
//		NodeList seleniumConditions = webElement.getChildNodes();
//		for (int i = 0; i < seleniumConditions.getLength(); i++) {
//			Element seleniumCondition = (Element)(seleniumConditions.item(i));
//			String value = seleniumCondition.getAttribute("value");
//			String conditionWhat = seleniumCondition.getAttribute("what");
//			pos.what = conditionWhat;
//			switch (seleniumCondition.getNodeName()) {
//			case "sequals":
//				ok &= selenium.equalText(e, value);
//				break;
//			case "scontains":
//				ok &= selenium.containsText(e, value);
//				break;
//			default:
//				System.out.println("Illegal XML tag: " + seleniumCondition.getNodeName());
//				break;
//			}
//		}
		for (Element seleniumCondition : getElements(webElement)) {
			String value = seleniumCondition.getAttribute("value");
			String conditionWhat = seleniumCondition.getAttribute("what");
			pos.what = conditionWhat;
			switch (seleniumCondition.getNodeName()) {
			case "sequals":
				ok &= selenium.equalText(e, value);
				break;
			case "scontains":
				ok &= selenium.containsText(e, value);
				break;
			default:
				System.out.println("Illegal XML tag: " + seleniumCondition.getNodeName());
				break;
			}
		}		
	}
	
	private int indent = 0;
	private PrintStream out = System.out;
	private final String basicIndent = " ";
	
	private void echo(Node n) {
	    outputIndentation();
	    int type = n.getNodeType();

	    switch (type) {
	        case Node.ATTRIBUTE_NODE:
	            out.print("ATTR:");
	            printlnCommon(n);
	            break;

	        case Node.CDATA_SECTION_NODE:
	            out.print("CDATA:");
	            printlnCommon(n);
	            break;

	        case Node.COMMENT_NODE:
	            out.print("COMM:");
	            printlnCommon(n);
	            break;

	        case Node.DOCUMENT_FRAGMENT_NODE:
	            out.print("DOC_FRAG:");
	            printlnCommon(n);
	            break;

	        case Node.DOCUMENT_NODE:
	            out.print("DOC:");
	            printlnCommon(n);
	            break;

	        case Node.DOCUMENT_TYPE_NODE:
	            out.print("DOC_TYPE:");
	            printlnCommon(n);
	            NamedNodeMap nodeMap = ((DocumentType)n).getEntities();
	            indent += 2;
	            for (int i = 0; i < nodeMap.getLength(); i++) {
	                Entity entity = (Entity)nodeMap.item(i);
	                echo(entity);
	            }
	            indent -= 2;
	            break;

	        case Node.ELEMENT_NODE:
	            out.print("ELEM:");
	            printlnCommon(n);

	            NamedNodeMap atts = n.getAttributes();
	            indent += 2;
	            for (int i = 0; i < atts.getLength(); i++) {
	                Node att = atts.item(i);
	                echo(att);
	            }
	            indent -= 2;
	            break;

	        case Node.ENTITY_NODE:
	            out.print("ENT:");
	            printlnCommon(n);
	            break;

	        case Node.ENTITY_REFERENCE_NODE:
	            out.print("ENT_REF:");
	            printlnCommon(n);
	            break;

	        case Node.NOTATION_NODE:
	            out.print("NOTATION:");
	            printlnCommon(n);
	            break;

	        case Node.PROCESSING_INSTRUCTION_NODE:
	            out.print("PROC_INST:");
	            printlnCommon(n);
	            break;

	        case Node.TEXT_NODE:
	            out.print("TEXT:");
	            printlnCommon(n);
	            break;

	        default:
	            out.print("UNSUPPORTED NODE: " + type);
	            printlnCommon(n);
	            break;
	    }

	    indent++;
	    for (Node child = n.getFirstChild(); child != null;
	         child = child.getNextSibling()) {
	        echo(child);
	    }
	    indent--;
	}
	
	private void outputIndentation() {
	    for (int i = 0; i < indent; i++) {
	        out.print(basicIndent);
	    }
	}
	
	private void printlnCommon(Node n) {
	    out.print(" nodeName=\"" + n.getNodeName() + "\"");

	    String val = n.getNamespaceURI();
	    if (val != null) {
	        out.print(" uri=\"" + val + "\"");
	    }

	    val = n.getPrefix();

	    if (val != null) {
	        out.print(" pre=\"" + val + "\"");
	    }

	    val = n.getLocalName();
	    if (val != null) {
	        out.print(" local=\"" + val + "\"");
	    }

	    val = n.getNodeValue();
	    if (val != null) {
	        out.print(" nodeValue=");
	        if (val.trim().equals("")) {
	            // Whitespace
	            out.print("[WS]");
	        }
	        else {
	            out.print("\"" + n.getNodeValue() + "\"");
	        }
	    }
	    out.println();
	}	
}

