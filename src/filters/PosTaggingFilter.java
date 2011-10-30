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
package filters;

import indexing.PosTag;
import indexing.ReviewTermPayload;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import util.AppLogger;
import config.Paths;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;


/**
 * A TokenFilter that <strong>consumes</strong> text segments, each segment constituting of one or
 * more complete sentences of text, and <strong>produces</strong> a series of POS-tagged tokens.
 * Each produced token represents a single entity, i.e. a single word or a punctuation mark. <h4>
 * Consumed attributes:</h4>
 * <ul>
 * <li>{@link TermAttribute}: should store the input text segment
 * </ul>
 * <h4>Produced attributes:</h4>
 * <ul>
 * <li>{@link TermAttribute}: stores the word or punctuation symbol represented by the token
 * <li>{@link TypeAttribute}: stores the POS tag of the token
 * </ul>
 * 
 * @author Stelios Karabasakis
 */
public class PosTaggingFilter extends TokenFilter {
	
	private MaxentTagger		tagger			= null;
	private LinkedList<String>	currentTokens	= new LinkedList<String>();
	
	private TermAttribute		input_term		= null;
	private TermAttribute		output_term		= null;
	private TypeAttribute		output_type		= null;
	private PayloadAttribute	output_payload	= null;
	
	// private PayloadAttribute output_payload = null;
	

	/**
	 * Constructor for class PosTaggingFilter
	 * 
	 * @param input
	 */
	public PosTaggingFilter(TokenStream input)
	{
		super(input);
		
		// Getting attributes from input token stream
		input_term = input.getAttribute(TermAttribute.class);
		
		// Setting attributes for this token stream
		output_term = this.addAttribute(TermAttribute.class);
		output_type = this.addAttribute(TypeAttribute.class);
		output_payload = this.addAttribute(PayloadAttribute.class);

		tagger = initializeTagger();
	}
	
	/**
	 * Constructor for class PosTaggingFilter
	 * 
	 * @param input
	 * @param tagger
	 *            The POS-tagger object that should be used to perform the tagging
	 */
	public PosTaggingFilter(TokenStream input, MaxentTagger tagger)
	{
		super(input);
		
		// Getting attributes from input token stream
		input_term = input.getAttribute(TermAttribute.class);
		
		// Setting attributes for this token stream
		output_term = this.getAttribute(TermAttribute.class);
		output_type = this.addAttribute(TypeAttribute.class);
		output_payload = this.addAttribute(PayloadAttribute.class);

		this.tagger = tagger;
	}
	
	public MaxentTagger initializeTagger()
	{
		MaxentTagger tagger = null;
		
		try {
			tagger = new MaxentTagger(Paths.taggerPath + "/" + MaxentTagger.DEFAULT_DISTRIBUTION_PATH);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot read model " + MaxentTagger.DEFAULT_DISTRIBUTION_PATH
				+ " from the following location:\n" + Paths.taggerPath);
		} catch ( ClassNotFoundException e ) {
			AppLogger.error.log(Level.SEVERE, "An error occured while loading a POS tagger based on model "
				+ MaxentTagger.DEFAULT_DISTRIBUTION_PATH);
		}
		
		return tagger;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException
	{
		// If we are out of buffered input, try to load the next input segment into the buffer
		if (currentTokens.isEmpty()) {
			if (input.incrementToken()) {
				// Calling the POS Tagger to tag the next segment of the input
				String pos_tagged_segment = tagger.tagString(input_term.term());
				
				// Tokenizing the output of the POS tagger. Tokens are stored into the currentTokens
				// list, which acts as a temporary term buffer that is preserved between calls.
				tokenizePosTaggedText(pos_tagged_segment);
			}
			else
				// No more input
				return false;
		}
		
		// If we do have input tokens pending in the currentTokens buffer, then process the first
		// token from the buffer.
		String next_token = currentTokens.remove();
		int split_position = next_token.lastIndexOf('/');
		String next_token_term, next_token_type;
		try {
			next_token_term = next_token.substring(0, split_position);
			next_token_type = next_token.substring(split_position + 1);
		} catch ( StringIndexOutOfBoundsException e ) {
			AppLogger.error.log(Level.SEVERE, "Token \"" + next_token + "\" lacks a POS tag and will not be indexed");
			return incrementToken();
		}

		output_term.setTermBuffer(next_token_term);
		output_type.setType(next_token_type);
		output_payload.setPayload(ReviewTermPayload.getPayload(PosTag.toCategory(next_token_type), PosTag
			.getDegree(next_token_type), false, false, false));

		return true;
	}
	
	/**
	 * Break down the output of the POS Tagger into individual tokens and store them into
	 * currentTokens
	 * 
	 * @param inputString
	 */
	private void tokenizePosTaggedText(String PosTaggedText)
	{
		String[] current_tokens_array = PosTaggedText.split(" ");
		
		int length = current_tokens_array.length;
		for (int i = 0 ; i < length ; i++) {
			currentTokens.add(current_tokens_array[i]);
		}
	}

}
