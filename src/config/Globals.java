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
package config;



/**
 * Global constants for AspectMiner project
 * 
 * @author Stelios Karabasakis
 */
public class Globals {
	
	public static class IndexFieldNames {
		
		public static String	reviewid	= "reviewid";
		public static String	rating		= "rating";
		public static String	title		= "title";
		public static String	text		= "text";

	}
	
	public static class Corpus {
		
		public static final int	N_SENTIMENT_CLASSES	= 10;
		public static final int	TotalReviews		= 1618938;
	}
	
	public static class Parameters {
		
		public static final float	ObjectivityTolerance	= 1.0F;
		public static final float	PNFrequencyCutoff		= 0.1F;
		public static final float	IntensityMassThreshold	= 0.25F;
		
	}
	
	public static class LexiconParameters {
		
		public static final int		minimumSynsetInstances	= 5;
		public static final int		minimumTermInstances	= 10;
		public static final float	polarityScoreThreshold	= 0.0f;
	}
	
	public static class TopicParameters {
		
		public static final float	subjectivityThreshold			= 1.5F;
		public static final float	topicMergeSimilarityThreshold	= 0.46F;
		public static final int		topKeywordsCutoff				= 20;
		public static final int		globalTopicId					= 0;
		public static final String	globalTopicName					= "*ALL";
	}
	
	public static class SentimentParameters {
		
		public static final int	MaxSentimentsPerTerm	= 5;
		public static final int	UsableSentimentsPerTerm	= 3;
	}
	
	public static class DependencyParameters {
		
		public static final float				minimumSentimentScore	= 0.0f /* 0.4F */;
	}

	public static final int	NO_FLAGS			= 0;
	public static final int	FLAG_FILTERED		= -1;
	public static final int	FLAG_NAMEDENTITY	= 1;


}
