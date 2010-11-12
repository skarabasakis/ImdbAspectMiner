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
package demo;


import indexing.Lemma;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;
import net.didion.jwnl.dictionary.Dictionary;
import wordnet.DictionaryFactory;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class WordnetTest {
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Dictionary wordnet = DictionaryFactory.getWordnetInstance();

		try {
			//IndexWord i = wordnet.lookupIndexWord(POS.VERB, "works out");
			//System.out.println(i.getLemma());
			String lookup_string = "localised";
			int lookup_string_length = lookup_string.split(" ").length;
			IndexWordSet j = wordnet.lookupAllIndexWords(lookup_string);
			IndexWord[] ja = j.getIndexWordArray();
			Lemma lemma_to_return = null;
			for (int i = 0 ; i < ja.length ; i++) {
				Lemma candidate_lemma = new Lemma();
				
				IndexWord candidate_indexword = ja[i];
				

				
				candidate_lemma.appendTokensToLemma(candidate_indexword.getLemma());
				candidate_lemma.setPos(candidate_indexword.getPOS());
				if (candidate_lemma.getLength() == lookup_string_length) {
					System.out.print(candidate_indexword.getSense(1).getKey() + "\n");
					

					if (candidate_indexword.getSenseCount() == 1) {
						Synset candidate_synset = wordnet.getSynsetAt(candidate_indexword.getPOS(), candidate_indexword
							.getSense(1).getOffset());
						Word[] w_array = candidate_synset.getWords();
						for (Word w : w_array) {
							System.out.print(w.getLemma() + " // ");
						}
						System.out.println();
					}


				}
			}
			

			System.out.println(lemma_to_return);

			//Iterator k = wordnet.getIndexWordIterator(POS.NOUN, "special");
			//while ( k.hasNext() ) {
			//	System.out.print(" - ");
			//	System.out.println(((IndexWord)k.next()).getLemma());
			//}


		} catch ( JWNLException e ) {
			System.out.println(JWNL.resolveMessage(e.getMessage()));
		} catch ( NullPointerException e ) {
			System.out.println("null");
		}

	}
	
}
