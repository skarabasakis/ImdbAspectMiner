// The MIT License
//
// Copyright (c) 2010 Stelios Karabasakis
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//
package topics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import util.AppLogger;
import classes.Counter;
import config.Paths;


/**
 * Collects sentences
 * 
 * @author Stelios Karabasakis
 */
public class TokenListsCollector implements Serializable {
	
	
	private static final long					serialVersionUID	= 1953712664414383225L;
	
	private static final String					FILENAME_PREFIX		= "docs";
	private static final int					FILENAME_ID_LENGTH	= 4;

	private Counter								topicModelFileId;
	private HashMap<Long, ArrayList<String>>	tokenLists;
	
	public TokenListsCollector()
	{
		tokenLists = new HashMap<Long, ArrayList<String>>();
		topicModelFileId = new Counter();
		topicModelFileId.set(1);
	}
	
	public int countTokens()
	{
		int counter = 0;
		Set<Entry<Long, ArrayList<String>>> tokenlists_entries = tokenLists.entrySet();
		for (Entry<Long, ArrayList<String>> tokenlist : tokenlists_entries) {
			counter += tokenlist.getValue().size();
		}
		return counter;
	}

	public ArrayList<String> getTokenListForDocument(Long docId)
	{
		ArrayList<String> list_to_return = null;
		
		if (!tokenLists.containsKey(docId)) {
			list_to_return = tokenLists.put(docId, new ArrayList<String>());
		}
		list_to_return = tokenLists.get(docId);
		
		return list_to_return;
	}
	
	public ArrayList<String> setTokenListForDocument(Long docId, ArrayList<String> tokenList)
	{
		ArrayList<String> docTokenList = getTokenListForDocument(docId);
		docTokenList.addAll(tokenList);
		return docTokenList;
	}
	
	public void writeNextFile(NumberFormat docIdFormat)
	{
		try {
			BufferedWriter f = new BufferedWriter(new FileWriter(new File(getTokenListFilePath()), false));
			Iterator<String> c;

			Iterator<Entry<Long, ArrayList<String>>> entries = tokenLists.entrySet().iterator();
			while ( entries.hasNext() ) {
				StringBuilder entryline = new StringBuilder();
				Entry<Long, ArrayList<String>> entry = entries.next();

				entryline.append(docIdFormat.format(entry.getKey()) + " " + entry.getValue().size());
				c = entry.getValue().iterator();
				while ( c.hasNext() ) {
					entryline.append(" " + c.next().replace(' ', '-'));
				}
				
				f.append(entryline);
				f.newLine();
			}
			
			f.close();
			
		} catch ( FileNotFoundException e ) {
			AppLogger.error.log(Level.WARNING, "Cannot open token list file (" + getTokenListFilePath()
				+ ") for writing");
		} catch ( IOException e ) {
			AppLogger.error.log(Level.WARNING, "Attempt to write token list entry to file " + getTokenListFilePath()
				+ " failed.");
		}
		
		// Resetting tokenlist hashmap
		tokenLists.clear();
		topicModelFileId.increment();
	}
	
	public static NumberFormat defaultDocIdFormat()
	{
		NumberFormat docIdFormat = NumberFormat.getNumberInstance();
		docIdFormat.setMinimumIntegerDigits(10);
		docIdFormat.setGroupingUsed(false);
		
		return docIdFormat;
	}
	
	private String getTokenListFilePath()
	{
		return Paths.tokenListPath + "/" + FILENAME_PREFIX + topicModelFileId.getValueString(FILENAME_ID_LENGTH)
			+ ".txt";
	}
}
