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
package search;



/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class SentimentClassification {
	
	public static enum Subjectivity {
		OBJECTIVE, SUBJECTIVE
	}

	public static enum Polarity {
		NEGATIVE, POSITIVE
	}
	
	public static enum Intensity {
		VERY_WEAK, WEAK, NORMAL, STRONG, VERY_STRONG
	}
	
	private Subjectivity	subjectivity;
	private Float[]			polarity	= null;
	private Float[]			intensity	= null;
	
	/**
	 * Constructor for class Parameters
	 */
	public SentimentClassification()
	{
	}
	
	private Intensity convertFloatToIntensity(Polarity polarity, Float intensity)
	{
		int intensity_level = -1;
		switch (polarity) {
			case POSITIVE:
				intensity_level = Math.round(intensity) - Ratings.MIN.get(Polarity.POSITIVE);
				break;
			case NEGATIVE:
				intensity_level = Ratings.MAX.get(Polarity.NEGATIVE) - Math.round(intensity);
				break;
		}
		return Intensity.values()[intensity_level];
	}
	
	/**
	 * @return the subjectivity
	 */
	public Subjectivity getSubjectivity()
	{
		return subjectivity;
	}
	
	
	/**
	 * @param subjectivity
	 *            the subjectivity to set
	 */
	public void setSubjectivity(Subjectivity subjectivity)
	{
		this.subjectivity = subjectivity;
		polarity = new Float[Polarity.values().length];
		intensity = new Float[Polarity.values().length];
	}
	
	
	/**
	 * @return the polarity
	 */
	private Float getPolarityScore(Polarity polarity)
	{
		return this.polarity[polarity.ordinal()];
	}
	
	public Polarity getPolarity() {
		// Find polarity with maximum support score
		Float max_score = 0F;
		Polarity polarity = null;
		for (Polarity current_polarity : Polarity.values()) {
			Float current_score = getPolarityScore(current_polarity);
			if (getPolarityScore(current_polarity) >= max_score) {
				max_score = current_score;
				polarity = current_polarity;
			}
		}
		
		return polarity;
	}
	
	/**
	 * @param polarity
	 *            the polarity to set
	 */
	public void setPolarity(Polarity polarity, Float score)
	{
		this.polarity[polarity.ordinal()] = score;
	}
	
	
	/**
	 * @return the intensity
	 */
	private Float getIntensityScore(Polarity polarity)
	{
		return intensity[polarity.ordinal()];
	}
	
	public Intensity getIntensity(Polarity polarity)
	{
		return convertFloatToIntensity(polarity, getIntensityScore(polarity));
	}
	
	/**
	 * @param intensity
	 *            the intensity to set
	 */
	public void setIntensity(Polarity polarity, Float intensity)
	{
		this.intensity[polarity.ordinal()] = intensity;
	}
	
}
