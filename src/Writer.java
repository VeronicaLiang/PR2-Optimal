
public class Writer {
	public Writer () {
		
	}
	
	public int run (String add, Processor pro, int currentCycle) {
		// run write operation, and return the number of finishing cycle
		//if the operation is miss or hit
				boolean isHitorMiss = Util.hitOrMiss(add, pro, Simulator.n1, Simulator.a1, Simulator.b, "l1");
				if(isHitorMiss){
					return 0;
				}else{
					//boolean isL2HitorMiss = Util.hitOrMiss(add, pro, n, a, b, "l2");
					
				}
		return 0;
	}
}
