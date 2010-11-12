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

import indexing.Token;
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
import java.util.Map.Entry;
import java.util.logging.Level;
import util.AppLogger;
import config.Paths;


/**
 * Collects sentences
 * 
 * @author Stelios Karabasakis
 */
@SuppressWarnings("serial")
public class ReviewSentencesCollector implements Serializable {
	
	private HashMap<Long, ArrayList<Token>>	tokenLists;
	
	public ReviewSentencesCollector()
	{
	}
	
	public ArrayList<Token> getTokenListForSentence(Long sentenceId)
	{
		ArrayList<Token> list_to_return = null;
		
		if (!tokenLists.containsKey(sentenceId)) {
			list_to_return = tokenLists.put(sentenceId, new ArrayList<Token>());
		}
		else {
			list_to_return = tokenLists.get(sentenceId);
		}
		
		return list_to_return;
	}
	
	public ArrayList<Token> setTokenListForSentence(Long sentenceId, ArrayList<Token> tokenList)
	{
		return tokenLists.put(sentenceId, tokenList);
	}
	
	public void writeToFile(NumberFormat sentenceIdFormat)
	{
		try {
			BufferedWriter f = new BufferedWriter(new FileWriter(new File(Paths.tokenListFile), true));
			Iterator<Token> c;

			Iterator<Entry<Long, ArrayList<Token>>> entries = tokenLists.entrySet().iterator();
			while ( entries.hasNext() ) {
				StringBuilder entryline = new StringBuilder();
				Entry<Long, ArrayList<Token>> entry = entries.next();

				entryline.append(sentenceIdFormat.format(entry.getKey()));
				c = entry.getValue().iterator();
				while ( c.hasNext() ) {
					entryline.append(" " + c.next());
				}
				
				f.append(entryline);
				f.newLine();
			}
			
			f.close();
		} catch ( FileNotFoundException e ) {
			AppLogger.error.log(Level.WARNING, "Cannot open token list file (" + Paths.tokenListFile + ") for writing");
		} catch ( IOException e ) {
			AppLogger.error.log(Level.WARNING, "Attempt to write token list entry to file " + Paths.tokenListFile + " failed.");
		}
	}
	
	private static NumberFormat defaultSentenceIdFormat()
	{
		NumberFormat docIdFormat = NumberFormat.getNumberInstance();
		docIdFormat.setMinimumIntegerDigits(11);
		
		return docIdFormat;
	}
}
