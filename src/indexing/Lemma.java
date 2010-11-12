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
package indexing;

import net.didion.jwnl.data.POS;
import org.apache.commons.lang.ArrayUtils;
import wordnet.Synset;



/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class Lemma {
	
	public static final long	NO_OFFSET	= 0;

	private StringBuilder		lemma;
	private POS					pos			= null;
	private long				offset		= NO_OFFSET;
	private int					length		= 0;
	
	/**
	 * Constructor for class Lemma
	 */
	public Lemma()
	{
		lemma = new StringBuilder();
	}
	
	public Lemma(String lemma)
	{
		this.lemma = new StringBuilder(lemma);
		length = 1;
	}
	
	public Lemma(String lemma, POS pos, long offset)
	{
		this.lemma = new StringBuilder(lemma);
		this.pos = pos;
		this.offset = offset;
		length = 1;
	}
	
	public Lemma(String lemma, net.didion.jwnl.data.Synset synset)
	{
		this.lemma = new StringBuilder(lemma);
		pos = synset.getPOS();
		offset = synset.getOffset();
		length = 1;
	}

	public void appendToLemma(Lemma lemma)
	{
		this.lemma.append(lemma.getLemma());
		length += lemma.getLength();
	}

	public void appendToLemma(String word)
	{
		lemma.append(length == 0 || word.startsWith("'") ? word : " " + word);
		length++;
	}
	
	public void appendToLemma(String phrase, int length)
	{
		lemma.append(length == 0 || phrase.startsWith("'") ? phrase : " " + phrase);
		this.length += length;
	}
	
	public void appendTokensToLemma(String phrase)
	{
		appendTokensToLemma(tokenize(phrase));
	}
	
	public void appendTokensToLemma(String[] tokens)
	{
		for (String token : tokens) {
			appendToLemma(token);
		}
	}
	
	private static String[] tokenize(String untokenized)
	{
		untokenized = untokenized.replaceAll("'", " '");
		String[] tokens = untokenized.split(" ");
		for (int i = 0 ; i < tokens.length ; i++) {
			if (tokens[0].isEmpty()) {
				tokens = (String[])ArrayUtils.remove(tokens, i);
			}
		}
		
		return tokens;
	}

	/**
	 * @return the lemma
	 */
	public String getLemma()
	{
		return lemma.toString();
	}
	
	/**
	 * @return the length
	 */
	public int getLength()
	{
		return length;
	}

	
	/**
	 * @return the pos
	 */
	public POS getPos()
	{
		return pos;
	}
	

	/**
	 * @param pos
	 *            the pos to set
	 */
	public void setPos(POS pos)
	{
		this.pos = pos;
	}
	
	/**
	 * @param l
	 *            the offset to set
	 */
	public void setOffset(long l)
	{
		offset = l;
	}
	
	/**
	 * @return the offset
	 */
	public int getOffset()
	{
		return (int)offset;
	}
	
	/**
	 * @param synset the synset to set
	 */
	public void setSynset(net.didion.jwnl.data.Synset synset)
	{
		pos = synset.getPOS();
		offset = synset.getOffset();
	}

	/**
	 * @return the synset
	 */
	public Synset getSynset()
	{
		return new Synset(PosTag.toCategory(pos), offset);
	}

	public boolean wasFound()
	{
		return offset != NO_OFFSET;
	}

	public void setNotFound()
	{
		offset = NO_OFFSET;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getLemma() + "(" + getLength() + ")" + "::" + PosTag.getTypeString(getPos());
	}

}
