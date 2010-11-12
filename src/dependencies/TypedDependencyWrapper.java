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
package dependencies;

import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.TypedDependency;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class TypedDependencyWrapper implements Comparable<TypedDependencyWrapper> {
	
	// private TypedDependency theDependency = null;
	private static final int	BELOW	= -1;
	private static final int	ABOVE	= 1;
	private static final int	EQUALS	= 0;
	
	private int					govIndex;
	private String				govLabel;
	private int					depIndex;
	private String				depLabel;
	private GrammaticalRelation	relation;

	/**
	 * Constructor for class TypedDependencyWrapper
	 */
	public TypedDependencyWrapper(TypedDependency d)
	{
		govIndex = d.gov().index();
		govLabel = d.gov().value().toLowerCase();
		depIndex = d.dep().index();
		depLabel = d.dep().value().toLowerCase();
		relation = d.reln();
	}
	
	public int getGovIndex()
	{
		return govIndex;
	}
	
	public String getGovLabel()
	{
		return govLabel;
	}
	
	public int getDepIndex()
	{
		return depIndex;
	}
	
	public String getDepLabel()
	{
		return depLabel;
	}
	
	public GrammaticalRelation getRelation()
	{
		return relation;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TypedDependencyWrapper other)
	{
		if (govIndex == other.govIndex) {
			// Relations with common governor: leading dependent wins
			if (depIndex > other.depIndex)
				return BELOW;
			else if (depIndex < other.depIndex)
				return ABOVE;
		}
		else {
			// Consecutive (i.e. chained) relations: top relation wins
			if (govIndex == other.depIndex)
				return BELOW;
			else if (depIndex == other.govIndex)
				return ABOVE;
				
			// Disconnected relations (i.e. no common nodes): leading governor wins
			else if (govIndex > other.govIndex)
				return BELOW;
			else if (govIndex < other.govIndex)
				return ABOVE;
		}

		return EQUALS;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return relation.getShortName() + "(" + govLabel + "-" + govIndex + ", " + depLabel + "-" + depIndex + ")";
	}
}
