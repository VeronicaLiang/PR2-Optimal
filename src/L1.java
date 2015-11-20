import java.util.LinkedList;

/*
 * L1 entity, every processor has a l1 cache
 * And every l1 cache has a directory to manage its block coherence as a Home.
 */
public class L1 {
	
	/*
	 * L1's sets container
	 */
	LinkedList<Set> setsList = new LinkedList<Set>();
	L1(int numberOfSetInL1,int associativity){
		//Initialize the sets in the cache
		for(int i=0;i<numberOfSetInL1;i++){
			Set set = new Set(associativity);
			setsList.add(set);
		}
	}
}
