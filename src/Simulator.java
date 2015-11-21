import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IntSummaryStatistics;

/*
 * The simulator is trace driven. That is memory load and store operations will specified in an
 * input trace file whose name is specified as the second command line input.
 */
public class Simulator {
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
	public static HashMap<Integer, ArrayList<String>> outputList = new HashMap<Integer, ArrayList<String>>();
	public static ArrayList<TraceItem> waitingList = new ArrayList<TraceItem>();
	public static Hashtable<String, Integer> runningList = new Hashtable<String, Integer>();

	public Simulator(String inputFile) {
		Hashtable<Integer, ArrayList<TraceItem>> commands = initializeUnits(inputFile);
		int clockcycle = 1;
		boolean finish = false;
		Reader reader = new Reader();
		Writer writer = new Writer();
		int finishCycle = 0;
		int lastCycle = 0;
		while (!finish) {
			// extract all commands need to operate in this clock cycle
			if(commands.containsKey(clockcycle)){
				ArrayList<TraceItem> instructions = new ArrayList<TraceItem>();
				instructions = commands.get(clockcycle);
				for (int i = 0; i < instructions.size(); i++) {
					TraceItem cur = instructions.get(i);
					if (cur.consecutive) {
						// if this trace is consecutive, then add to waiting list
						waitingList.add(cur);
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
						runningList.put(cur.tag, finishCycle);
					}
				}
				for (int i = 0; i < waitingList.size(); i++) {
					TraceItem cur = waitingList.get(i);
					if (runningList.get(cur.tag) <= clockcycle && !cur.issued) {
						if (cur.error > 0) {
							cur.error = cur.error - 1;
						} else {
							cur.issued = true;
							runningList.put(cur.tag, 0);
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
							runningList.put(cur.tag, finishCycle);
						}

					}
				}
				commands.remove(String.valueOf(clockcycle));
			}
			
			
			clockcycle++;
			if (commands.size() == 0) {
				finish = true;
				for (int i = 0; i < waitingList.size(); i++) {
					if (!waitingList.get(i).issued) {
						finish = false;
						break;
					}
				}
			}
			
		}

		Util.printOutputList(outputList, lastCycle);
	}

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

		// the size of l1
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
			while ((line = bufferedreader.readLine()) != null) {
				String[] ss = line.split(" ");
				TraceItem item = new TraceItem();
				item.cycle = Integer.parseInt(ss[0]);
				item.coreid = ss[1];
				item.operationFlag = Integer.parseInt(ss[2]);
				item.address = Util.hexToBinary(ss[3].substring(2));
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
				// traceList.add(item);
				System.out.println("read trace file line->" + "  cycle-" + item.cycle + "  coreid-" + item.coreid
						+ "  operationFlag-" + item.operationFlag + "  address-" + item.address);
				
			}
			// check if any operations are consecutive
			Hashtable<String, TraceItem> htlast = new Hashtable<String, TraceItem>();
			Hashtable<String, TraceItem> htnow = new Hashtable<String, TraceItem>();
			for (int i = 0; i < commands.size(); i++) {
				if(commands.containsKey(i)) {
					ArrayList<TraceItem> items = commands.get(i);
					htnow = new Hashtable<String, TraceItem>();
					for (int j = 0; j < items.size(); j++) {
						htnow.put(items.get(j).coreid, items.get(j));
						if (htlast.containsKey(items.get(j).coreid)) {
							items.get(j).consecutive = true;
							items.get(j).previous = htlast.get(items.get(j).coreid).tag;
							items.get(j).error = items.get(j).cycle - htlast.get(items.get(j).coreid).cycle;
						}
					}
					htlast = (Hashtable<String, TraceItem>) htnow.clone();
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
			inputFile = "tracefile";
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
