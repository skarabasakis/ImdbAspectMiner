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

import lexicon.TermSentiment.Intensity;
import lexicon.TermSentiment.Polarity;
import lexicon.TermSentiment.Sentiment;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class DependencyRuleOutcomes {
	
	private static final Intensity[]		i				= Intensity.values();
	private static final int				intensities		= i.length;
	private static final int				min_intensity	= 0;
	private static final int				max_intensity	= intensities - 1;
	private static final int				mid_intensity	= (min_intensity + max_intensity) / 2;

	
	private static DependencyRuleOutcomes	Outcomes		= new DependencyRuleOutcomes();
	
	public static Outcome					POSITIVE_GOV	= Outcomes.new Positive(false);
	public static Outcome					POSITIVE_DEP	= Outcomes.new Positive(true);
	public static Outcome					NEGATIVE_GOV	= Outcomes.new Negative(false);
	public static Outcome					NEGATIVE_DEP	= Outcomes.new Negative(true);
	public static Outcome					UNCHANGED_GOV	= Outcomes.new Unchanged(false);
	public static Outcome					UNCHANGED_DEP	= Outcomes.new Unchanged(true);
	public static Outcome					STRONGER_GOV	= Outcomes.new Stronger(false);
	public static Outcome					STRONGER_DEP	= Outcomes.new Stronger(true);
	public static Outcome					AVG_GOV			= Outcomes.new Avg(false);
	public static Outcome					AVG_DEP			= Outcomes.new Avg(true);
	public static Outcome					INTENSIFY_GOV	= Outcomes.new Intensify(false);
	public static Outcome					INTENSIFY_DEP	= Outcomes.new Intensify(true);
	public static Outcome					REFLECT_GOV		= Outcomes.new Reflect(false);
	public static Outcome					REFLECT_DEP		= Outcomes.new Reflect(true);
	public static Outcome					NEGATE_GOV		= Outcomes.new Negate(false);
	public static Outcome					NEGATE_DEP		= Outcomes.new Negate(true);
	
	
	public class Positive extends Outcome {
		
		public Positive(boolean reverse)
		{
			super(reverse);
		}
		
		@Override
		protected Float score(Sentiment base, Sentiment mod)
		{
			return base.getScore();
		}
		
		@Override
		protected Polarity polarity(Sentiment base, Sentiment mod)
		{
			return Polarity.POSITIVE;
		}
		
		@Override
		protected Intensity intensity(Sentiment base, Sentiment mod)
		{
			return base.getIntensity();
		}
		
	}
	
	public class Negative extends Outcome {
		
		public Negative(boolean reverse)
		{
			super(reverse);
		}
		
		@Override
		protected Float score(Sentiment base, Sentiment mod)
		{
			return base.getScore();
		}
		
		@Override
		protected Polarity polarity(Sentiment base, Sentiment mod)
		{
			return Polarity.NEGATIVE;
		}
		
		@Override
		protected Intensity intensity(Sentiment base, Sentiment mod)
		{
			return base.getIntensity();
		}
		
	}

	public class Unchanged extends Outcome {
		
		public Unchanged(boolean reverse)
		{
			super(reverse);
		}
		
		@Override
		protected Float score(Sentiment base, Sentiment mod)
		{
			return base.getScore();
		}
		
		@Override
		protected Polarity polarity(Sentiment base, Sentiment mod)
		{
			return base.getPolarity();
		}
		
		@Override
		protected Intensity intensity(Sentiment base, Sentiment mod)
		{
			return base.getIntensity();
		}
		
	}
	
	public class Stronger extends Outcome {
		
		public Stronger(boolean reverse)
		{
			super(reverse);
		}
		
		@Override
		protected Float score(Sentiment base, Sentiment mod)
		{
			return base.getIntensity().ordinal() >= mod.getIntensity().ordinal() ? base.getScore() : mod.getScore();
		}
		
		@Override
		protected Polarity polarity(Sentiment base, Sentiment mod)
		{
			return base.getIntensity().ordinal() >= mod.getIntensity().ordinal() ? base.getPolarity() : mod
				.getPolarity();
		}
		
		@Override
		protected Intensity intensity(Sentiment base, Sentiment mod)
		{
			return base.getIntensity().ordinal() >= mod.getIntensity().ordinal() ? base.getIntensity() : mod
				.getIntensity();
		}
		
	}
	
	public class Avg extends Outcome {
		
		public Avg(boolean reverse)
		{
			super(reverse);
		}
		
		@Override
		protected Float score(Sentiment base, Sentiment mod)
		{
			if (base.getPolarity() == mod.getPolarity())
				return (base.getScore() + mod.getScore()) / 2;
			else
				return base.getIntensity().ordinal() >= mod.getIntensity().ordinal() ? base.getScore() : mod.getScore();
		}
		
		@Override
		protected Polarity polarity(Sentiment base, Sentiment mod)
		{
			if (base.getPolarity() == mod.getPolarity())
				return base.getPolarity();
			else
				return base.getIntensity().ordinal() >= mod.getIntensity().ordinal() ? base.getPolarity() : mod
					.getPolarity();
		}
		
		@Override
		protected Intensity intensity(Sentiment base, Sentiment mod)
		{
			if (base.getPolarity() == mod.getPolarity())
				return i[Math.round((base.getIntensity().ordinal() + mod.getIntensity().ordinal()) / 2.0f)];
			else
				return base.getIntensity().ordinal() >= mod.getIntensity().ordinal() ? base.getIntensity() : mod
					.getIntensity();
		}
		
	}
	
	public class Intensify extends Outcome {
		
		public Intensify(boolean reverse)
		{
			super(reverse);
		}
		
		@Override
		protected Float score(Sentiment base, Sentiment mod)
		{
			return (base.getScore() + mod.getScore()) / 2;
		}
		
		@Override
		protected Polarity polarity(Sentiment base, Sentiment mod)
		{
			return base.getPolarity();
		}
		
		
		@Override
		protected Intensity intensity(Sentiment base, Sentiment mod)
		{
			return i[Math.round(base.getIntensity().ordinal() + (max_intensity - base.getIntensity().ordinal())
				* mod.getIntensity().ordinal() / (float)intensities)];
		}
	}
	
	
	public class Reflect extends Outcome {
		
		public Reflect(boolean reverse)
		{
			super(reverse);
		}
		
		@Override
		protected Float score(Sentiment base, Sentiment mod)
		{
			return (base.getScore() + mod.getScore()) / 2;
		}
		
		@Override
		protected Polarity polarity(Sentiment base, Sentiment mod)
		{
			return mod.getIntensity() == Intensity.WEAKEST ? base.getPolarity() : mod.getPolarity();
		}
		
		@Override
		protected Intensity intensity(Sentiment base, Sentiment mod)
		{
			if (base.getPolarity() == mod.getPolarity()) {
				int intensity = Math.round(base.getIntensity().ordinal() + (mod.getIntensity().ordinal() - mid_intensity)
							/ (intensities / 2));
				intensity = Math.max(0, Math.min(intensities - 1, intensity));
				return i[intensity];
			}
			else
				return mod.getIntensity();
		}
		
	}

	public class Negate extends Outcome {
		
		public Negate(boolean reverse)
		{
			super(reverse);
		}
		
		@Override
		protected Float score(Sentiment base, Sentiment mod)
		{
			return base.getScore();
		}
		
		@Override
		protected Polarity polarity(Sentiment base, Sentiment mod)
		{
			if (base.getIntensity().ordinal() > mid_intensity)
				return base.getPolarity();
			else
				return base.getPolarity() == Polarity.POSITIVE ? Polarity.NEGATIVE : Polarity.POSITIVE;
		}
		
		@Override
		protected Intensity intensity(Sentiment base, Sentiment mod)
		{
			if (base.getIntensity().ordinal() > mid_intensity)
				return i[base.getIntensity().ordinal() - intensities / 2];
			else
				return i[intensities / 2 - base.getIntensity().ordinal()];
		}
		
	}
	

}
