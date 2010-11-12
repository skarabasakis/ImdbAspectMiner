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
package indexing.filters;

import indexing.Token;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.Payload;
import topics.ReviewSentencesCollector;
import util.AppLogger;
import classes.Counter;
import classes.ReviewId;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class SentenceCollectorFilter extends TokenFilter {
	
	private ReviewId			reviewId;
	private Counter				sentenceNumber;
	private ArrayList<Token>	sentenceTokens;
	
	private ReviewSentencesCollector	sentences;
	
	private TermAttribute		input_term;
	private TypeAttribute		input_type;
	private FlagsAttribute		input_flags;
	private PayloadAttribute	input_payload;
	private TermAttribute		output_term;
	private TypeAttribute		output_type;
	private FlagsAttribute		output_flags;
	private PayloadAttribute	output_payload;

	
	/**
	 * Constructor for class IndexableFilter
	 * 
	 * @param input
	 */
	public SentenceCollectorFilter(TokenStream input, ReviewSentencesCollector tokenLists, ReviewId reviewId)
	{
		super(input);
		
		// Getting attributes from input token stream
		input_term = input.getAttribute(TermAttribute.class);
		input_type = input.getAttribute(TypeAttribute.class);
		input_flags = input.getAttribute(FlagsAttribute.class);
		input_payload = input.hasAttribute(PayloadAttribute.class) ? input.getAttribute(PayloadAttribute.class) : null;
		
		// Setting attributes for this token stream
		output_term = this.getAttribute(TermAttribute.class);
		output_type = this.getAttribute(TypeAttribute.class);
		output_flags = this.addAttribute(FlagsAttribute.class);
		output_payload = input.hasAttribute(PayloadAttribute.class) ? this.getAttribute(PayloadAttribute.class) : null;
		
		this.reviewId = reviewId;
		sentenceNumber = new Counter();
		sentences = tokenLists;
		sentenceTokens = new ArrayList<Token>();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException
	{
		Token current_token;
		if ((current_token = getNextToken()) != null) {
			
			if (current_token.isDelim(true)) {

				if (!sentenceTokens.isEmpty()) {
					sentenceNumber.increment();
					sentenceTokens.add(current_token);
					sentences.setTokenListForSentence(sentenceId(), sentenceTokens);
				}

				sentenceTokens.clear();
			}
			else {
				sentenceTokens.add(current_token);
			}
			
			output_term.setTermBuffer(input_term.term());
			output_type.setType(input_type.type());
			output_flags.setFlags(input_flags.getFlags());
			output_payload.setPayload(input_payload.getPayload());

			return true;
		}
		else {
			if (!sentenceTokens.isEmpty()) {
				sentenceNumber.increment();
				sentences.setTokenListForSentence(sentenceId(), sentenceTokens);
			}
			return false;
		}
	}
	
	private Token getNextToken() throws IOException
	{
		if (input.incrementToken()) {
			// Reading term attributes from input stream
			String term = input_term.term();
			String postag = input_type.type();
			int flags = input_flags.getFlags();
			Payload payload = input_payload.getPayload();
			
			// Adding token into buffer
			Token t = new Token(term, postag, flags);
			if (t.payload.decode(payload.getData()) == false) {
				AppLogger.error.log(Level.WARNING, "Invalid payload data for token [" + term + ":" + postag
					+ "] detected while reading input stream for NegationFilter");
			}
			
			return t;
		}
		else
			return null;
		
	}

	private Long sentenceId()
	{
		return new Long(reviewId.get() * 1000L + sentenceNumber.get());
	}
}
