import sun.print.SunMinMaxPage;

import javax.naming.SizeLimitExceededException;
import java.util.ArrayList;
import java.util.Hashtable;

public class Reader {
	public Reader () {
		
	}
	
	public int run (String coreid, String address, int cycle) {
		Processor pro = (Processor) Simulator.processorsTable.get(coreid);
		boolean l1readHit = Util.hitOrMiss(address, pro, Simulator.n1, Simulator.a1, Simulator.b);
		if (l1readHit) {
			System.out.println(coreid + ": L1 read hit");
			return cycle+1;
		} else {
			int cycle_needed = readMiss(address, coreid, cycle);
			return cycle+cycle_needed;
		}
	}

	public int readMiss(String add, String coreid, int cur_cycle){
		int cycle_used = 0;
		String homeid = Integer.parseInt(add.substring(19-Simulator.p+1, 20),2) +"";

		Processor pro = Simulator.processorsTable.get(homeid);
		if(pro.l2.directory.blocktable.contains(add)){

		}else{
			int local2home = Util.getManhattanDistance(coreid,homeid, Simulator.p);
			int home2controller = Util.getManhattanDistance(homeid, "0", Simulator.p);
			System.out.println(coreid + ": L2 uncached, need to send message to controller" +
					" and fetch data from memory");
			// The time from memory to controller is d1
			cur_cycle += local2home + 2*home2controller + Simulator.d1;
			int store_time_l2 = Util.storeBlockToCache(add, "l2", homeid, cur_cycle);
			cur_cycle += store_time_l2 + local2home;
			int store_time_l1 = Util.storeBlockToCache(add, "l1", homeid, cur_cycle);
			cur_cycle += store_time_l1;
		}
		return cur_cycle;
	}
	
	public int shared (String localid, String homeid, String address, int cycle) {
		Processor processor = Simulator.processorsTable.get(homeid);
		
		// 1. L sends request to H
		int manhattanDistance = Util.getManhattanDistance(localid, homeid, Simulator.p);
		cycle = manhattanDistance * Simulator.C + cycle;
		
		// 2. H return block to L
		// add sharer L
		cycle = manhattanDistance * Simulator.C + Simulator.d + cycle;
		processor.l2.directory.blocktable.get(address).sharers.add(localid);
		
		
		// L get data
		// store to l1
		// set state of block to "shared"
		cycle = cycle + Util.storeBlockToCache(address, "l1", localid, cycle);
		Util.setBlockStatus(Directory.SHARED_STATE);
		
		return cycle;
	}
	
	public int exclusive (String localid, String homeid, String address, int cycle) {
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
		Util.setBlockStatus(Directory.SHARED_STATE);
		cycle = cycle + Math.max(Util.getManhattanDistance(localid, remoteid, Simulator.p), Util.getManhattanDistance(localid, remoteid, Simulator.p))*Simulator.C;
		
		// L get block
		// store to L1
		// set state of Block to "shared"
		cycle = cycle + Util.storeBlockToCache(address, "l1", localid, cycle);
		Util.setBlockStatus(Directory.SHARED_STATE);
		
		// H change to "shared"
		// store to L2
		// add sharer
		processor.l2.directory.blocktable.get(address).state = Directory.SHARED_STATE;
		cycle = cycle + Util.storeBlockToCache(address, "l2", homeid, cycle);
		processor.l2.directory.blocktable.get(address).sharers.clear();
		processor.l2.directory.blocktable.get(address).sharers.add(remoteid);
		processor.l2.directory.blocktable.get(address).sharers.add(localid);
		
		return cycle;
	}
}
