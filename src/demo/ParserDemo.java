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

import indexing.Token;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import config.Globals;
import config.Paths;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;


class ParserDemo {
	
	public static void main(String[] args)
	{
		LexicalizedParser lp = new LexicalizedParser(Paths.depParserPath + "/englishPCFG.ser.gz");
		lp.setOptionFlags(new String[]{ "-maxLength", "80", "-retainTmpSubcategories" });

		String[] sent = { "This", "is", "an", "egerggbnnthnnbenhofen", "sentence" };
		Tree parse = lp.apply(Arrays.asList(sent));
		
		Token[] sentence = { new Token("This", "DT", Globals.NO_FLAGS) };

		// lp.parse("This is an easy sentence");
		// parse.pennPrint();
		
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		Collection<TypedDependency> tdl = gs.typedDependencies();
		System.out.println(tdl);
		System.out.println();

		// Let's try and read and process the typed dependency collection
		Iterator<TypedDependency> tds = tdl.iterator();

		while ( tds.hasNext() ) {
			TypedDependency current_td = tds.next();
			
			System.out.println(current_td.reln().getParent().getParent().getParent().getLongName());
		}

		// TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
		// tp.printTree(parse);
	}

}
