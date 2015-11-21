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
		// |TAG          |SET INDEX          |OFFSET           |
		// |-------------|-------------------|-----------------|
		// 31.........n-a|n-a-1.............b|b-1..............0

		String setloc = add.substring(31 - n + a + 1, 31 - b + 1);
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
		String homeid = Integer.parseInt(add.substring(19-Simulator.p+1, 20),2) +"";
		int runcycle = 0;
		if (l.equals("l1")) {
			Processor pro = Simulator.processorsTable.get(coreid);
			String setloc = add.substring(32 - Simulator.n1 + Simulator.a1 + 1, 31 - Simulator.b + 1);
			Set l1set = pro.l1.setsList.get(Integer.parseInt(setloc, 2));
			int oldest_cycle = -1;
			int oc_index = -1;
			for (int i = 0; i < l1set.blockList.size(); i++) {
				if (l1set.blockList.get(i).data == 0) {
					l1set.blockList.get(i).tag = add.substring(0, 31 - Simulator.n1 + Simulator.a1 + 1);
					l1set.blockList.get(i).data = 1;
//					l1set.blockList.get(i).state = Directory.SHARED_STATE;
					l1set.blockList.get(i).cur_cycle = cur_cycle;
					l1set.blockList.get(i).address = add;
					return 0;
				}
				if ((oldest_cycle == -1) || (oldest_cycle > l1set.blockList.get(i).cur_cycle)) {
					oldest_cycle = l1set.blockList.get(i).cur_cycle;
					oc_index = i;
				}
			}

			// replace l1 block
			l1set.blockList.get(oc_index).tag = add.substring(0, 31 - Simulator.n1 + Simulator.a1 + 1);
			l1set.blockList.get(oc_index).data = 1;
			l1set.blockList.get(oc_index).cur_cycle = cur_cycle;
			Processor homepro = Simulator.processorsTable.get(homeid);
			// write back to l2
			String replacedBlockadd = getBlockAddress(l1set.blockList.get(oc_index));
			if (homepro.l2.directory.blocktable.get(replacedBlockadd).state == Directory.SHARED_STATE){
				homepro.l2.directory.blocktable.get(replacedBlockadd).sharers.remove(coreid);
			}else{
				String l2setloc =  replacedBlockadd.substring(31-Simulator.n2+Simulator.a2+1,31-Simulator.b+1);
				Set l2set = homepro.l2.setsList.get(Integer.parseInt(l2setloc,2));
				String l2blocktag = replacedBlockadd.substring(0,31-Simulator.n2+Simulator.a2+1);
				for (int j=0; j<l2set.blockList.size(); j++) {
					Block check = l2set.blockList.get(j);
					if (check.tag.equals(l2blocktag)) {
						check.cur_cycle = cur_cycle + Util.getManhattanDistance(coreid, homeid, Simulator.p);
					}
				}
			}
			homepro.l2.directory.blocktable.get(replacedBlockadd).state = Directory.SHARED_STATE;
			homepro.l2.directory.blocktable.get(replacedBlockadd).sharers.remove(coreid);
			return 0;

		} else if (l.equals("l2")) {
			Processor homepro = Simulator.processorsTable.get(homeid);
			String setlocl2 = add.substring(32 - Simulator.n2 + Simulator.a2 + 1, 31 - Simulator.b + 1);
			Set l2set = homepro.l1.setsList.get(Integer.parseInt(setlocl2, 2));
			int oldest_cycle = -1;
			int oc_index = -1;
			for (int i = 0; i < l2set.blockList.size(); i++) {
				if (l2set.blockList.get(i).data == 0) {
					l2set.blockList.get(i).tag = add.substring(0, 31 - Simulator.n2 + Simulator.a2 + 1);
					l2set.blockList.get(i).data = 1;
					l2set.blockList.get(i).state = Directory.SHARED_STATE;
					l2set.blockList.get(i).cur_cycle = cur_cycle;
					return 0;
				}
				if ((oldest_cycle == -1) || (oldest_cycle > l2set.blockList.get(i).cur_cycle)) {
					oldest_cycle = l2set.blockList.get(i).cur_cycle;
					oc_index = i;
				}
			}
			String replacedBlockadd = getBlockAddress(l2set.blockList.get(oc_index));
			if (homepro.l2.directory.blocktable.get(replacedBlockadd).state == Directory.SHARED_STATE){
				// H sends replace to sharers (R)
				int maxDist = 0;
				ArrayList<String> sharers = homepro.l2.directory.blocktable.get(replacedBlockadd).sharers;
				for (int k=0; k<sharers.size();k++){
					int dist = Util.getManhattanDistance(homeid,sharers.get(k),Simulator.p);
					if(maxDist < dist){
						maxDist = dist;
					}
					// R delete block in L1
					deleteL1Block(sharers.get(k), replacedBlockadd);
				}
				// H update the block
				l2set.blockList.get(oc_index).tag = add.substring(0, 31 - Simulator.n2 + Simulator.a2 + 1);
				l2set.blockList.get(oc_index).data = 1;
				l2set.blockList.get(oc_index).state = Directory.SHARED_STATE;
				l2set.blockList.get(oc_index).cur_cycle = cur_cycle;
				runcycle = maxDist;
			}else if(homepro.l2.directory.blocktable.get(replacedBlockadd).state == Directory.MODIFIED_STATE){
				// H sends replace to Owner
				String owner = homepro.l2.directory.blocktable.get(replacedBlockadd).owner;
				int home2owner = Util.getManhattanDistance(homeid,owner,Simulator.p);
				// Owner sends block to H; Owner delete block in L1
				int local2owner = Util.getManhattanDistance(homeid,owner,Simulator.p);
				deleteL1Block(owner,replacedBlockadd);
				// H update the block in L2
				l2set.blockList.get(oc_index).tag = add.substring(0, 31 - Simulator.n2 + Simulator.a2 + 1);
				l2set.blockList.get(oc_index).data = 1;
				l2set.blockList.get(oc_index).state = Directory.SHARED_STATE;
				l2set.blockList.get(oc_index).cur_cycle = cur_cycle;
				runcycle = home2owner + local2owner;
			}
		}
		return runcycle;
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

	public static void deleteL1Block(String coreid, String add){
		Processor pro = Simulator.processorsTable.get(coreid);
		String setloc = add.substring(32 - Simulator.n1 + Simulator.a1 + 1, 31 - Simulator.b + 1);
		//Set l1set = ;
		String tag = add.substring(0, 31 - Simulator.n1 + Simulator.a1 + 1);
		for (int i = 0; i < pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.size(); i++){
			if(pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.get(i).tag.equals(tag)){
				pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.get(i).tag ="";
				pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.get(i).data = 0;
			}
		}
	}
	
	public static int getBlockStatus(String coreid, String address) {
		return 0;
	}
	
	public static void updateLRU (String address, String coreid, String l, int cycle) {
		Processor pro = Simulator.processorsTable.get(coreid);
		if (l.equals("l1")) {
			String setloc = address.substring(32 - Simulator.n1 + Simulator.a1 + 1, 31 - Simulator.b + 1);
			String tag = address.substring(0, 31 - Simulator.n1 + Simulator.a1 + 1);
			for (int i = 0; i < pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.size(); i++){
				if(pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.get(i).tag.equals(tag)){
					pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.get(i).cur_cycle=cycle;
				}
			}
		} else if (l.equals("l2")) {
			String setlocl2 = address.substring(32 - Simulator.n2 + Simulator.a2 + 1, 31 - Simulator.b + 1);
			String tag = address.substring(0, 31 - Simulator.n2 + Simulator.a2 + 1);
			for (int i = 0; i < pro.l2.setsList.get(Integer.parseInt(setlocl2, 2)).blockList.size(); i++){
				if(pro.l1.setsList.get(Integer.parseInt(setlocl2, 2)).blockList.get(i).tag.equals(tag)){
					pro.l1.setsList.get(Integer.parseInt(setlocl2, 2)).blockList.get(i).cur_cycle=cycle;
				}
			}
		}
		
	}

	public static String getBlockAddress(Block curbloc){
		return curbloc.address;
	}

}