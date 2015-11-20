import javax.naming.SizeLimitExceededException;
import java.util.ArrayList;
import java.util.Hashtable;

public class Reader {
	public Reader () {
		
	}
	
	public int run (String coreid, String address, int cycle) {
		Processor pro = (Processor) Simulator.processorsTable.get(coreid);
		boolean l1readHit = Util.hitOrMiss(address, pro, Simulator.n1, Simulator.a1, Simulator.b, "l1");
		if (l1readHit) {
			System.out.println(coreid + ": L1 read hit");
			return cycle+1;
		} else {
			int cycle_needed = readMiss(pro, address, coreid);
			return cycle+cycle_needed;
		}
	}

	public int readMiss(Processor pro, String add, String coreid){
		int cycle_used = 0;
		String homeid = Integer.parseInt(add.substring(19-Simulator.p+1, 20),2) +"";

		if(pro.l2.directory.blocktable.contains(add)){

		}else{
			// L2 miss, need to fetch from memory
			// add:
			// 1. messages to and from controller
			// 2. memory access delay d1
		}
		int manhattanDistance = Util.getManhattanDistance(coreid, "0", Simulator.p);
		cycle_used += 2*manhattanDistance + Simulator.d1;
		System.out.println(coreid + ": L2 miss, need to send message to controller" +
				" and fetch data from memory");
		return cycle_used;
	}
	
	public int exclusive () {
		return 0;
	}
}
