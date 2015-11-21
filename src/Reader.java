
public class Reader {
	public Reader() {

	}

	public int run(String coreid, String address, int cycle) {
		Processor pro = (Processor) Simulator.processorsTable.get(coreid);
		boolean l1readHit = Util.hitOrMiss(address, pro, Simulator.n1, Simulator.a1, Simulator.b);
		if (l1readHit) {
			return readHit(address, coreid, cycle);
		} else {
			return readMiss(address, coreid, cycle);
		}
	}

	public int readHit(String address, String coreid, int cycle) {
		Util.updateLRU(address, coreid, "l1", cycle);
		return cycle;
	}

	public int readMiss(String add, String coreid, int cur_cycle) {
		String homeid = Integer.parseInt(add.substring(19 - Simulator.p + 1, 20), 2) + "";
		Processor pro = Simulator.processorsTable.get(homeid);

		if (pro.l2.directory.blocktable.contains(add)) {
			// read miss, l2 hit, check state of block
			if (pro.l2.directory.blocktable.get(add).state == Directory.SHARED_STATE) {
				// read miss, l2 shared
				cur_cycle = shared(coreid, homeid, add, cur_cycle);
			} else if (pro.l2.directory.blocktable.get(add).state == Directory.MODIFIED_STATE) {
				// read miss, l2 exclusive
				cur_cycle = exclusive(coreid, homeid, add, cur_cycle);
			}
		} else {
			// read miss, l2 uncached
			cur_cycle = uncached(coreid, homeid, add, cur_cycle);
		}
		return cur_cycle;
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
		// set state of block to "shared" in dir
		// store to l2
		// add sharer L
		cycle = cycle + Util.storeBlockToCache(address, "l2", homeid, cycle);
		processor.l2.directory.blocktable.get(address).state = Directory.SHARED_STATE;
		processor.l2.directory.blocktable.get(address).sharers.add(localid);
		cycle = cycle + local2home * Simulator.C;

		// L get data
		// store to l1
		// set state of block to "shared"
		cycle = cycle + Util.storeBlockToCache(address, "l1", localid, cycle);
		Util.setBlockStatus(localid, address, Directory.SHARED_STATE);

		return cycle;
	}

	public int shared(String localid, String homeid, String address, int cycle) {
		Processor processor = Simulator.processorsTable.get(homeid);

		// 1. L sends request to H
		int manhattanDistance = Util.getManhattanDistance(localid, homeid, Simulator.p);
		cycle = manhattanDistance * Simulator.C + cycle;

		// 2. H return block to L
		// add sharer L
		Util.updateLRU(address, homeid, "l2", cycle);
		cycle = manhattanDistance * Simulator.C + Simulator.d + cycle;
		processor.l2.directory.blocktable.get(address).sharers.add(localid);

		// L get data
		// store to l1
		// set state of block to "shared"
		cycle = cycle + Util.storeBlockToCache(address, "l1", localid, cycle);
		Util.setBlockStatus(localid, address, Directory.SHARED_STATE);

		return cycle;
	}

	public int exclusive(String localid, String homeid, String address, int cycle) {
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
		// set state of block to "shared"
		Util.setBlockStatus(remoteid, address, Directory.SHARED_STATE);
		int cycleByL = 0;
		int cycleByH = 0;
		cycleByL = Util.getManhattanDistance(localid, remoteid, Simulator.p) * Simulator.C;
		cycleByH = Util.getManhattanDistance(homeid, remoteid, Simulator.p) * Simulator.C;

		// L get block
		// store to L1
		// set state of Block to "shared"
		cycleByL = cycleByL + Util.storeBlockToCache(address, "l1", localid, cycleByL);
		Util.setBlockStatus(localid, address, Directory.SHARED_STATE);

		// H change to "shared"
		// store to L2
		// add sharer
		processor.l2.directory.blocktable.get(address).state = Directory.SHARED_STATE;
		cycleByH = cycleByH + Util.storeBlockToCache(address, "l2", homeid, cycleByH);
		processor.l2.directory.blocktable.get(address).sharers.clear();
		processor.l2.directory.blocktable.get(address).sharers.add(remoteid);
		processor.l2.directory.blocktable.get(address).sharers.add(localid);

		cycle = cycle + Math.max(cycleByL, cycleByH);
		return cycle;
	}
}
