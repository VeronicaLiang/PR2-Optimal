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
			String setloc = add.substring(32 - Simulator.n1 + Simulator.a1 + 1, 31 - Simulator.b + 1);
			Set l1set = pro.l1.setsList.get(Integer.parseInt(setloc, 2));
			boolean flag = false;
			int oldest_cycle = -1;
			int oc_index = -1;
			for (int i = 0; i < l1set.blockList.size(); i++) {
				if (l1set.blockList.get(i).data == 0) {
					l1set.blockList.get(i).tag = add.substring(0, 31 - Simulator.n1 + Simulator.a1 + 1);
					l1set.blockList.get(i).data = 1;
					flag = true;
					break;
				}
				if ((oldest_cycle == -1) || (oldest_cycle > l1set.blockList.get(i).cur_cycle)) {
					oldest_cycle = l1set.blockList.get(i).cur_cycle;
					oc_index = i;
				}
			}
			if (!flag) {
				l1set.blockList.get(oc_index).tag = add.substring(0, 31 - Simulator.n1 + Simulator.a1 + 1);
				l1set.blockList.get(oc_index).data = 1;
				l1set.blockList.get(oc_index).cur_cycle = cur_cycle;
			}
		} else if (l.equals("l2")) {
			// TODO store block to cache
		}
		//TODO temporarily return 0
		return 0;
	}
	
	public static void setBlockStatus(int blockStatus) {
		// TODO set state of block in node to some state
	}

}