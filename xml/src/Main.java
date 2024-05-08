import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

class Parser {
	DocumentBuilder builder;
	
	Parser() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		builder = factory.newDocumentBuilder();
	}
	
	Element parse(String str) throws Exception{
		byte[] bytes = str.getBytes("UTF-8");
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		Document doc = builder.parse(is);
		return doc.getDocumentElement();
	}
}

public class Main {
	static Map<String, Element> db;

	static void readXml(String filename) {
		db = new HashMap<String, Element>();
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
					Element e = parser.parse(b.toString());
					String id = e.getAttribute("id");
					db.put(id, e);
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