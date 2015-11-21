import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

public class Util {
	public static int add(int a, int b) {
		return a + b;
	}

	public static void printOutputList(HashMap<Integer, ArrayList<String>> outputList, int last) {
		ArrayList<String> strList = new ArrayList<String>();
		for (int i = 0; i < last + 5; i++) {
			strList = outputList.get(i);
			for (int j = 0; j < strList.size(); j++) {
				System.out.println(strList.get(j));
			}
		}
	}

	// TODO check whether this calculation is correct
	public static int getManhattanDistance(String coreid, String homecoreid, int p) {
		int edge = (int) Math.pow(2, p / 2);
		int core_x = Integer.parseInt(coreid) / edge;
		int core_y = Integer.parseInt(coreid) % edge;
		int home_x = Integer.parseInt(homecoreid) / edge;
		int home_y = Integer.parseInt(homecoreid) % edge;

		int dist = Math.abs(core_x - core_y) + Math.abs(home_x - home_y);

		return dist;
	}

	public static Boolean hitOrMiss(String add, Processor pro, int n, int a, int b) {
		// 0.......31-n+a|31-n+a+1.......31-b|31-b+1..........31
		// |-------------|-------------------|-----------------|
		// |TAG |SET INDEX |OFFSET |
		// |-------------|-------------------|-----------------|
		// 31.........n-a|n-a-1.............b|b-1..............0

		String setloc = add.substring(32 - n + a + 1, 31 - b + 1);
		String blocktag = add.substring(0, 31 - n + a + 1);

		Set l1set = pro.l1.setsList.get(Integer.parseInt(setloc, 2));
		// a should be equal to l1set.blockList.size()
		for (int i = 0; i < l1set.blockList.size(); i++) {
			if (blocktag.equals(l1set.blockList.get(i).tag)) {
				// check whether the block state is invalid.
				if (l1set.blockList.get(i).state != Directory.INVALID_STATE) {
					return true;
				} else {
					return false;
				}
			}
		}

		return false;
	}

	public static String hexToBinary(String hex) {
		String value = new BigInteger(hex, 16).toString(2);
		String zero_pad = "0";
		for (int i = 1; i < 32 - value.length(); i++)
			zero_pad = zero_pad + "0";
		return zero_pad + value;
	}

	public static int storeBlockToCache(String add, String l, String coreid, int cur_cycle) {
		if (l.equals("l1")) {
			Processor pro = Simulator.processorsTable.get(coreid);
			String homeid = Integer.parseInt(add.substring(19-Simulator.p+1, 20),2) +"";
			String setloc = add.substring(32 - Simulator.n1 + Simulator.a1 + 1, 31 - Simulator.b + 1);
			Set l1set = pro.l1.setsList.get(Integer.parseInt(setloc, 2));
			boolean flag = false;
			int oldest_cycle = -1;
			int oc_index = -1;
			for (int i = 0; i < l1set.blockList.size(); i++) {
				if (l1set.blockList.get(i).data == 0) {
					l1set.blockList.get(i).tag = add.substring(0, 31 - Simulator.n1 + Simulator.a1 + 1);
					l1set.blockList.get(i).data = 1;
					l1set.blockList.get(i).state = Directory.SHARED_STATE;
					l1set.blockList.get(i).cur_cycle = cur_cycle;
					flag = true;
					break;
				}
				if ((oldest_cycle == -1) || (oldest_cycle > l1set.blockList.get(i).cur_cycle)) {
					oldest_cycle = l1set.blockList.get(i).cur_cycle;
					oc_index = i;
				}
			}
			if (!flag) {
				// replace l1 block
				l1set.blockList.get(oc_index).tag = add.substring(0, 31 - Simulator.n1 + Simulator.a1 + 1);
				l1set.blockList.get(oc_index).data = 1;
				l1set.blockList.get(oc_index).cur_cycle = cur_cycle;
				Processor homepro = Simulator.processorsTable.get(homeid);
				if (pro.l2.directory.blocktable.get(add).state == Directory.SHARED_STATE){
					pro.l2.directory.blocktable.get(add).sharers.remove(coreid);
				}else{
					String l2setloc =  add.substring(31-Simulator.n2+Simulator.a2+1,31-Simulator.b+1);
					Set l2set = homepro.l2.setsList.get(Integer.parseInt(l2setloc,2));
					String l2blocktag = add.substring(0,31-Simulator.n2+Simulator.a2+1);
					for (int j=0; j<l2set.blockList.size(); j++) {
						Block check = l2set.blockList.get(j);
						if (check.tag.equals(l2blocktag)) {
							check.cur_cycle = cur_cycle + Util.getManhattanDistance(coreid, homeid, Simulator.p);
						}
					}
				}
				pro.l2.directory.blocktable.get(add).state = Directory.SHARED_STATE;
				pro.l2.directory.blocktable.get(add).sharers.remove(coreid);
			}
			return 0;
		} else if (l.equals("l2")) {
			// TODO store block to cache
		}
		//TODO temporarily return 0
		return 0;
	}
	
	public static void setBlockStatus(String coreid, String add,int blockStatus) {
		// set state of block in node to some state
		Processor pro = Simulator.processorsTable.get(coreid);
		String setloc = add.substring(32 - Simulator.n1 + Simulator.a1 + 1, 31 - Simulator.b + 1);
		//Set l1set = ;
		String tag = add.substring(0, 31 - Simulator.n1 + Simulator.a1 + 1);
		for (int i = 0; i < pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.size(); i++){
			if(pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.get(i).tag.equals(tag)){
				pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.get(i).state = blockStatus;
			}
		}
	}
	

}