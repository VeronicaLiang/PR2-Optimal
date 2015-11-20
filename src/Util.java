
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
}
