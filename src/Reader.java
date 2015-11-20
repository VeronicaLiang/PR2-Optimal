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
			int cycle_needed = readMiss(address, coreid);
			return cycle+cycle_needed;
		}
	}

	public int readMiss(String add, String coreid){
		int cycle_used = 0;
		String homeid = Integer.parseInt(add.substring(19-Simulator.p+1, 20),2) +"";
		int local2home = Util.getManhattanDistance(coreid,homeid, Simulator.p);
		Processor pro = Simulator.processorsTable.get(homeid);
		if(pro.l2.directory.blocktable.contains(add)){
			return cycle_used;
		}else{
			// L2 miss, need to fetch from memory
			// add:
			// 1. messages to and from controller
			// 2. memory access delay d1
			int home2controller = Util.getManhattanDistance(homeid, "0", Simulator.p);
			System.out.println(coreid + ": L2 uncached, need to send message to controller" +
					" and fetch data from memory");
			int store_time = Util.storeBlockToCache(add, "l2",);
			cycle_used += 2*local2home + 2*home2controller + Simulator.d1;
			return cycle_used;
		}
	}
	
	public int exclusive (String localid, String homeid, String address) {
		return 0;
	}
}
