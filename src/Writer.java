
public class Writer {
	public Writer() {

	}

	public int run(String add, String coreid, int cycle) {
		// run write operation, and return the number of finishing cycle
		// if the operation is miss or hit
		Processor pro = Simulator.processorsTable.get(coreid);
		String homeid = Integer.parseInt(add.substring(19 - Simulator.p + 1, 20), 2) + "";
		Processor homeProcessor = Simulator.processorsTable.get(homeid);
		boolean hit = Util.hitOrMiss(add, pro, Simulator.n1, Simulator.a1, Simulator.b);

		if (hit) {
			int blockStatus = Util.getBlockStatus(coreid, add);
			if (blockStatus == Directory.MODIFIED_STATE) {
				// write hit, exclusive
				return hitExclusive(coreid, add, cycle);
			} else {
				// write hit, shared
				return share(coreid, homeid, add, cycle, hit);
			}
		} else {

			if (homeProcessor.l2.directory.blocktable.contains(add)) {
				// write miss, l2 hit
				if (homeProcessor.l2.directory.blocktable.get(add).state == Directory.MODIFIED_STATE) {
					// write miss, l2 exclusive
					return missExclusive(coreid, homeid, add, cycle);
				} else {
					// write miss, l2 shared
					return share(coreid, homeid, add, cycle, hit);
				}
			} else {
				// write miss, l2 uncached
				return uncached(coreid, homeid, add, cycle);
			}
		}
	}

	public int hitExclusive(String localid, String address, int cycle) {
		Util.updateLRU(address, localid, "l1", cycle);
		return cycle;
	}

	public int uncached(String localid, String homeid, String address, int cycle) {
		int local2home = Util.getManhattanDistance(localid, homeid, Simulator.p);
		int home2controller = Util.getManhattanDistance(homeid, "0", Simulator.p);
		Processor processor = Simulator.processorsTable.get(homeid);

		// 1. L sends request to H
		cycle = cycle + local2home * Simulator.C;

		// 2. H sends request to 0
		cycle = cycle + home2controller * Simulator.C;

		// 3. 0 get data from mem
		// return to H
		cycle = cycle + Simulator.d1 + home2controller * Simulator.C;

		// 4. H get data, return to L
		// set state of block to "exclusive" in dir
		// store to l2
		// add sharer L
		cycle = cycle + Util.storeBlockToCache(address, "l2", homeid, cycle);
		processor.l2.directory.blocktable.get(address).state = Directory.MODIFIED_STATE;
		processor.l2.directory.blocktable.get(address).sharers.add(localid);
		cycle = cycle + local2home * Simulator.C;

		// L get data
		// store to l1
		// set state of block to "exclusive"
		cycle = cycle + Util.storeBlockToCache(address, "l1", localid, cycle);
		Util.setBlockStatus(localid, address, Directory.MODIFIED_STATE);

		return cycle;
	}

	public int missExclusive(String localid, String homeid, String address, int cycle) {
		Processor processor = Simulator.processorsTable.get(homeid);

		// 1. L sends request to H
		int manhattanDistance = Util.getManhattanDistance(localid, homeid, Simulator.p);
		cycle = manhattanDistance * Simulator.C + cycle;

		// 2. H return owner to L
		cycle = manhattanDistance * Simulator.C + cycle;

		// 3. L get R, sends request to R
		String remoteid = processor.l2.directory.blocktable.get(address).owner;
		manhattanDistance = Util.getManhattanDistance(localid, remoteid, Simulator.p);
		cycle = manhattanDistance * Simulator.C + cycle;

		// 4. R sends block to L and H
		// set state of block to "invalid"
		Util.setBlockStatus(remoteid, address, Directory.INVALID_STATE);
		int cycleByL = 0;
		int cycleByH = 0;
		cycleByL = Util.getManhattanDistance(localid, remoteid, Simulator.p) * Simulator.C;
		cycleByH = Util.getManhattanDistance(homeid, remoteid, Simulator.p) * Simulator.C;

		// L get block
		// store to L1
		// set state of Block to "exclusive"
		cycleByL = cycleByL + Util.storeBlockToCache(address, "l1", localid, cycleByL);
		Util.setBlockStatus(localid, address, Directory.MODIFIED_STATE);

		// L get data perform write
		Util.updateLRU(address, localid, "l1", cycle + cycleByL);

		// H change to "exclusive"
		// store to L2
		// add sharer
		processor.l2.directory.blocktable.get(address).state = Directory.MODIFIED_STATE;
		Util.updateLRU(address, homeid, "l2", cycle + cycleByH);
		processor.l2.directory.blocktable.get(address).owner = localid;

		cycle = cycle + Math.max(cycleByL, cycleByH);
		return cycle;
	}

	public int share(String localid, String homeid, String address, int cycle, boolean hit) {
		Processor Processor = Simulator.processorsTable.get(homeid);
		int manhattanDistance = Util.getManhattanDistance(localid, homeid, Simulator.p);
		// 1. L sends request to H
		cycle = cycle + manhattanDistance * Simulator.C;

		// 2. H return sharers list to L.
		// set block state to "exclusive"
		// change owner to L
		Util.updateLRU(address, homeid, "l2", cycle);
		Processor.l2.directory.blocktable.get(address).owner = localid;
		Processor.l2.directory.blocktable.get(address).state = Directory.MODIFIED_STATE;
		if (hit) {
			// return a small message
		} else {
			// return a large message
		}
		cycle = cycle + manhattanDistance * Simulator.C;

		// 3. L sends invalidating message to sharers
		// set state to exclusive
		if (!hit) {
			cycle = cycle + Util.storeBlockToCache(address, "l1", localid, cycle);
		}
		Util.setBlockStatus(localid, address, Directory.MODIFIED_STATE);
		int longestLatency = 0;
		for (int i = 0; i < Processor.l2.directory.blocktable.get(address).sharers.size(); i++) {
			int latency = Util.getManhattanDistance(localid,
					Processor.l2.directory.blocktable.get(address).sharers.get(i), Simulator.p);
			if (latency > longestLatency) {
				longestLatency = latency;
			}
		}
		cycle = cycle + longestLatency * Simulator.C;

		// 4. R sets block state to "invalid"
		// send ack to L
		for (int i = 0; i < Processor.l2.directory.blocktable.get(address).sharers.size(); i++) {
			String rn = Processor.l2.directory.blocktable.get(address).sharers.get(i);
			// set remote blocks' state to invalid
			Util.setBlockStatus(rn, address, Directory.INVALID_STATE);
		}
		cycle = cycle + longestLatency * Simulator.C;

		// L get ack, perform write
		Util.updateLRU(address, localid, "l1", cycle);

		return cycle;
	}

}
