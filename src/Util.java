import java.math.BigInteger;

public class Util {
	public static int add (int a, int b) {
		return a+b;
	}
	public static Boolean hitOrMiss(String add, Processor pro, int n, int a, int b, String l) {
		// 0.......31-n+a|31-n+a+1.......31-b|31-b+1..........31
		// |-------------|-------------------|-----------------|
		// |TAG          |SET INDEX          |OFFSET           |
		// |-------------|-------------------|-----------------|
		// 31.........n-a|n-a-1.............b|b-1..............0

		String setloc = add.substring(32-n+a+1, 31-b+1);
		String blocktag = add.substring(0,31-n+a+1);

		if (l.equals("l1")) {
			Set l1set = pro.l1.setsList.get(Integer.parseInt(setloc,2));
			// a should be equal to l1set.blockList.size()
			for (int i=0; i<l1set.blockList.size(); i++){
				if(blocktag.equals(l1set.blockList.get(i).tag)){
					// check whether the block state is invalid.
					if(l1set.blockList.get(i).state != Directory.INVALID_STATE){
						return true;
					}else{
						return false;
					}
				}
			}
		} else if (l.equals("l2")) {
			Set l2set = pro.l2.setsList.get(Integer.parseInt(setloc, 2));
			for (int i=0; i<l2set.blockList.size(); i++){
				if(blocktag.equals(l2set.blockList.get(i).tag)){
					// check whether the block state is invalid.
					// TODO whether only when the state is "share" should return true
					if(pro.l2.directory.blocktable.contains(add) && pro.l2.directory.blocktable.get(add).state != 0){
						return true;
					}else{
						return false;
					}
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

	public static void storeBlockToCache(String add, String l, String coreid, int n, int a, int b, int cur_cycle, Processor pro) {

		if(l.equals("l1")){
			String setloc = add.substring(32-n+a+1, 31-b+1);
			Set l1set = pro.l1.setsList.get(Integer.parseInt(setloc,2));
			boolean flag = false;
			int oldest_cycle = -1;
			int oc_index = -1;
			for(int i=0; i<l1set.blockList.size(); i++){
				if (l1set.blockList.get(i).data == 0){
					l1set.blockList.get(i).tag = add.substring(0,31-n+a+1);
					l1set.blockList.get(i).data = 1;
					flag = true;
					break;
				}
				if((oldest_cycle == -1)||(oldest_cycle > l1set.blockList.get(i).cur_cycle)){
					oldest_cycle = l1set.blockList.get(i).cur_cycle;
					oc_index = i;
				}
			}
			if(!flag){
				l1set.blockList.get(oc_index).tag = add.substring(0,31-n+a+1);
				l1set.blockList.get(oc_index).data = 1;
				l1set.blockList.get(oc_index).cur_cycle=cur_cycle;
			}
		}else if (l.equals("l2")) {
			// TODO store block to cache
		}
	}


}