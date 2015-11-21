
public class Writer {
	public Writer () {
		
	}
	
	public int run (String add, String coreid, int currentCycle) {
		// run write operation, and return the number of finishing cycle
		//if the operation is miss or hit
		Processor pro = Simulator.processorsTable.get(coreid);
		String homeid = Integer.parseInt(add.substring(19-Simulator.p+1, 20),2) +"";
		Processor homeProcessor = Simulator.processorsTable.get(homeid);
		boolean isHitorMiss = Util.hitOrMiss(add, pro, Simulator.n1, Simulator.a1, Simulator.b);
		int manhattanDistance = Util.getManhattanDistance(coreid, homeid, Simulator.p);
		int cycles = 0;
		cycles = Simulator.d;
		if(isHitorMiss){
			//L to H
			cycles = cycles+manhattanDistance * Simulator.C;
			
			//change the block's state to exclusive and the owner to L
			homeProcessor.l2.directory.blocktable.get(add).owner=coreid;
			homeProcessor.l2.directory.blocktable.get(add).state = 1;
			//H to L, 
			cycles = cycles+manhattanDistance * Simulator.C;
			
			//the longest latency in all the sharer
			int longestLatency = 0;
			for(int i =0;i<homeProcessor.l2.directory.blocktable.get(add).sharers.size();i++){
				int latency = Util.getManhattanDistance(coreid, homeProcessor.l2.directory.blocktable.get(add).sharers.get(i), Simulator.p);
				if(latency>longestLatency){
					longestLatency = latency;
				}
			}
			//L to R
			Util.setBlockStatus(1);//TODO set the block's state to exclusive in local l1
			cycles = cycles+longestLatency * Simulator.C;
			
			longestLatency = 0;
			for(int i =0;i<homeProcessor.l2.directory.blocktable.get(add).sharers.size();i++){
				int latency = Util.getManhattanDistance(coreid, homeProcessor.l2.directory.blocktable.get(add).sharers.get(i), Simulator.p);
				if(latency>longestLatency){
					longestLatency = latency;
				}
				String rn = homeProcessor.l2.directory.blocktable.get(add).sharers.get(i);
				Processor romteProcessor = Simulator.processorsTable.get(rn);
				//romteProcessor.l1 set remote blocks' state to invalid
				Util.setBlockStatus(1);//TODO
			}
			// Rs send ack to L
			cycles = cycles+longestLatency * Simulator.C;
			//write operation
			
		}else{
			
			if (homeProcessor.l2.directory.blocktable.contains(add)) {
				if(homeProcessor.l2.directory.blocktable.get(add).state == 0){
					//if the block's state is uncached
					//L to H
					cycles = cycles+manhattanDistance * Simulator.C;
					//H to P0
					int hto0manhattanDistance = Util.getManhattanDistance(homeid, "0", Simulator.p);
					cycles = cycles+hto0manhattanDistance * Simulator.C;
					//p0 fetch data from memory
					cycles = cycles+Simulator.d1;
					//p0 to H
					cycles = cycles+hto0manhattanDistance * Simulator.C;
					
					//store the block 
					Util.storeBlockToCache(add,"l2",homeid,cycles);
					//change the block's state to exclusive
					Util.setBlockStatus(1);//TODO 
					//set the block's owner
					homeProcessor.l2.directory.blocktable.get(add).owner = coreid;
					//H to L
					cycles = cycles+manhattanDistance * Simulator.C;
					//store the block in local l1
					Util.storeBlockToCache(add,"l1",coreid,cycles);
					//set the block's state to exclusive
					Util.setBlockStatus(1);//TODO 
				}else if(homeProcessor.l2.directory.blocktable.get(add).state == 1){
					//L to H
					cycles = cycles+manhattanDistance * Simulator.C;
					
					//H to L, 
					cycles = cycles+manhattanDistance * Simulator.C;
					
					//L to R
					int latency = Util.getManhattanDistance(coreid,homeProcessor.l2.directory.blocktable.get(add).owner, Simulator.p);
					cycles = cycles+latency * Simulator.C;
					
					//set the block's state to invalid in the remote l1
					Util.setBlockStatus(1);//TODO
					//send block to L, and send alert to H
					int rToHLatency = Util.getManhattanDistance(homeid,homeProcessor.l2.directory.blocktable.get(add).owner, Simulator.p);
					
					if(latency>rToHLatency){
						cycles = cycles+latency * Simulator.C;
					}else{
						cycles = cycles+rToHLatency * Simulator.C;
					}
					//store the block in local l1 and set the block's state to exclusive
					Util.storeBlockToCache(add,"l1",coreid,cycles);
					Util.setBlockStatus(1);//TODO
					
					//change the owner of the block in H to L
					homeProcessor.l2.directory.blocktable.get(add).owner = coreid;
				}else if(homeProcessor.l2.directory.blocktable.get(add).state == 2){
					//if the block's state is shared
					//L to H
					cycles = cycles+manhattanDistance * Simulator.C;
					
					//change the block's state to exclusive and the owner to L
					homeProcessor.l2.directory.blocktable.get(add).owner=coreid;
					homeProcessor.l2.directory.blocktable.get(add).state = 1;
					//H to L, 
					cycles = cycles+manhattanDistance * Simulator.C;
					
					//the longest latency in all the sharer
					int longestLatency = 0;
					for(int i =0;i<homeProcessor.l2.directory.blocktable.get(add).sharers.size();i++){
						int latency = Util.getManhattanDistance(coreid, homeProcessor.l2.directory.blocktable.get(add).sharers.get(i), Simulator.p);
						if(latency>longestLatency){
							longestLatency = latency;
						}
					}
					
					Util.setBlockStatus(1);//TODO set the block's state to exclusive in local l1
					//store the block in local l1
					Util.storeBlockToCache(add,"l1",coreid,cycles);
					//L to R
					cycles = cycles+longestLatency * Simulator.C;
					
					longestLatency = 0;
					for(int i =0;i<homeProcessor.l2.directory.blocktable.get(add).sharers.size();i++){
						int latency = Util.getManhattanDistance(coreid, homeProcessor.l2.directory.blocktable.get(add).sharers.get(i), Simulator.p);
						if(latency>longestLatency){
							longestLatency = latency;
						}
						String rn = homeProcessor.l2.directory.blocktable.get(add).sharers.get(i);
						Processor romteProcessor = Simulator.processorsTable.get(rn);
						//romteProcessor.l1 set remote blocks' state to invalid
						Util.setBlockStatus(1);//TODO
					}
					// Rs send ack to L
					cycles = cycles+longestLatency * Simulator.C;
					//write operation
				}
			} else {
						//l2 does not have the block
						
			}
		}
		return cycles;
	}
}
