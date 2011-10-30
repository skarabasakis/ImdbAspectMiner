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
package dependencies.visualisation;

import indexing.Token;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import lexicon.TermSentiment.Sentiment;
import lexicon.TermSentiment.Sentiment.SentimentFormat;
import util.AppLogger;
import config.Paths;
import dependencies.DependencyTree;
import dependencies.DependencyTreeParse;
import dependencies.TypedDependencyWrapper;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class DependencyTreeParseViz extends DependencyTreeParse {
	
	private String				basename;
	private static NumberFormat	filenf	= NumberFormat.getInstance();
	
	/**
	 * Constructor for class DependencyTreeParseViz
	 * 
	 * @param sentence
	 * @throws IllegalArgumentException
	 */
	public DependencyTreeParseViz(ArrayList<Token> sentence) throws IllegalArgumentException
	{
		super(sentence);
		basename = new Long(new Date().getTime()).toString();
		filenf.setMinimumIntegerDigits(2);
	}
	
	/**
	 * Constructor for class DependencyTreeParseViz
	 */
	public DependencyTreeParseViz(DependencyTree tree)
	{
		super(tree);
	}
	
	protected void visualize_init()
	{
		printDotFile(true);
	}

	@Override
	protected void visualize()
	{
		printDotFile(false);
	}

	private BufferedWriter getDotFileWriter(String path)
	{
		File dotfile = new File(Paths.dotFilesPath + path);
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(dotfile, false));
		} catch ( IOException e ) {
			try {
				dotfile.getParentFile().mkdirs();
				w = new BufferedWriter(new FileWriter(dotfile, false));
			} catch ( IOException e1 ) {
				AppLogger.error.log(Level.SEVERE, "Cannot initialize step tree visualization file at location:" + path);
				System.exit(-1);
			}
		}
		
		return w;
	}
	
	private BufferedWriter getDotFile(int step)
	{
		return getDotFileWriter(basename + (step == relationIndex.size() ? "" : "/" + filenf.format(step)) + ".gv");
	}
	
	private BufferedWriter getDotFile()
	{
		return getDotFileWriter(basename + ".gv");
	}
	
	private String stmt(String stmt)
	{
		return "\t" + stmt + ";\n";
	}
	
	private String invisible(String stmt)
	{
		return "\t" + stmt + "[style=invisible color=white];\n";
	}

	// Get the numeric rating that corresponds to sentiment s. This is an utility function
	private String snum(Sentiment s)
	{
		return s.toString(SentimentFormat.SIGNED_INTEGER_WZERO, false);
	}
	
	// Get the graphviz font color name that corresponds to sentiment s
	private String color(Sentiment s)
	{
		String color = "grey30";
		if (!s.isNeutral()) {
			switch (s.getPolarity()) {
				case POSITIVE:
					color = "green";
					break;
				case NEGATIVE:
					color = "red";
					break;
			}
			
			switch (s.getIntensity()) {
				case WEAKEST:
					color += "1";
					break;
				case WEAK:
					color += "2";
					break;
				case NORMAL:
					break;
				case STRONG:
					color += "3";
					break;
				case STRONGEST:
					color += "4";
					break;
			}
		}
		
		return color;
	}
	
	// Get the graphviz font color name that corresponds to sentiment s
	private String fillcolor(Sentiment s)
	{
		String color = "grey50";
		if (!s.isNeutral()) {
			switch (s.getPolarity()) {
				case POSITIVE:
					color = "green";
					break;
				case NEGATIVE:
					color = "tomato";
					break;
			}
			
			switch (s.getIntensity()) {
				case WEAKEST:
					color += "1";
					break;
				case WEAK:
					color += "2";
					break;
				case NORMAL:
					break;
				case STRONG:
					color += "3";
					break;
				case STRONGEST:
					color += "4";
					break;
			}
		}
		
		return color;
	}

	private String topicStr(int topicId)
	{
		return "t" + topicId + " [" //
			+ "label=\"" + nodeTopics.get(topicId) //
			+ "\" " //
			+ "fillcolor=grey60]";
	}
	
	private String topicSentimentStr(int topicId)
	{
		Sentiment s = nodeSentiments.get(nodeTopicRoots.get(topicId));
		return "t" + topicId + " [" //
			+ "label=\"" + nodeTopics.get(topicId)//
			+ (s.isNeutral() ? "" : " " + snum(s)) //
			+ "\" " //
			+ "fillcolor=" + fillcolor(s) + "]";
	}
	
	private String nodeStr(int nodeId)
	{
		Sentiment s = nodeSentimentsInit.get(nodeId);
		return "n" + nodeId + " [label=\"" + tokenIndex.get(nodeId - 1).word() //
			+ "|" + snum(s) + "\"]" //
			+ "[fontcolor=" + color(s) + " color=" + color(s) + "]";
		
	}
	
	private String nodeLeafStr(int nodeId)
	{
		return "l" + nodeId;
	}
	
	private String nodeSentimentStr(int nodeId)
	{
		Sentiment s = nodeSentiments.get(nodeId);
		return "n" + nodeId + "s [label=\"" + snum(s) + "\"]" //
			+ "[fillcolor=" + fillcolor(s) + "]";
	}

	private String nodeNeutralStr(int nodeId)
	{
		return "n" + nodeId + " [label=\"" + tokenIndex.get(nodeId - 1).word() + "\"]";
	}
	
	private String nodeTopicStr(int nodeId)
	{
		return "n" + nodeId + " [label=\"" + tokenIndex.get(nodeId - 1).word() + "\"]" //
			+ "[shape=house color=grey20 fontcolor=black width=0.6]";
	}
	
	private String relationStr(int relId)
	{
		TypedDependencyWrapper rel = relationIndex.get(relId);

		return "n" + rel.getGovIndex() + " -> n" + rel.getDepIndex() //
			+ " [label=" + rel.getRelation().getShortName() //
			// + " headlabel=" + relId //
			+ "]";
	}
	
	private String relationActiveStr(int relId)
	{
		TypedDependencyWrapper rel = relationIndex.get(relId);
		
		return "n" + rel.getGovIndex() + " -> n" + rel.getDepIndex() //
			+ " [label=" + rel.getRelation().getShortName() //
			// + " headlabel=" + relId //
			+ "][style=bold color=blue]";
	}

	private String relationTraversedStr(int relId)
	{
		TypedDependencyWrapper rel = relationIndex.get(relId);
		
		return "n" + rel.getGovIndex() + " -> n" + rel.getDepIndex() //
			+ " [label=" + rel.getRelation().getShortName() //
			// + " headlabel=" + relId //
			+ " style=dotted]";
	}
	
	private String relationLeafStr(int nodeId)
	{
		return "n" + nodeId + " -> l" + nodeId;
	}
	
	private String edgeTopicStr(int topicId, Integer nodeId)
	{
		String n = "n" + nodeId;
		String t = "t" + topicId;
		return "{rank=same; " + n + " " + t + "}; " + n + " -> " + t;
	}
	
	private String edgeSentimentStr(int nodeId)
	{
		String n = "n" + nodeId;
		String ns = "n" + nodeId + "s";
		return "{rank=same; " + n + " " + ns + "}; " + n + " -> " + ns;
	}

	private void printDotFile(boolean init)
	{
		try {
			BufferedWriter g = init ? getDotFile() : getDotFile(next);
			
			// Header
			g.write("digraph g" + basename + " {\n\n");
			g.write(stmt("graph [nodesep=0.1 splines=false bb=1 landscape=false ordering=out]"));
			g.write(stmt("label=\"" + getSentence() + "\""));
			g.write(stmt("labelloc=t"));
			g.newLine();
			
			// Token nodes, numbered by their nodeid, and marked up with their prior sentiment
			Set<Integer> nodes = nodeSentimentsInit.keySet();
			g.write( //
				stmt("node [shape=record style=\"solid,filled\" color=grey60 fontcolor=grey60 fillcolor=lightyellow "
					+ "fontname=\"Arial\" fontsize=9 height=0.1]"));
			for (Integer node : relationTerms) {
				if (nodeTopics.containsKey(node)) {
					g.write(stmt(nodeTopicStr(node)));
				}
				else if (nodeSentimentsInit.get(node).isNeutral()) {
					g.write(stmt(nodeNeutralStr(node)));
				}
				else {
					g.write(stmt(nodeStr(node)));
				}
			}
			g.newLine();
			
			// Invisible Leaf Nodes (no meaning, just used to force proper alignment on the graph
			// rendering)
			for (Integer node : leafNodes) {
				g.write(invisible(nodeLeafStr(node)));
			}
			g.newLine();
			
			// Topic Nodes, number by their current root position
			g.write( //
				stmt("node [shape=folder style=\"filled,solid\" color=black fontcolor=white height=0.3]"));
			for (Entry<Integer, Integer> nodeTopicEntry : nodeTopicRoots.entrySet()) {
				if (!init && traversedNodes.contains(nodeTopicEntry.getValue())
					&& !leafNodes.contains(nodeTopicEntry.getValue())) {
					g.write(stmt(topicSentimentStr(nodeTopicEntry.getKey())));
				}
				else {
					g.write(stmt(topicStr(nodeTopicEntry.getKey())));
				}
			}
			g.newLine();
			
			// Sentiment Nodes
			Set<Integer> topicRoots = new HashSet<Integer>(nodeTopicRoots.values());
			g.write( //
				stmt("node [shape=circle color=white style=filled fixedsize=true width=0.3 height=0.3]"));
			for (Integer snode : relationTerms) {
				if (!init && traversedNodes.contains(snode) && !leafNodes.contains(snode)) {
					
					if (!topicRoots.contains(snode)) {
						g.write(stmt(nodeSentimentStr(snode)));
					}
				}
				else {
					if (!topicRoots.contains(snode)) {
						g.write(invisible(nodeSentimentStr(snode)));
					}
				}
			}
			g.newLine();
			
			// Relation edges, untraversed
			g.write( //
				stmt("edge [color=steelblue fontcolor=steelblue dir=both arrowhead=none arrowsize=0.5 fontsize=8.0 style=solid arrowtail=vee labelangle=90]"));
			for (int relid = relationIndex.size() - 1 ; relid > next ; relid--) {
				g.write(stmt(relationStr(relid)));
			}
			
			// Relation edge, current
			if (next < relationIndex.size()) {
				if (!init) {
					g.write(stmt(relationActiveStr(next)));
				}
				else {
					g.write(stmt(relationStr(next)));
				}
			}
			
			// Relation edges, traversed
			for (int relid = next - 1 ; relid > -1 ; relid--) {
				g.write(stmt(relationTraversedStr(relid)));
			}
			g.newLine();
			
			// Invisible Leaf edges (again, just used for proper alignment)
			for (Integer node : leafNodes) {
				g.write(invisible(relationLeafStr(node)));
			}
			g.newLine();
			
			// Sentiment Edges
			g.write(//
				stmt("edge [color=grey20 dir=forward style=solid arrowhead=dot labelangle=0 labeldistance=0 nodesep=0.2]"));
			for (Integer snode : relationTerms) {
				if (!topicRoots.contains(snode)) {
					if (!init && traversedNodes.contains(snode) && !leafNodes.contains(snode)) {
						g.write(stmt(edgeSentimentStr(snode)));
					}
					else {
						g.write(invisible(edgeSentimentStr(snode)));
					}
				}
			}
			g.newLine();
			
			// Topic Edges
			g.write(stmt("edge [color=grey20 dir=forward style=solid arrowhead=ediamond]"));
			for ( Entry<Integer, Integer> topicEntry: nodeTopicRoots.entrySet()) {
				g.write(stmt(edgeTopicStr(topicEntry.getKey(), topicEntry.getValue())));
			}
			g.newLine();
			

			// Footer
			g.write("}\n");
			g.newLine();
			
			g.close();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Error writing to dotfile:" + e.getMessage());
		}
	}
	

}
