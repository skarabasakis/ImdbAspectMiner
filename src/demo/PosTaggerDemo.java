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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import config.Paths;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class PosTaggerDemo {
	
	public static void main(String[] args) throws Exception
	{
		if (args.length != 2) {
			System.err.println("usage: java TaggerDemo modelFile fileToTag");
			return;
		}
		MaxentTagger tagger = new MaxentTagger(Paths.taggerPath + "/" + MaxentTagger.DEFAULT_DISTRIBUTION_PATH);
		
		// String tagged = tagger.tagString("Test a string");
		// System.out.println(tagged);


		List<ArrayList<? extends HasWord>> sentences = MaxentTagger.tokenizeText(new BufferedReader(new FileReader(
				args[1])));
		for (ArrayList<? extends HasWord> sentence : sentences) {
			ArrayList<TaggedWord> tSentence = tagger.apply(sentence)/* tagSentence(sentence) */;
			System.out.println(tSentence);
		}

	}

}
