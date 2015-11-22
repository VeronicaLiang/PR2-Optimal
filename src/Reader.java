
public class Reader {
	public Reader() {

	}

	public int run(String coreid, String address, int cycle) {
		Processor pro = (Processor) Simulator.processorsTable.get(coreid);
		boolean l1readHit = Util.hitOrMiss(address, pro, Simulator.n1, Simulator.a1, Simulator.b);
		if (l1readHit) {
			Util.addCount(coreid, 1, true);
			return readHit(address, coreid, cycle);
		} else {
			Util.addCount(coreid, 1, false);
			return readMiss(address, coreid, cycle);
		}
	}

	public int readHit(String address, String coreid, int cycle) {
		Util.updateLRU(address, coreid, "l1", cycle);
		String str = coreid + ": L1 read hit, read block.";
		Util.addOutput(cycle, str);
		return cycle;
	}

	public int readMiss(String add, String coreid, int cur_cycle) {
		String homeid = Integer.parseInt(add.substring(19 - Simulator.p + 1, 20), 2) + "";
		Processor pro = Simulator.processorsTable.get(homeid);

		if (pro.l2.directory.blocktable.containsKey(add)) {
			// read miss, l2 hit, check state of block
			Util.addCount(homeid, 2, true);
			if (pro.l2.directory.blocktable.get(add).state == Directory.SHARED_STATE) {
				// read miss, l2 shared
				cur_cycle = shared(coreid, homeid, add, cur_cycle);
				
			} else if (pro.l2.directory.blocktable.get(add).state == Directory.MODIFIED_STATE) {
				// read miss, l2 exclusive
				cur_cycle = exclusive(coreid, homeid, add, cur_cycle);
			}
		} else {
			// read miss, l2 uncached
			Util.addCount(homeid, 2, false);
			cur_cycle = uncached(coreid, homeid, add, cur_cycle);
		}
		return cur_cycle;
	}

	public int uncached(String localid, String homeid, String address, int cycle) {
		int local2home = Util.getManhattanDistance(localid, homeid, Simulator.p);
		int home2controller = Util.getManhattanDistance(homeid, "0", Simulator.p);
		Processor processor = Simulator.processorsTable.get(homeid);

		// 1. L sends request to H
		String str = localid + ": L1 read miss, sends request to H:" + homeid +". This is a short message.";
		Simulator.shortCount++;
		Util.addOutput(cycle, str);
		cycle = cycle + local2home * Simulator.C + Simulator.d;

		// 2. H sends request to 0
		str = homeid + ": gets request from L:" + localid +", L2 read miss, sends request to Memory Controller:0. This is a short message.";
		Simulator.shortCount++;
		Util.addOutput(cycle, str);
		cycle = cycle + home2controller * Simulator.C;

		// 3. 0 get data from mem
		// return to H
		str = 0 + ": gets request from H:" + homeid +", starts to fetch data from memory.";
		Util.addOutput(cycle, str);
		cycle = cycle + Simulator.d1;
		
		str = 0 + ": gets request from H:" + homeid +", gets block from memory, sends blocks to H:" + homeid + ". This is a long message.";
		Simulator.longCount++;
		Util.addOutput(cycle, str);
		cycle = cycle + home2controller * Simulator.C;

		// 4. H get data, return to L
		// set state of block to "shared" in dir
		// store to l2
		// add sharer L
		str = homeid + ": gets block from Memory Controller:0, sends blocks to L:" + localid + ". This is a long message.";
		Simulator.longCount++;
		Util.addOutput(cycle, str);
		cycle = cycle + Util.storeBlockToCache(address, "l2", homeid, cycle);
		processor.l2.directory.blocktable.get(address).state = Directory.SHARED_STATE;
		processor.l2.directory.blocktable.get(address).sharers.add(localid);
		cycle = cycle + local2home * Simulator.C;

		// L get data
		// store to l1
		// set state of block to "shared"
		str = localid + ": gets block from H:" + homeid + ", read block.";
		Util.addOutput(cycle, str);
		cycle = cycle + Util.storeBlockToCache(address, "l1", localid, cycle);
		Util.setBlockStatus(localid, address, Directory.SHARED_STATE);

		return cycle;
	}

	public int shared(String localid, String homeid, String address, int cycle) {
		Processor processor = Simulator.processorsTable.get(homeid);

		// 1. L sends request to H
		String str = localid + ": L1 read miss, sends request to H:" + homeid +". This is a short message.";
		Simulator.shortCount++;
		Util.addOutput(cycle, str);
		int manhattanDistance = Util.getManhattanDistance(localid, homeid, Simulator.p);
		cycle = manhattanDistance * Simulator.C + cycle + Simulator.d;

		// 2. H return block to L
		// add sharer L
		str = homeid + ": gets request from L:" + localid +", L2 read hit, sends block to L:" + localid + ". This is a long message.";
		Simulator.longCount++;
		Util.addOutput(cycle, str);
		Util.updateLRU(address, homeid, "l2", cycle);
		cycle = manhattanDistance * Simulator.C + cycle;
		processor.l2.directory.blocktable.get(address).sharers.add(localid);

		// L get data
		// store to l1
		// set state of block to "shared"
		str = localid + ": gets block from H:" + homeid + ", read block.";
		Util.addOutput(cycle, str);
		cycle = cycle + Util.storeBlockToCache(address, "l1", localid, cycle);
		Util.setBlockStatus(localid, address, Directory.SHARED_STATE);

		return cycle;
	}

	public int exclusive(String localid, String homeid, String address, int cycle) {
		Processor processor = Simulator.processorsTable.get(homeid);

		// 1. L sends request to H
		String str = localid + ": L1 read miss, sends request to H:" + homeid +". This is a short message.";
		Simulator.shortCount++;
		Util.addOutput(cycle, str);
		int manhattanDistance = Util.getManhattanDistance(localid, homeid, Simulator.p);
		cycle = manhattanDistance * Simulator.C + cycle + Simulator.d;

		// 2. H return owner to L
		str = homeid + ": gets request from L:" + localid +", L2 read hit(exclusive), sends owner to L:" + localid + ". This is a short message.";
		Simulator.shortCount++;
		Util.addOutput(cycle, str);
		cycle = manhattanDistance * Simulator.C + cycle;

		// 3. L get R, sends request to R
		String remoteid = processor.l2.directory.blocktable.get(address).owner;
		str = localid + ": gets owner from H:" + homeid +", sends request to R:" + remoteid + ". This is a short message.";
		Simulator.shortCount++;
		Util.addOutput(cycle, str);
		manhattanDistance = Util.getManhattanDistance(localid, remoteid, Simulator.p);
		cycle = manhattanDistance * Simulator.C + cycle;

		// 4. R sends block to L and H
		// set state of block to "shared"
		str = remoteid + ": gets request from L:" + localid +", sends block to L:" + localid + ". This is a long message.";
		Simulator.longCount++;
		Util.addOutput(cycle, str);
		if (!homeid.equals(remoteid)){
			str = remoteid + ": gets request from L:" + localid +", sends block to H:" + homeid + ". This is a long message.";
			Util.addOutput(cycle, str);
			Simulator.longCount++;
		}
		Util.setBlockStatus(remoteid, address, Directory.SHARED_STATE);
		int cycleByL = 0;
		int cycleByH = 0;
		cycleByL = Util.getManhattanDistance(localid, remoteid, Simulator.p) * Simulator.C;
		cycleByH = Util.getManhattanDistance(homeid, remoteid, Simulator.p) * Simulator.C;

		// L get block
		// store to L1
		// set state of Block to "shared"
		str = localid + ": gets block from R:" + remoteid + ", read block.";
		Util.addOutput(cycleByL, str);
		cycleByL = cycleByL + Util.storeBlockToCache(address, "l1", localid, cycleByL);
		Util.setBlockStatus(localid, address, Directory.SHARED_STATE);

		// H change to "shared"
		// store to L2
		// add sharer
		if (!homeid.equals(remoteid)){
			str = homeid + ": gets block from R:" + remoteid + ", store block to cache";
			Util.addOutput(cycleByH, str);
		} else {
			str = homeid + ": store block to cache";
			Util.addOutput(cycleByH, str);
		}
		processor.l2.directory.blocktable.get(address).state = Directory.SHARED_STATE;
		cycleByH = cycleByH + Util.storeBlockToCache(address, "l2", homeid, cycleByH);
		processor.l2.directory.blocktable.get(address).sharers.clear();
		processor.l2.directory.blocktable.get(address).sharers.add(remoteid);
		processor.l2.directory.blocktable.get(address).sharers.add(localid);

		cycle = cycle + Math.max(cycleByL, cycleByH);
		return cycle;
	}
}
