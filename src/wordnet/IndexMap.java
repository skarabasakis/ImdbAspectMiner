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
package wordnet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class IndexMap implements Serializable {
	
	private static final long			serialVersionUID	= 4491762253642324190L;

	private HashMap<String, IndexMap>	map	= null;
	
	public boolean has(String key)
	{
		return map.containsKey(key);
	}
	
	public IndexMap get(String key)
	{
		return map.get(key);
	}
	
	public IndexMap get(String[] keys)
	{
		IndexMap current_map = this;
		for (String key : keys) {
			current_map = current_map.get(key);
			if (current_map == null) {
				break;
			}
		}

		return current_map;
	}

	public IndexMap put(String key)
	{
		return map.put(key, new IndexMap());
	}

	public void index(ArrayList<String[]> keyphrases)
	{
		for (String[] keyphrase : keyphrases) {
			int current_level = 0;
			IndexMap current_map = this;
			while ( current_level <= keyphrase.length ) {
				String key = current_level == keyphrase.length ? "." : keyphrase[current_level];
				if (!current_map.has(key)) {
					current_map.put(key);
				}
				current_map = current_map.get(key);
				current_level++;
			}
		}
	}
	
	public boolean isEndNode()
	{
		return map.containsKey(".");
	}
	
	public boolean isIntermediateNode()
	{
		return map.containsKey(".") ? map.size() > 1 : map.size() >= 1;
	}



	/**
	 * Constructor for class IndexMap
	 */
	public IndexMap()
	{
		map = new HashMap<String, IndexMap>(2, 1.0F);
	}


	public static void main(String[] args)
	{
		IndexMap nouns = new IndexMap();
		
		String[] compound = {"rain","cats","and","dogs"};
		
		int i = 0;
		IndexMap lookup_map = nouns;
		while (i < compound.length) {
			if (lookup_map.has(compound[i])) {
				lookup_map = lookup_map.get(compound[i]);
				i++;
			}
			else {
				break;
			}
		}

	}

}
