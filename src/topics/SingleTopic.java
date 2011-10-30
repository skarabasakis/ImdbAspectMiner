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
package topics;

import java.util.Set;
import config.Globals;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class SingleTopic<Keyword> extends Topic<Keyword> {
	
	private static final long	serialVersionUID	= -4193217864650680097L;

	/**
	 * Constructor for class SingleTopic
	 */
	public SingleTopic()
	{
		super();
	}
	
	public void addKeyword(Keyword keyword, Integer weight)
	{
		topKeywords.put(keyword, weight);
	}
	
	public Set<Keyword> keywords()
	{
		return topKeywords(Globals.TopicParameters.topKeywordsCutoff).keySet();
	}
	
	public int size()
	{
		return topKeywords(Globals.TopicParameters.topKeywordsCutoff).size();
	}
	
	/*
	 * (non-Javadoc)
	 * @see topics.Topic#topicCount()
	 */
	@Override
	public int topicCount()
	{
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * @see topics.Topic#join(topics.SingleTopic)
	 */
	@Override
	public JointTopic<Keyword> join(SingleTopic<Keyword> topic)
	{
		JointTopic<Keyword> joint_topic = new JointTopic<Keyword>();
		joint_topic.setKeywordPrinter(keywordPrinter);
		return joint_topic.join(this).join(topic);
	}
	
	/*
	 * (non-Javadoc)
	 * @see topics.Topic#toSingleTopic()
	 */
	@Override
	public SingleTopic<Keyword> toSingleTopic()
	{
		return this;
	}

}
