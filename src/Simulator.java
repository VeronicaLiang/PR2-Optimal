import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

/*
 * The simulator is trace driven. That is memory load and store operations will specified in an
 * input trace file whose name is specified as the second command line input.
 */
public class Simulator {
	
	public static boolean output = true;
	/*
	 * The power of processors with a root of 2
	 */
	public static int p = 0;
	/*
	 * The power of the size of every l1 with a root of 2
	 */
	public static int n1 = 0;
	/*
	 * The power of the size of every l2 with a root of 2
	 */
	public static int n2 = 0;
	/*
	 * The size of a block
	 */
	public static int b = 0;
	/*
	 * The power of the associativity of l1 with a root of 2
	 */
	public static int a1 = 0;
	/*
	 * The power of the associativity of l2 with a root of 2
	 */
	public static int a2 = 0;
	/*
	 * The number of delay cycles caused by communicating between two nodes(a
	 * node consists of a processor and l1 cache)
	 */
	public static int C = 0;
	/*
	 * The number of cycles caused by a l2 hit(The l1 hit is satisfied in the
	 * same cycle in which it is issued)
	 */
	public static int d = 0;
	/*
	 * The number of cycles caused by a memory access
	 */
	public static int d1 = 0;

	public static HashMap<String, Processor> processorsTable = new HashMap<String, Processor>();
	public HashMap<String, TraceItem> waitingList = new HashMap<String, TraceItem>();
	//public ArrayList<TraceItem> waitingList = new ArrayList<TraceItem>();
	public Hashtable<Integer, ArrayList<String>> runningList = new Hashtable<Integer, ArrayList<String>>();
	
	public static HashMap<Integer, ArrayList<String>> outputList = new HashMap<Integer, ArrayList<String>>();
	public ArrayList<String> traceList = new ArrayList<String>();
	public Hashtable<String, String> startAndFinish = new Hashtable<String, String>();
	public static int shortCount = 0;
	public static int longCount = 0;

	public Simulator(String inputFile) {
		Hashtable<Integer, ArrayList<TraceItem>> commands = initializeUnits(inputFile);
		int clockcycle = 1;
		boolean finish = false;
		Reader reader = new Reader();
		Writer writer = new Writer();
		int finishCycle = 0;
		int lastCycle = 0;
		ArrayList<TraceItem> instructions = new ArrayList<TraceItem>();
		ArrayList<String> tags = new ArrayList<String>();
		ArrayList<TraceItem> readyList = new ArrayList<TraceItem>();
		TraceItem cur;
		ArrayList<TraceItem> removeList = new ArrayList<TraceItem>();
		while (!finish) {
			// extract all commands need to operate in this clock cycle
			if(commands.containsKey(clockcycle)){
				instructions = commands.get(clockcycle);
				for (int i = 0; i < instructions.size(); i++) {
					cur = instructions.get(i);
					if (cur.consecutive) {
						// if this trace is consecutive, then add to waiting list
						//waitingList.put(cur.previous, cur);
						//waitingList.add(cur);
					} else {
						if (cur.operationFlag == 0) {
							// Issue a read operation
							finishCycle = reader.run(cur.coreid, cur.address, clockcycle);
						} else if (cur.operationFlag == 1) {
							// Issue a write operation
							finishCycle = writer.run(cur.address, cur.coreid, clockcycle);
						}
						if (lastCycle < finishCycle) {
							lastCycle = finishCycle;
						}
						
						// add penalty count
						Util.addPenalty(cur.coreid, finishCycle - clockcycle);
						
						// add to start and finish map for output
						startAndFinish.put(cur.tag, "Start Cycle: " + clockcycle + "\tFinish Cycle: " + finishCycle + "\tCost: " + (finishCycle - clockcycle));
						
						// add to running list
						if (runningList.containsKey(finishCycle)) {
							runningList.get(finishCycle).add(cur.tag);
						} else {
							ArrayList<String> tmp = new ArrayList<String>();
							tmp.add(cur.tag);
							runningList.put(finishCycle, tmp);
						}
						
						//runningList.put(cur.tag, finishCycle);
					}
				}
				
				commands.remove(clockcycle);
				
				
			}
			boolean flag = true;
			while (runningList.containsKey(clockcycle) || flag) {
				if (runningList.containsKey(clockcycle)) {
					tags = runningList.get(clockcycle);
					for (int i = 0; i < tags.size(); i++) {
						if (waitingList.containsKey(tags.get(i))) {
							cur = waitingList.get(tags.get(i));
							if (clockcycle < cur.cycle) {
								cur.error = cur.cycle - clockcycle;
							}
							readyList.add(cur);
							waitingList.remove(tags.get(i));
						}

					}
					runningList.remove(clockcycle);
				}
				
				
				removeList = new ArrayList<TraceItem>();
				for (int i = 0; i < readyList.size(); i++) {
					cur = readyList.get(i);
					if (cur.newIssued) {
						if (cur.error > 0) {
							cur.error = cur.error - 1;
						} else {

							if (cur.operationFlag == 0) {
								// Issue a read operation
								finishCycle = reader.run(cur.coreid, cur.address, clockcycle);
							} else if (cur.operationFlag == 1) {
								// Issue a write operation
								finishCycle = writer.run(cur.address, cur.coreid, clockcycle);
							}
							if (lastCycle < finishCycle) {
								lastCycle = finishCycle;
							}
							// add penalty count
							Util.addPenalty(cur.coreid, finishCycle - clockcycle);
							// add to start and finish map for output
							startAndFinish.put(cur.tag, "Start Cycle: " + clockcycle + "\tFinish Cycle: " + finishCycle
									+ "\tCost: " + (finishCycle - clockcycle));
							// add to running list
							if (runningList.containsKey(finishCycle)) {
								runningList.get(finishCycle).add(cur.tag);
							} else {
								ArrayList<String> tmp = new ArrayList<String>();
								tmp.add(cur.tag);
								runningList.put(finishCycle, tmp);
							}
							removeList.add(cur);
						}
						cur.newIssued = false;
					} else if (flag) {
						if (cur.error > 0) {
							cur.error = cur.error - 1;
						} else {

							if (cur.operationFlag == 0) {
								// Issue a read operation
								finishCycle = reader.run(cur.coreid, cur.address, clockcycle);
							} else if (cur.operationFlag == 1) {
								// Issue a write operation
								finishCycle = writer.run(cur.address, cur.coreid, clockcycle);
							}
							if (lastCycle < finishCycle) {
								lastCycle = finishCycle;
							}
							// add penalty count
							Util.addPenalty(cur.coreid, finishCycle - clockcycle);
							// add to start and finish map for output
							startAndFinish.put(cur.tag, "Start Cycle: " + clockcycle + "\tFinish Cycle: " + finishCycle
									+ "\tCost: " + (finishCycle - clockcycle));
							// add to running list
							if (runningList.containsKey(finishCycle)) {
								runningList.get(finishCycle).add(cur.tag);
							} else {
								ArrayList<String> tmp = new ArrayList<String>();
								tmp.add(cur.tag);
								runningList.put(finishCycle, tmp);
							}
							removeList.add(cur);
						}
					}
				}
				flag = false;

				readyList.removeAll(removeList);
			}
			
			if (output) {
				Util.printOutputList(outputList, clockcycle);
			}
			
			clockcycle++;
			//System.out.println("--------------");
			//System.out.println(runningList.size());
			//System.out.println(commands.size());
			//System.out.println(waitingList.size());
			//System.out.println(readyList.size());
			if (runningList.size() == 0 && commands.size() == 0 && waitingList.size() == 0 && readyList.size() == 0) {
				finish = true;
			}
			
		}
		System.out.println("**********");
		String s = "";
		for (int i = 0; i < traceList.size(); i++) {
			s = traceList.get(i) + "\t";
			s = s + startAndFinish.get(i + "");
			System.out.println(s);
		}
		if (output) {
			System.out.println("**********");
			ArrayList<String> cores = new ArrayList<String>();
			for (int i = 0; i < Math.pow(2, Simulator.p); i++) {
				cores.add(i+"");
			}
			Util.printStateContent(cores);
		}
		
		System.out.println("**********");
		for (int i = 0; i < Math.pow(2, Simulator.p); i++) {
			Processor pro = Simulator.processorsTable.get(i + "");
			System.out.println("----------");
			System.out.println("Core: " + i);
			if (pro.l1HitCount + pro.l1MissCount != 0) {
				System.out.println("L1 hit rate is " + (pro.l1HitCount * 1.0) / ((pro.l1HitCount + pro.l1MissCount) * 1.0));
				System.out.println("L1 miss rate is " + (pro.l1MissCount * 1.0) / ((pro.l1HitCount + pro.l1MissCount) * 1.0));
				System.out.println("Average miss penalty for L1 is " + (pro.penalty * 1.0) / (pro.l1MissCount * 1.0));
			} else {
				System.out.println("No operation on L1");
			}
			if (pro.l2HitCount + pro.l2MissCount != 0) {
				System.out.println("L2 hit rate is " + (pro.l2HitCount * 1.0) / ((pro.l2HitCount + pro.l2MissCount) * 1.0));
				System.out.println("L2 miss rate is " + (pro.l2MissCount * 1.0) / ((pro.l2HitCount + pro.l2MissCount) * 1.0));
			} else {
				System.out.println("No operation on L2");
			}
			System.out.println("----------");

		}
		System.out.println("**********");
		System.out.println("Number of control messages: " + shortCount);
		System.out.println("Number of data messages: " + longCount);
		//Util.dumpOutputList(outputList, lastCycle, inputFile + "-out");

		
	}

	@SuppressWarnings({ "unchecked", "resource" })
	Hashtable<Integer, ArrayList<TraceItem>> initializeUnits(String inputFile) {
		// Initialize
		// processors===============================================================
		int base = 2;
		// the size of l1
		int sizeOfl1 = (int) Math.pow(base, n1);
		// the number of blocks in the l1=the size of l1/the size of a block
		int numberOfBlocksInL1 = sizeOfl1 / ((int) Math.pow(base, b));

		// the the associativity of l1
		int associativityOfL1 = (int) Math.pow(base, a1);
		// so the number of sets in the l1=the number of blocks in the l1/the
		// associativity of l1
		// int numberOfSetInL1 = numberOfBlocksInL1/associativityOfL1;
		int numberOfSetInL1 = (int) Math.pow(base, (n1 - a1 - b));

		// the size of l2
		int sizeOfl2 = (int) Math.pow(base, n2);
		// the number of blocks in the l2=the size of l2/the size of a block
		int numberOfBlocksInL2 = sizeOfl2 / ((int) Math.pow(base, b));

		// the the associativity of l2
		int associativityOfL2 = (int) Math.pow(base, a2);
		// so the number of sets in the l2=the number of blocks in the l2/the
		// associativity of l2
		// int numberOfSetInL2 = numberOfBlocksInL2/associativityOfL2;
		int numberOfSetInL2 = (int) Math.pow(base, (n2 - a2 - b));

		int processorsNumber = (int) Math.pow(base, Simulator.p);
		for (int i = 0; i < processorsNumber; i++) {
			Processor processor = new Processor(numberOfSetInL1, numberOfSetInL2, associativityOfL1, associativityOfL2);
			processorsTable.put(i + "", processor);
		}

		// load benchmarks, run and trace all the states
		String line = null;
		// Use a hashtable to record all commands from the trace file. The key
		// is the clock cycle, so that in each cycle
		// the commands that need to operate can be easily extracted.
		Hashtable<Integer, ArrayList<TraceItem>> commands = new Hashtable<Integer, ArrayList<TraceItem>>();
		try {
			FileReader filereader = new FileReader(inputFile);
			BufferedReader bufferedreader = new BufferedReader(filereader);
			int tag = 0;
			int maxCycle = 0;
			while ((line = bufferedreader.readLine()) != null) {
				if (!line.trim().equals("")){
					traceList.add(line);
					String[] ss = line.split("\t");
					TraceItem item = new TraceItem();
					item.cycle = Integer.parseInt(ss[0]);
					if (maxCycle < item.cycle) {
						maxCycle = item.cycle;
					}
					item.coreid = ss[1];
					item.operationFlag = Integer.parseInt(ss[2]);
//					item.address = Util.hexToBinary(ss[3].substring(2));
					item.origin = Util.hexToBinary(ss[3].substring(2));
					String pad = "";
					for (int k=0; k<b; k++){
						pad += "0";
					}
					item.address = item.origin.substring(0,31-b+1) + pad;
					item.tag = tag + "";
					tag++;
					boolean ccexist = commands.containsKey(Integer.parseInt(ss[0]));
					if (ccexist) {
						commands.get(Integer.parseInt(ss[0])).add(item);
					} else {
						ArrayList<TraceItem> tmp = new ArrayList<TraceItem>();
						tmp.add(item);
						commands.put(Integer.parseInt(ss[0]), tmp);
					}
					
					
					if (output) {
						System.out.println("read trace file line->" + "  cycle-" + item.cycle + "  coreid-" + item.coreid
								+ "  operationFlag-" + item.operationFlag + "  address-" + item.address);
					}
					
				}
				
				
			}
			// check if any operations are consecutive
			//Hashtable<String, TraceItem> htlast = new Hashtable<String, TraceItem>();
			//Hashtable<String, TraceItem> htnow = new Hashtable<String, TraceItem>();
			Hashtable<String, String> tmp = new Hashtable<String, String>();
			TraceItem[] pros = new TraceItem[(int) Math.pow(2, Simulator.p)];
			for (int i = 1; i <= maxCycle; i++) {
				if(commands.containsKey(i)) {
					ArrayList<TraceItem> items = commands.get(i);
					//htnow = new Hashtable<String, TraceItem>();
					for (int j = 0; j < items.size(); j++) {

						if (!(pros[Integer.parseInt(items.get(j).coreid)]==null)){
							items.get(j).consecutive = true;
							items.get(j).previous = pros[Integer.parseInt(items.get(j).coreid)].tag;
							items.get(j).error = items.get(j).cycle - pros[Integer.parseInt(items.get(j).coreid)].cycle;
							waitingList.put(pros[Integer.parseInt(items.get(j).coreid)].tag, items.get(j));
						}
						pros[Integer.parseInt(items.get(j).coreid)] = items.get(j);
						tmp.put(items.get(j).coreid, items.get(j).coreid);
						
						
						/**
						htnow.put(items.get(j).coreid, items.get(j));
						if (htlast.containsKey(items.get(j).coreid)) {
							items.get(j).consecutive = true;
							items.get(j).previous = htlast.get(items.get(j).coreid).tag;
							items.get(j).error = items.get(j).cycle - htlast.get(items.get(j).coreid).cycle;
							//waitingList.put(htlast.get(items.get(j).coreid).tag, items.get(j));
							if (waitingList.containsKey(htlast.get(items.get(j).coreid).tag)) {
								waitingList.get(htlast.get(items.get(j).coreid).tag).add(items.get(j));
							} else {
								ArrayList<TraceItem> tmp = new ArrayList<TraceItem>();
								tmp.add(items.get(j));
								waitingList.put(htlast.get(items.get(j).coreid).tag, tmp);
							}
						}
						**/
					}
					for (int m = 0; m < Math.pow(2, Simulator.p); m++) {
						if (!tmp.containsKey(m+"")){
							pros[m] = null;
						}
					}
					tmp.clear();
					
					//htlast = (Hashtable<String, TraceItem>) htnow.clone();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return commands;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		boolean test = true;
		String inputFile = "";
		if (test) {
			Simulator.output = true;
			inputFile = "readmiss-exclusive.txt";
			//inputFile = "/Users/colin/Documents/Work/GitHub/PR2-Optimal/tracefile";
			Simulator.p = 4;// The power of processors with a root of 2
			Simulator.n1 = 14;// The power of the size of every l1 with a root
								// of 2
			Simulator.n2 = 19;// The power of the size of every l2 with a root
								// of 2
			Simulator.b = 6;// The size of a block
			Simulator.a1 = 2;// The power of the associativity of l1 with a root
								// of 2
			Simulator.a2 = 2;// The power of the associativity of l2 with a root
								// of 2
			Simulator.C = 3;// The number of delay cycles caused by
							// communicating between two nodes(a node consists
							// of a processor and l1 cache)
			Simulator.d = 3;// The number of cycles caused by a l2 hit(The l1
							// hit is satisfied in the same cycle in which it is
							// issued)
			Simulator.d1 = 100;// The number of cycles caused by a memory access
		} else {
			inputFile = args[0];
			Simulator.p = Integer.parseInt(args[1]);// The power of processors
													// with a root
			// of 2
			Simulator.n1 = Integer.parseInt(args[2]);// The power of the size of
														// every l1
			// with a root of 2
			Simulator.n2 = Integer.parseInt(args[3]);// The power of the size of
														// every l2
			// with a root of 2
			Simulator.b = Integer.parseInt(args[4]);// The size of a block
			Simulator.a1 = Integer.parseInt(args[5]);// The power of the
														// associativity of
			// l1 with a root of 2
			Simulator.a2 = Integer.parseInt(args[6]);// The power of the
														// associativity of
			// l2 with a root of 2
			Simulator.C = Integer.parseInt(args[7]);// The number of delay
													// cycles caused
			// by communicating between two
			// nodes(a node consists of a
			// processor and l1 cache)
			Simulator.d = Integer.parseInt(args[8]);// The number of cycles
													// caused by a l2
			// hit(The l1 hit is satisfied in
			// the same cycle in which it is
			// issued)
			Simulator.d1 = Integer.parseInt(args[9]);// The number of cycles
														// caused by a
			// memory access
		}

		new Simulator(inputFile);
	}

}
