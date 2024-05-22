import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

class Param {
	static int rounds = 15;
	static int threads = 10;
	static String inFile;
	static Set<String> moreFields = new HashSet<String>();
	
	static void parseArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i++]) {
			case "-h":
				System.out.println("Main [options] in-file");
				System.out.println("  -r R  repeat R times");
				System.out.println("  -t N  use N threads");
				System.out.println("  -f F  take field F in addition to the default fields");
				System.out.println("default fields are `protein' and `sequence'");
				System.out.println("-f option can be given multiple times");
				break;
			case "-r":
				rounds = Integer.parseInt(args[i]);
				break;
			case "-t":
				threads = Integer.parseInt(args[i]);
				break;
			case "-f":
				moreFields.add(args[i]);
				break;
			default:
				inFile = args[i - 1];
				break;
			}
		}
	}
}

class Protein {
	String id;
	String sequence;
	CustomNode protain;
	CustomNode[] more;
	Protein(String id, CustomNode protain, String sequence) {
		this.id = id;
		this.protain = protain;
		this.sequence = sequence;
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
	
	Protein parse(String str) throws Exception{
		byte[] bytes = str.getBytes("UTF-8");
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		Document doc = builder.parse(is);
		Element e = doc.getDocumentElement();
		return domToProtein(e);
	}
	
	Protein domToProtein(Element e) {
		String id = e.getAttribute("id");
		CustomNode n = null;
		
		// protain field
		NodeList children = e.getElementsByTagName("protein");
		CustomNode protainField = domToTree(children.item(0));
		
		// sequence
		children = e.getElementsByTagName("sequence");
		Element x = (Element) children.item(0);
		children = x.getChildNodes();
		Text t = (Text) children.item(0);
		String sequence = t.getWholeText();
		
		Protein p = new Protein(id, protainField, sequence);
		
		// more fields
		int total = 0;
		for (String fn: Param.moreFields) {
			children = e.getElementsByTagName(fn);
			total += children.getLength();
		}
		p.more = new CustomNode[total];
		int i = 0;
		for (String fn: Param.moreFields) {
			children = e.getElementsByTagName(fn);
			for (int j = 0; j < children.getLength(); j++, i++)
				p.more[i] = domToTree(children.item(j));
		}

		return p;
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
	static Map<String, Protein> db;
	static Object lock = new Object();
	static int count = 0;

	static void readXml(String filename) {
		db = new HashMap<String, Protein>();
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
					Protein pro = parser.parse(b.toString());
					db.put(pro.id, pro);
					if (++count % 10000 == 0)
						System.out.println(count);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(db.size());
	}
	
	static void readXmlMT(String filename) {
		db = new HashMap<String, Protein>();

		ExecutorService pool = Executors.newFixedThreadPool(Param.threads);
		
		try {
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
					Runnable t = new Runnable() {
						@Override
						public void run() {
							try {
								Parser parser = new Parser();
								Protein pro = parser.parse(b.toString());
								synchronized (lock) {
									db.put(pro.id, pro);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
					count++;
					pool.submit(t);
				}
			}
			pool.shutdown();
			pool.awaitTermination(1000, java.util.concurrent.TimeUnit.SECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(db.size());
	}
	
	public static void main(String[] args) {
		Param.parseArgs(args);
		
		for (int i = 0; i < Param.rounds; i++) {
			System.gc();
			long start = System.currentTimeMillis();
			readXmlMT(Param.inFile);
			long end = System.currentTimeMillis();
			System.out.println(String.format("time %.2f", (end - start) * 0.001));
		}
	}
}