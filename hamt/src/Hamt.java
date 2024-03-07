// Copyright 2024 Tomoharu Ugawa
// Licensed under the Apache License, Version 2.0.
// 
// Simplified implementation of a hash array mapped trie
// in Functional Java (https://github.com/functionaljava)


class Pair<K, V> {
	final K k;
	final V v;
	public Pair(K k, V v) {
		this.k = k;
		this.v = v;
	}
}

public class Hamt<K, V> {
	
	static final boolean DEBUG = false;
	
	static final int MIN_INDEX = 0;
	static final int BITS_IN_INDEX = 5;
	static final int ARITY = 1 << BITS_IN_INDEX;
	
	private final int bits;
	private final Object[] contents;
	
	int hash(K k) {
		return k.hashCode();
	}
	
	private Object[] contentsInsert(int index, Object v) {
		Object[] a = new Object[contents.length + 1];
		for (int i = 0; i < index; i++)
			a[i] = contents[i];
		a[index] = v;
		for (int i = index; i < contents.length; i++)
			a[i + 1] = contents[i];
		return a;
	}
	
	private Object[] contentsUpdate(int index, Object v) {
		Object[] a = new Object[contents.length];
		for (int i = 0; i < index; i++)
			a[i] = contents[i];
		a[index] = v;
		for (int i = index + 1; i < contents.length; i++)
			a[i] = contents[i];
		return a;
	}

	private Hamt(int b, Object[] c) {
		bits = b;
		contents = c;
		if (DEBUG) {
			int n = Integer.bitCount(b);
			if (c.length != n)
				throw new Error();
			for (int i = 0; i < n; i++) {
				if (c[i] == null)
					throw new Error();
				if (!(c[i] instanceof Pair || c[i] instanceof Hamt))
					throw new Error();
			}
		}
	}
	
	public static <K, V> Hamt<K, V> empty() {
		return new Hamt<K, V>(0, new Object[] {});
	}
	
	public Hamt<K, V> set(K k, V v) {
		return set(k, v, MIN_INDEX);
	}
	
	private Hamt<K, V> set(K k, V v, int lowIndex) {
		int h = hash(k);
		int h1 = (h >>> lowIndex) & ((1 << BITS_IN_INDEX) - 1);
		int bit = 1 << h1;
		int index = Integer.bitCount(bits & (bit - 1));
		
		if ((bits & bit) == 0) {
			Object[] newContents = contentsInsert(index, new Pair<K, V>(k, v));
			return new Hamt<K, V>(bits | bit, newContents);
		} else {
			Object x = contents[index];
			if (x instanceof Pair) {
				@SuppressWarnings("unchecked")
				Pair<K, V> oldP = (Pair<K, V>) x;
				if (oldP.k.equals(k)) {
					// replace
					Object[] newContents = contentsUpdate(index, new Pair<K, V>(k, v));
					return new Hamt<K, V>(bits, newContents);
				} else {
					// make hamt with two pairs: oldP and current (k, v)
					Hamt<K, V> e = new Hamt<K, V>(0, new Object[] {});
					Hamt<K, V> hamt1 = e.set(oldP.k, oldP.v, lowIndex + BITS_IN_INDEX);
					Hamt<K, V> hamt2 = hamt1.set(k, v, lowIndex + BITS_IN_INDEX);
					Object[] newContents = contentsUpdate(index, hamt2);
					return new Hamt<K, V>(bits, newContents);
				}
			} else {
				@SuppressWarnings("unchecked")
				Hamt<K, V> hamt = (Hamt<K, V>) x;
				Hamt<K, V> hamt2 = hamt.set(k, v, lowIndex + BITS_IN_INDEX);
				Object[] newContents = contentsUpdate(index, hamt2);
				return new Hamt<K, V>(bits, newContents);
			}
		}
	}
	
	public V find(K k) {
		return find(k, MIN_INDEX);
	}
	
	private V find(K k, int lowIndex) {
		int h = hash(k);
		int h1 = (h >>> lowIndex) & ((1 << BITS_IN_INDEX) - 1);
		int bit = 1 << h1;
		
		if ((bits & bit) == 0)
			return null;
		else {
			int index = Integer.bitCount(bits & (bit - 1));
			Object x = contents[index];
			if (x instanceof Pair) {
				Pair<K, V> p = (Pair<K, V>) x;
				if (p.k.equals(k))
					return p.v;
				else
					return null;
			} else {
				Hamt<K, V> hamt = (Hamt<K, V>) x;
				return hamt.find(k, lowIndex + BITS_IN_INDEX);
			}
		}
	}
	
	private String spaces(int n) {
		String s = "";
		for (int i = 0; i < n; i++)
			s += " ";
		return s;
	}
	
	public String toString(int indent) {
		String s = "";
		int index = 0;
		for (int i = 0; i < ARITY; i++)
			if ((bits & (1 << i)) != 0) {
				Object x = contents[index++];
				if (x instanceof Pair) {
					Pair<K, V> p = (Pair<K, V>) x;
					s += spaces(indent) + "[" + i + "] (" + p.k + "," + p.v +")\n"; 
				} else {
					Hamt<K, V> h = (Hamt<K, V>) x;
					s += spaces(indent) + "[" + i + "] HAMT\n";
					s += h.toString(indent + 2);
				}
			}
		if (contents.length != index)
			System.out.println("wrong length of contents");
		return s;
	}
}