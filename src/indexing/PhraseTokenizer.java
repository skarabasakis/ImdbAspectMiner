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

import java.io.Reader;
import org.apache.lucene.analysis.CharTokenizer;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class PhraseTokenizer extends CharTokenizer {

	private static boolean	isFirstTokenChar;


	/**
	 * Constructor for class PhraseTokenizer
	 * 
	 * @param input
	 */
	public PhraseTokenizer(Reader input)
	{
		super(input);
		isFirstTokenChar = true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.lucene.analysis.CharTokenizer#isTokenChar(char)
	 */
	@Override
	protected boolean isTokenChar(char c)
	{
		// TODO Unicode characters for apostrophe
		boolean is_token_char = Character.isLetter(c) || Character.isWhitespace(c) && !isFirstTokenChar || c == '\''
			|| c == '-';
		
		isFirstTokenChar = is_token_char ? false : true;

		return is_token_char;
	}
}
