import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

class IdNodePair {
	String id;
	CustomNode node;
	IdNodePair(String id, CustomNode node) {
		this.id = id;
		this.node = node;
	}
}

class CustomNode {
}

class ElementNode extends CustomNode {
	String name;
	CustomNode[] childNodes;
}

class TextNode extends CustomNode {
	String text;
}

class Parser {
	DocumentBuilder builder;
	
	Parser() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		builder = factory.newDocumentBuilder();
	}
	
	IdNodePair parse(String str) throws Exception{
		byte[] bytes = str.getBytes("UTF-8");
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		Document doc = builder.parse(is);
		Element e = doc.getDocumentElement();
		CustomNode n = domToTree(e);
		return new IdNodePair(e.getAttribute("id"), n);
	}
	
	CustomNode domToTree(Node x) {
		if (x instanceof Element) {
			Element e = (Element) x;
			ElementNode n = new ElementNode();
			n.name = e.getTagName();
			NodeList children = e.getChildNodes();
			int nc = children.getLength();
			n.childNodes = new CustomNode[nc];
			for (int i = 0; i < nc; i++)
				n.childNodes[i] = domToTree(children.item(i));
			return n;
		} else if (x instanceof Text) {
			Text e = (Text) x;
			TextNode n = new TextNode();
			n.text = e.getWholeText();
			return n;
		} else {
			System.out.println(x);
			return null;
		}
	}

}

public class Main {
	static Map<String, CustomNode> db;

	static void readXml(String filename) {
		db = new HashMap<String, CustomNode>();
		int count = 0;
		try {
			Parser parser = new Parser();
			BufferedReader in = new BufferedReader(new FileReader(filename));
			while (true) {
				String line = in.readLine();
				if (line == null)
					break;
				if (line.startsWith("<ProteinEntry ")) {
					StringBuilder b = new StringBuilder(line);
					while (true) {
						line = in.readLine();
						b.append(line);
						if (line.startsWith("</ProteinEntry>"))
							break;
					}
					IdNodePair idNode = parser.parse(b.toString());
					db.put(idNode.id, idNode.node);
					if (++count % 10000 == 0)
						System.out.println(count);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(db.size());
	}
	
	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			System.gc();
			long start = System.currentTimeMillis();
			readXml(args[0]);
			long end = System.currentTimeMillis();
			System.out.println(String.format("time %.2f", (end - start) * 0.001));
		}
	}
}