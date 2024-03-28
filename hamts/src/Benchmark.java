// Copyright 2024 Tomoharu Ugawa
// Licensed under the Apache License, Version 2.0.

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Entity {
	Integer value;
	Entity(int v) {
		value = v;
	}
}

public class Benchmark {
	
	static final boolean VERIFY = true;
	
	static final int NTHREADS = 2;
	static final int NROUNDS = 5;
	static final int N =1000 * 1000 * 1;
	
	@durable_root
	static Hamt<Integer, Entity> map = Hamt.empty();
	static final Object lock = new Object();
	
	static int nOps = N;
	static int keyRange = N * 10;
	static int nThreads = NTHREADS;
	static int nRounds = NROUNDS;
	static int batchSize = 1;
	
	static void set(int x) {
		Entity e = new Entity(x);
		Integer k = x;
		synchronized(lock) {
			map = map.set(k, e);
		}
	}

	static Hamt<Integer, Entity> setInternal(Hamt<Integer, Entity> m, int x) {
		Entity e = new Entity(x);
		Integer k = x;
		return m.set(k, e);
	}
	
	static int get(int x) {
		Integer k = x;
		Entity e;
		synchronized(lock) {
			e = map.find(k);
		}
		if (e != null) {
			if (e.value != x)
				throw new Error();
			return e.value;
		} else
			return -1;
	}

	public static void doBenchmark(int s) {
		Random r = new Random(s);
		List<Integer> verify = null;
		if (VERIFY)
			verify = new ArrayList<Integer>();

		for (int i = 0; i < nOps / nThreads; i += batchSize) {
			if (batchSize == 1) {
				int x = r.nextInt() % keyRange;
				set(x);
				if (VERIFY)
					verify.add(x);
			} else {
				synchronized(lock) {
					Hamt<Integer, Entity> m = map;
					for (int j = 0; j < batchSize; j++) {
						int x = r.nextInt() % keyRange;
						m = setInternal(m, x);
						if (VERIFY)
							verify.add(x);
					}
					map = m;
				}
			}
		}
		if(VERIFY)
			for (int x: verify) {
				int v = get(x);
				if (v == -1 && x != -1) {
					System.out.println(x);
					throw new Error();
				}
			}
	}
	
	public static void runThreads(int n) {
		Thread[] ts = new Thread[n];
		for (int i = 0; i < n; i++) {
			final int s = i;
			Thread t = new Thread() {
				@Override
				public void run() {
					doBenchmark(s);
				}
			};
			t.start();
			ts[i] = t;
		}
		for (int i = 0; i < n; i++)
			try {
				ts[i].join();
			} catch (InterruptedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
	}
	
	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i++]) {
			case "-r":
				nRounds = Integer.parseInt(args[i]);
				break;
			case "-n":
				nOps = Integer.parseInt(args[i]);
				break;
			case "-t":
				nThreads = Integer.parseInt(args[i]);
				break;
			case "-b":
				batchSize = Integer.parseInt(args[i]);
			}
		}
			
		for (int r = 0; r < nRounds; r++) {
			long start = System.currentTimeMillis();
			if (nThreads > 1) {
				runThreads(nThreads);
			} else {
				doBenchmark(0);
			}
			long end = System.currentTimeMillis();
			System.out.println(String.format("thread %d round %d n %d time %.2f", nThreads, r, nOps, (end - start) * 0.001, args));
		}
	}
}
