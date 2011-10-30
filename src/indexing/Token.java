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

import java.io.Serializable;
import net.didion.jwnl.data.POS;
import wordnet.Synset;
import edu.stanford.nlp.ling.HasTag;
import edu.stanford.nlp.ling.HasWord;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class Token implements HasWord, HasTag, Serializable {
	
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -3411501645464029086L;

	public String				term;
	public String				type;
	public Integer				flags;
	public ReviewTermPayload	payload;
	
	public Token(String term, String type, int flags)
	{
		this.term = term;
		this.type = type;
		this.flags = flags;
		payload = new ReviewTermPayload();
	}
	
	public Token(String term, String type, int flags, ReviewTermPayload payload)
	{
		this.term = term;
		this.type = type;
		this.flags = flags;
		this.payload = payload;
	}

	public POS getPOS()
	{
		return PosTag.toPOS(type);
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.stanford.nlp.ling.HasWord#setWord(java.lang.String)
	 */
	@Override
	public void setWord(String word)
	{
		term = word;
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.stanford.nlp.ling.HasWord#word()
	 */
	@Override
	public String word()
	{
		// TODO Auto-generated method stub
		return term;
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.stanford.nlp.ling.HasTag#setTag(java.lang.String)
	 */
	@Override
	public void setTag(String tag)
	{
		type = tag;
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.stanford.nlp.ling.HasTag#tag()
	 */
	@Override
	public String tag()
	{
		// TODO Return tag by reading the payload if necessary
		return type;
	}
	
	public Synset synset()
	{
		if (isLemmatized())
			return new Synset(payload.getPosCat(), flags);
		else
			return new Synset(payload.getPosCat());
	}

	public void concat(Token token)
	{
		setWord(word() + " " + token.word());
		setTag(token.tag());
	}

	public boolean isProper()
	{
		return payload.isProper();
	}

	public boolean isOpenClass()
	{
		return PosTag.isOpenClass(PosTag.toCategory(type));
	}

	public boolean isIndexable()
	{
		return isOpenClass() && !payload.isProper();
	}
	
	public boolean isLemmatized()
	{
		return !payload.isUnlemmatizable();
	}
	
	public boolean isSynset()
	{
		return flags != 0;
	}

	public boolean isDelim(boolean sentence_delim) {
		return sentence_delim ? PosTag.isSentenceDelim(PosTag.toCategory(type)) : PosTag.isDelim(PosTag
			.toCategory(type));
	}
	
	public boolean isQuotation(boolean closing)
	{
		return type.equals("``") || type.equals("''") && closing;
	}

	public ComparisonDegree getDegree()
	{
		return PosTag.getDegree(type);
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "[" + (payload == null ? "" : payload.toString() + " ")
			+ (flags == 0 ? "\t\t\t" : synset().toString() + "\t")
			+ term + "/" + type + "]\n";
	}

}
