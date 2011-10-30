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
import indexing.Token;
import indexing.PosTag.PosCategory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import net.didion.jwnl.data.POS;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.Payload;
import util.AppLogger;
import config.Paths;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class StopLemmaFilter extends TokenFilter {
	
	private HashMap<POS, ArrayList<String>>	stopLemmas	= null;
	
	private boolean							removeStopLemmas;

	private TermAttribute		input_term;
	private TypeAttribute		input_type;
	private FlagsAttribute		input_flags;
	private PayloadAttribute	input_payload;
	private TermAttribute		output_term;
	private TypeAttribute		output_type;
	private FlagsAttribute		output_flags;
	private PayloadAttribute	output_payload;
	
	
	/**
	 * Constructor for class StopLemmaFilter
	 * 
	 * @param input
	 */
	public StopLemmaFilter(TokenStream input, boolean removeStopLemmas)
	{
		super(input);
		
		// Getting attributes from input token stream
		input_term = input.getAttribute(TermAttribute.class);
		input_type = input.getAttribute(TypeAttribute.class);
		input_flags = input.getAttribute(FlagsAttribute.class);
		input_payload = input.getAttribute(PayloadAttribute.class);
		
		// Setting attributes for this token stream
		output_term = this.getAttribute(TermAttribute.class);
		output_type = this.getAttribute(TypeAttribute.class);
		output_flags = this.addAttribute(FlagsAttribute.class);
		output_payload = this.getAttribute(PayloadAttribute.class);
		
		this.removeStopLemmas = removeStopLemmas;

		// Initializing stop-lemma lists
		stopLemmas = new HashMap<POS, ArrayList<String>>();
		POS[] pos_array = {POS.VERB, POS.NOUN, POS.ADJECTIVE, POS.ADVERB};
		for (POS pos : pos_array) {
			stopLemmas.put(pos, new ArrayList<String>());
		}

		// Populating stop-lemma lists with data from stop-lemma file
		try {
			BufferedReader stop_file = new BufferedReader(new InputStreamReader(new FileInputStream(Paths.stopLemmasFile)));
			
			// Read stop-lemmas from stop-lemma file, one lemma per line
			String stop_line;
			while ( (stop_line = stop_file.readLine()) != null ) {
				// Each line in the stop-lemma file contains a lemma and its corresponding POS
				// category
				String[] stoplemma = stop_line.split(" ", 2);
				POS stoplemma_pos = PosTag.toPOS(stoplemma[0].toUpperCase());
				String stoplemma_string = stoplemma[1].toLowerCase();
				
				// Adding stop-lemma to the list that corresponds to its POS category
				ArrayList<String> stop_list;
				if ((stop_list = stopLemmas.get(stoplemma_pos)) != null) {
					stop_list.add(stoplemma_string);
				}
				else {
					AppLogger.error.log(Level.WARNING, "Unrecognized POS in stop-lemma file: " + stoplemma_pos
						+ " on line \"" + stop_line + "\"");
				}
			}
			stop_file.close();

		} catch ( FileNotFoundException e ) {
			AppLogger.error.log(Level.SEVERE, "Stop-lemma file not found at location " + Paths.stopLemmasFile);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Error reading from stop-lemma file");
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException
	{
		// Load next token from input stream
		Token current_token;
		if ((current_token = getNextToken()) == null)
			return false;

		// If current token term belongs to the set of stop-lemmas, remove it from the stream if
		// removeStopLemmas is enabled, otherwise change its PosCategory to "other", so that it will
		// be ignored by subsequent analysis steps
		ArrayList<String> stoplemmas;
		if ((stoplemmas = stopLemmas.get(current_token.getPOS())) != null) {
			if (stoplemmas.contains(current_token.term.toLowerCase())) {
				if (removeStopLemmas)
					return incrementToken();
				else {
					current_token.payload.setPosCat(PosCategory.other);
				}
			}
		}

		// Setting attributes for output stream
		output_term.setTermBuffer(current_token.term);
		output_type.setType(current_token.type);
		output_flags.setFlags(current_token.flags);
		output_payload.setPayload(current_token.payload.getPayload());

		return true;
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
					+ "] detected while reading input stream for NegationScopeFilter");
			}
			
			return t;
		}
		else
			return null;

	}
}
