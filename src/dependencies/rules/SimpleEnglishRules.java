// The MIT License
//
// Copyright (c) 2011 Stelios Karabasakis
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
package dependencies.rules;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import wordnet.Synset.SynsetCategory;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import filters.indexing.NegationScopeFilter;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class SimpleEnglishRules {
	
	private static String[]				negationTerms	= NegationScopeFilter.negationTerms;
	private static String[]				negationAux		= { "should", "would", "could", "might", "ought" };
	private static String[]				positiveMods	= { "enough" };
	private static String[]				negativeMods	= { "too" };
	private static String[]				softPreps		= { "like" };

	public static List<DependencyRule>	defaultRules	= defaultRules();

	private static List<DependencyRule> defaultRules()
	{
		DependencyRule[] rules = {

		/**************
		 * Negation
		 **************/
		new DependencyRule("neg", DependencyRuleOutcomes.NEGATE_GOV),
			new DependencyRule("det,prt,advmod,dobj,nsubj,dep", null, negationTerms, DependencyRuleOutcomes.NEGATE_GOV),
			new DependencyRule("pobj", negationTerms, null, DependencyRuleOutcomes.NEGATE_DEP),
			new DependencyRule("aux", null, negationAux, DependencyRuleOutcomes.NEGATE_GOV),


			/**************
			 * Subjects
			 **************/
			new DependencyRule("nsubj,nsubjpass", DependencyRuleOutcomes.INTENSIFY_GOV),
			new DependencyRule("csubj,csubjpass", DependencyRuleOutcomes.REFLECT_GOV),
			
			/*************
			 * Objects
			 *************/
			new DependencyRule("dobj", DependencyRuleOutcomes.REFLECT_GOV),
			new DependencyRule("iobj", DependencyRuleOutcomes.UNCHANGED_GOV),
			new DependencyRule("pobj", DependencyRuleOutcomes.UNCHANGED_DEP),
			
			/***************
			 * Modifiers
			 ***************/
			new DependencyRule("advmod,amod", null, positiveMods, DependencyRuleOutcomes.POSITIVE_GOV),
			new DependencyRule("advmod,amod", null, negativeMods, DependencyRuleOutcomes.NEGATIVE_GOV),
			new DependencyRule("advmod", categories(SynsetCategory.V), null, DependencyRuleOutcomes.REFLECT_GOV),
			new DependencyRule("advmod", DependencyRuleOutcomes.INTENSIFY_GOV),
			new DependencyRule("amod", DependencyRuleOutcomes.REFLECT_GOV),
			new DependencyRule("infmod", categories(SynsetCategory.J), null, DependencyRuleOutcomes.REFLECT_GOV),
			new DependencyRule("infmod", DependencyRuleOutcomes.INTENSIFY_DEP),
			new DependencyRule("partmod", categories(SynsetCategory.J), null, DependencyRuleOutcomes.REFLECT_DEP),
			new DependencyRule("partmod", DependencyRuleOutcomes.STRONGER_DEP),
			new DependencyRule("quantmod", DependencyRuleOutcomes.INTENSIFY_GOV),
			new DependencyRule("prt", DependencyRuleOutcomes.STRONGER_GOV),
			new DependencyRule("prep", null, softPreps, DependencyRuleOutcomes.UNCHANGED_GOV),
			new DependencyRule("prep", DependencyRuleOutcomes.REFLECT_GOV),


			/***********************
			 * Clausal Modifiers
			 ***********************/
			new DependencyRule("advcl", categories(SynsetCategory.J), null, DependencyRuleOutcomes.REFLECT_DEP),
			new DependencyRule("advcl", DependencyRuleOutcomes.UNCHANGED_DEP),
			new DependencyRule("purpcl", DependencyRuleOutcomes.UNCHANGED_DEP),

			/******************************
			 * Clauses and Conjunctions
			 ******************************/
			new DependencyRule("ccomp", DependencyRuleOutcomes.REFLECT_GOV),
			new DependencyRule("xcomp", DependencyRuleOutcomes.REFLECT_GOV),
			new DependencyRule("acomp", DependencyRuleOutcomes.REFLECT_GOV),
			new DependencyRule("conj", DependencyRuleOutcomes.AVG_GOV),
			new DependencyRule("appos", DependencyRuleOutcomes.AVG_GOV),
			new DependencyRule("parataxis", DependencyRuleOutcomes.AVG_GOV),
			new DependencyRule("dep", DependencyRuleOutcomes.STRONGER_DEP),

			new DependencyRule("", DependencyRuleOutcomes.UNCHANGED_GOV),

		};
		
		return Arrays.asList(rules);
	}

	/**
	 * @param j
	 * @return
	 */
	private static SynsetCategory[] categories(SynsetCategory... cat)
	{
		return cat;
	}
	
	public static Set<GrammaticalRelation>	clausalRelations	= relations("csubj", "csubjpass", "dobj", "iobj",
																			"prep", "infmod", "advcl", "purpcl",
																			"ccomp", "xcomp", "conj", "dep",
																			"parataxis", "appos");
	
	private static Set<GrammaticalRelation> relations(String... relns)
	{
		HashSet<GrammaticalRelation> relnset = new HashSet<GrammaticalRelation>();
		for (String reln : relns) {
			relnset.add(EnglishGrammaticalRelations.valueOf(reln));
		}
		
		return relnset;
	}
}
