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
package lexicon;

import indexing.ComparisonDegree;
import indexing.ReviewTermPayload;
import java.util.Collection;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class PayloadFilters {
	
	public static interface PayloadFilter {
		
		public String name();
		
		public boolean filterPayload(ReviewTermPayload p);
	}
	
	public static PayloadFilter	FILTER_NONE			= new PayloadFilter() {
														
														public String name()
														{
															return "all";
														}
														
														public boolean filterPayload(ReviewTermPayload p)
														{
															return true;
														}
													};

	public static PayloadFilter	FILTER_PLAIN		= new PayloadFilter() {
														
														public String name()
														{
															return "plain";
														}
														
														@Override
														public boolean filterPayload(ReviewTermPayload p)
														{
															ComparisonDegree degree = p.getDegree();
															boolean negation = p.isNegation();
															return negation == false
																&& (degree == ComparisonDegree.NONE || degree == ComparisonDegree.POSITIVE);
														}
													};
	
	public static PayloadFilter	FILTER_NEGATED		= new PayloadFilter() {
														
														public String name()
														{
															return "negated";
														}
														
														@Override
														public boolean filterPayload(ReviewTermPayload p)
														{
															ComparisonDegree degree = p.getDegree();
															boolean negation = p.isNegation();
															return negation == true
																&& (degree == ComparisonDegree.NONE || degree == ComparisonDegree.POSITIVE);
														}
													};
	
	public static PayloadFilter	FILTER_COMPARATIVE	= new PayloadFilter() {
														
														public String name()
														{
															return "comparative";
														}
														
														@Override
														public boolean filterPayload(ReviewTermPayload p)
														{
															ComparisonDegree degree = p.getDegree();
															boolean negation = p.isNegation();
															return negation == false
																&& degree == ComparisonDegree.COMPARATIVE;
														}
													};
	
	public static PayloadFilter	FILTER_SUPERLATIVE	= new PayloadFilter() {
														
														public String name()
														{
															return "superlative";
														};
														
														@Override
														public boolean filterPayload(ReviewTermPayload p)
														{
															ComparisonDegree degree = p.getDegree();
															boolean negation = p.isNegation();
															return negation == false
																&& degree == ComparisonDegree.SUPERLATIVE;
														}
													};
	
	/**
	 * Test a given payload against a list of payload filters and returns the first matching filter
	 * from the list that accepts the payload.
	 * 
	 * @param p
	 *            The payload to be tested
	 * @param candidate_filters
	 *            The list of filters to
	 * @return The first filter from the candidate filters list that matches the payload, or null if
	 *         none of the filters accepts the payload
	 */
	public static PayloadFilter findMatchingFilter(ReviewTermPayload p, Collection<PayloadFilter> candidate_filters)
	{
		try {
			for (PayloadFilter filter : candidate_filters) {
				if (filter.filterPayload(p))
					return filter;
			}
			
			return null;
		} catch ( NullPointerException e ) {
			return null;
		}
	}

}
