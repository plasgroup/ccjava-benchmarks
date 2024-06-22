import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
//import java.util.HashMap;
import java.util.HashSet;
//import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

class Param {
	static int rounds = 15;
	static int threads = 30;
	static String inFile;
	static Set<String> moreFields = new HashSet<String>();
	
	static void parseArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i++]) {
			case "-h":
				System.out.println("Main [options] in-file");
				System.out.println("  -r R  repeat R times");
				System.out.println("  -t N  use N threads");
				System.out.println("  -f F  take field F in addition to the default fields [disabled]");
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

/*
class Executor {
	static class Worker extends Thread {
		int id;
		Executor executor;
		Runnable task;
		boolean done = false;  // protected by the Executor's monitor
		
		Worker(int id, Executor executor, Runnable task) {
			this.id = id;
			this.executor = executor;
			this.task = task;
		}

		@Override
		public void run() {
			task.run();
			executor.notifyDone(this);
		}
	}
	
	Worker[] workers;
	
	Executor(int n) {
		workers = new Worker[n];
	}
	
	synchronized void notifyDone(Worker worker) {
		worker.done = true;
		notifyAll();
	}
	
	synchronized int findWorkerSlot() {
		while (true) {
			for (int i = 0; i < workers.length; i++) {
				if (workers[i] == null)
					return i;
				if (workers[i].done)
					return i;
			}
			try {
				wait();
			} catch (Exception e) {
				e.printStackTrace();
			}
				
		}
		
	}
	
	synchronized void exec(Runnable task) {
		int slot = findWorkerSlot();
		if (workers[slot] != null)
			try {
				workers[slot].join();
			} catch (Exception e) {
				e.printStackTrace();
			}
		Worker worker = new Worker(slot, this, task);
		workers[slot] = worker;
		worker.start();
	}
	
	
	void join() {
		for (int i = 0; i < workers.length; i++)
			if (workers[i] != null)
				try {
					workers[i].join();
				} catch (Exception e) {
					e.printStackTrace();
				}
	}
}
*/


class Executor {
	static final Runnable TERMINATE = new Runnable() { public void run() {} };
	
	static class Worker extends Thread {
		int id;
		Executor executor;
		Runnable task;
		boolean done = false;  // protected by the Executor's monitor
		
		Worker(int id, Executor executor) {
			this.id = id;
			this.executor = executor;
		}

		@Override
		public void run() {
			try {
				while (true) {
					synchronized (executor) {
						while (task == null)
							executor.wait();
					}
					if (task == TERMINATE) {
						synchronized (executor) {
							task = null;
							executor.notifyAll();
						}
						return;
					}
					task.run();
					synchronized (executor) {
						task = null;
						executor.notifyAll();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		boolean exec(Runnable task) {
			if (this.task == null) {
				this.task = task;
				executor.notifyAll();
				return true;
			}
			return false;
		}
		
		void terminate() {
			try {
				synchronized (executor) {
					while (this.task != null) 
						executor.wait();
					this.task = TERMINATE;
					executor.notifyAll();
				}
				join();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	Worker[] workers;
	
	Executor(int n) {
		workers = new Worker[n];
		for (int i = 0; i < n; i++) {
			workers[i] = new Worker(i, this);
			workers[i].start();
		}
	}
	
	synchronized void exec(Runnable task) {
		try {
			while (true) {
				for (int i = 0; i < workers.length; i++)
					if (workers[i].exec(task))
						return;
				wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void join() {
		for (int i = 0; i < workers.length; i++)
			workers[i].terminate();
	}
}

class Protein {
	String id;
	String sequence;
	CustomNode protain;
//	CustomNode[] more;
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
	
	CustomNode[] takeMoreFields(Element e) {
		int total = 0;
		for (String fn: Param.moreFields) {
			NodeList children = e.getElementsByTagName(fn);
			total += children.getLength();
		}
		CustomNode[] more = new CustomNode[total];
		int i = 0;
		for (String fn: Param.moreFields) {
			NodeList children = e.getElementsByTagName(fn);
			for (int j = 0; j < children.getLength(); j++, i++)
				more[i] = domToTree(children.item(j));
		}
		return more;
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
		//p.more = takeMoreFields(e);

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
	static CustomHashMap<String, Protein> db;
	static Object lock = new Object();
	static int count = 0;
	
	static void readXmlMT(String filename) {
		db = new CustomHashMap<String, Protein>();
		Executor pool = new Executor(Param.threads);
		
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
//					if (count % 1000 == 0)
//						System.out.println(count);
					pool.exec(t);
				}
			}
			pool.join();
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


class CustomHashMap<K, V>{
    private static final int INITIAL_CAPACITY = 1 << 4;  
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    private Entry[] hashtable;
    private int size;

    public CustomHashMap(){
        hashtable = new Entry[INITIAL_CAPACITY];
        size = 0;
    }

    public CustomHashMap(Integer capacity){
        int cap = tableSizeFor(capacity);
        hashtable = new Entry[cap];
        size = 0;
    }

    private int tableSizeFor(Integer cap){
        int n = -1 >>> Integer.numberOfLeadingZeros(cap-1);
        return n < 0 ? 1 : (n >= MAXIMUM_CAPACITY ? MAXIMUM_CAPACITY: n + 1);
    }

    class Entry<K, V>{
        public K key;
        public V value;
        public Entry next;

        Entry(K key, V value){
            this.key = key;
            this.value = value;
        }
    }
    final int hash(Object key){
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    public void put(K key, V value){
        int hashCode = hash(key) & (hashtable.length - 1);
        Entry node = hashtable[hashCode];
        if(node == null){
            Entry newNode = new Entry(key, value);
            hashtable[hashCode] = newNode;
            size++;
        }else{
            Entry prevNode = node;
            while (node != null){
                if(node.key.equals(key)){
                    node.value = value;
                    return;
                }
                prevNode = node;
                node = node.next;
            }
            Entry newNode = new Entry(key, value);
            prevNode.next = newNode;
            size++;
        }
    }

    public V get(K key){
        int hashCode = hash(key) & (hashtable.length - 1);
        Entry node = hashtable[hashCode];
        while(node != null){
            if(node.key.equals(key)){
                return (V) node.value;
            }
            node = node.next;
        }
        return null;
    }

    public int size(){
    	return size;
    }
}