
public class Writer {
	public Writer () {
		
	}
	
	public int run (String add, String coreid, int currentCycle) {
		// run write operation, and return the number of finishing cycle
		//if the operation is miss or hit
		Processor pro = Simulator.processorsTable.get(coreid);
		String homeid = Integer.parseInt(add.substring(19-Simulator.p+1, 20),2) +"";
		Processor homeProcessor = Simulator.processorsTable.get(homeid);
		boolean isHitorMiss = Util.hitOrMiss(add, pro, Simulator.n1, Simulator.a1, Simulator.b, "l1");
		int manhattanDistance = Util.getManhattanDistance(coreid, homeid, Simulator.p);
		int cycles = 0;
		if(isHitorMiss){
			return 0;
		}else{
			cycles = Simulator.d;
			if (homeProcessor.l2.directory.blocktable.contains(add)) {
				if(homeProcessor.l2.directory.blocktable.get(add).state == 0){
						
				}else if(homeProcessor.l2.directory.blocktable.get(add).state == 1){
					
				}else if(homeProcessor.l2.directory.blocktable.get(add).state == 2){
					//if the block's state is shared
					//L to H
					cycles = cycles+manhattanDistance * Simulator.C;
					//H to L, change the block's state to exclusive and the owner to L
					cycles = cycles+manhattanDistance * Simulator.C;
					homeProcessor.l2.directory.blocktable.get(add).owner=coreid;
					homeProcessor.l2.directory.blocktable.get(add).state = 1;
					//the longest latency in all the sharer
					int longestLatency = 0;
					for(int i =0;i<homeProcessor.l2.directory.blocktable.get(add).sharers.size();i++){
						int latency = Util.getManhattanDistance(coreid, homeProcessor.l2.directory.blocktable.get(add).sharers.get(i), Simulator.p);
						if(latency>longestLatency){
							longestLatency = latency;
						}
					}
					//L to R
					cycles = cycles+longestLatency * Simulator.C;
					//change the block's state to exclusive in local l1
					pro.l1.setsList
				}
			} else {
						//l2 does not have the block
						
			}
		}
		return 0;
	}
}
