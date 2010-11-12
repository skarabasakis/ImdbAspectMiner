/**
 * 
 */
package demo;

import indexing.PosTag;


//import org.apache.lucene.*;
//import com.mysql.jdbc.*;

/**
 * @author Skarab
 *
 */
public class LuceneTest {
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		PosTag.PosCategory posCat = PosTag.PosCategory.V;
		boolean negation = true;
		
		System.out.println(Math.abs((byte)((negation ? -1 : 1) * posCat.ordinal())));

	}
	
}
