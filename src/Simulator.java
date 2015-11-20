import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import javax.xml.bind.DatatypeConverter;

/*
 * The simulator is trace driven. That is memory load and store operations will specified in an
 * input trace file whose name is specified as the second command line input.
 */
public class Simulator {



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Util.add(1, 2);
		
		String inputFile = args[0];
		int p = Integer.parseInt(args[1]);// The power of processors with a root
											// of 2
		int n1 = Integer.parseInt(args[2]);// The power of the size of every l1
											// with a root of 2
		int n2 = Integer.parseInt(args[3]);// The power of the size of every l2
											// with a root of 2
		int b = Integer.parseInt(args[4]);// The size of a block
		int a1 = Integer.parseInt(args[5]);// The power of the associativity of
											// l1 with a root of 2
		int a2 = Integer.parseInt(args[6]);// The power of the associativity of
											// l2 with a root of 2
		int C = Integer.parseInt(args[7]);// The number of delay cycles caused
											// by communicating between two
											// nodes(a node consists of a
											// processor and l1 cache)
		int d = Integer.parseInt(args[8]);// The number of cycles caused by a l2
											// hit(The l1 hit is satisfied in
											// the same cycle in which it is
											// issued)
		int d1 = Integer.parseInt(args[9]);// The number of cycles caused by a
											// memory access
	}

}
