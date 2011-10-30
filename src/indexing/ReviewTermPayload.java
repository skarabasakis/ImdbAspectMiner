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
package indexing;

import indexing.PosTag.PosCategory;
import java.io.Serializable;
import java.util.Date;
import java.util.Random;
import org.apache.lucene.index.Payload;


/**
 * Stores and manages payload data for an indexed term
 * 
 * @author Stelios Karabasakis
 */
public class ReviewTermPayload implements Serializable {
	
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1706187872859820513L;

	private PosTag.PosCategory	posCat;
	private ComparisonDegree	degree;
	private boolean				proper;
	private boolean				unlemmatizable;
	private boolean				negation;
	
	public ReviewTermPayload() {
		posCat = PosTag.PosCategory.other;
		degree = ComparisonDegree.POSITIVE;
		proper = false;
		unlemmatizable = false;
		negation = false;
	}
	
	public ReviewTermPayload(PosTag.PosCategory posCat, ComparisonDegree degree, boolean proper,
			boolean unlemmatizable,
			boolean negation)
	{
		this.posCat = posCat;
		this.degree = degree;
		this.proper = proper;
		this.unlemmatizable = unlemmatizable;
		this.negation = negation;
	}

	/**
	 * Constructor for class ReviewTermPayload
	 * 
	 * @param payload
	 */
	public ReviewTermPayload(Payload payload)
	{
		this.decode(payload.getData());
	}

	public Payload getPayload()
	{
		return new Payload(this.encode());
	}
	
	public static Payload getPayload(PosTag.PosCategory posCat, ComparisonDegree degree, boolean proper,
			boolean unlemmatizable, boolean negation)
	{
		return new Payload(encode(posCat, degree, proper, unlemmatizable, negation));
	}

	public byte[] encode()
	{
		return encode(posCat, degree, proper, unlemmatizable, negation);
	}
	
	public static byte[] encode(PosTag.PosCategory posCat, ComparisonDegree degree, boolean proper,
			boolean unlemmatizable, boolean negation)
	{
		byte payload_value = (byte)(posCat.ordinal() + /* bytes 0-2: posCat */
		(degree.ordinal() << 3) + /* bytes 3-4: degree */
		((proper ? 1 : 0) << 5) + /* byte 5: proper */
		((unlemmatizable ? 1 : 0) << 6) + /* byte 6: unlemmatizable */
		((negation ? 1 : 0) << 7) /* byte 7: negation */
		);
		byte[] payload = { 0 };
		payload[0] = payload_value;
		
		return payload;
	}
	
	public boolean decode(byte[] encoded) {
		return decode(encoded, this);
	}
	
	public static boolean decode(byte[] encoded, ReviewTermPayload decoded)
	{
		byte payload_value = encoded[0];

		decoded.negation = (payload_value >> 7 & 0x01) == 0 ? false : true;
		decoded.unlemmatizable = (payload_value >> 6 & 0x01) == 0 ? false : true;
		decoded.proper = (payload_value >> 5 & 0x01) == 0 ? false : true;
		decoded.degree = ComparisonDegree.values()[payload_value >> 3 & 0x03];
		decoded.posCat = PosTag.PosCategory.values()[payload_value & 0x07];
	
		return true;
	}
	
	/**
	 * @return the posCat
	 */
	public PosTag.PosCategory getPosCat()
	{
		return posCat;
	}
	
	/**
	 * @param posCat the posCat to set
	 */
	public void setPosCat(PosTag.PosCategory posCat)
	{
		this.posCat = posCat;
	}
	

	/**
	 * @return the degree
	 */
	public ComparisonDegree getDegree()
	{
		return degree;
	}
	
	
	/**
	 * @param degree
	 *            the degree to set
	 */
	public void setDegree(ComparisonDegree degree)
	{
		this.degree = degree;
	}
	
	/**
	 * @return the proper
	 */
	public boolean isProper()
	{
		return proper;
	}
	
	
	/**
	 * @param proper
	 *            the proper to set
	 */
	public void setProper(boolean proper)
	{
		this.proper = proper;
		if (proper == true) {
			posCat = PosCategory.N;
		}
	}
	
	/**
	 * @return the unlemmatizable
	 */
	public boolean isUnlemmatizable()
	{
		return unlemmatizable;
	}
	
	
	/**
	 * @param unlemmatizable
	 *            the unlemmatizable to set
	 */
	public void setUnlemmatizable(boolean unlemmatizable)
	{
		this.unlemmatizable = unlemmatizable;
	}
	
	/**
	 * @return the negation
	 */
	public boolean isNegation()
	{
		return negation;
	}
	
	/**
	 * @param negation the negation to set
	 */
	public void setNegation(boolean negation)
	{
		this.negation = negation;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		ReviewTermPayload target = (ReviewTermPayload)obj;
		
		return posCat == target.posCat && degree == target.degree && unlemmatizable == target.unlemmatizable
			&& proper == target.proper && negation == target.negation;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return (negation ? "!" : " ") + (unlemmatizable ? "?" : " ") + (proper ? "N" : " ") + degreeString() + " "
			+ PosTag.getTypeString(posCat);
	}
	
	private String degreeString() {
		switch (degree) {
			case NONE:
				return " ";
			case POSITIVE:
				return "_";
			case COMPARATIVE:
				return "C";
			case SUPERLATIVE:
				return "S";
			default:
				return " ";
		}
	}
	
	public boolean isPlain()
	{
		return negation == false && (degree == ComparisonDegree.NONE || degree == ComparisonDegree.POSITIVE);
	}

	private void randomize()
	{
		Random generator = new Random(new Date().getTime());
		
		posCat = PosTag.PosCategory.values()[generator.nextInt(PosTag.PosCategory.values().length)];
		degree = ComparisonDegree.values()[generator.nextInt(ComparisonDegree.values().length)];
		proper = generator.nextBoolean();
		unlemmatizable = generator.nextBoolean();
		negation = generator.nextBoolean();
	}

	public static void main(String[] args)
	{
		// TODO Delete main
		boolean test_result;
		do {
			ReviewTermPayload p	= new ReviewTermPayload();
			p.randomize();
	
			ReviewTermPayload pt = new ReviewTermPayload();
			pt.decode(p.encode());
			
			test_result = pt.equals(p);
			System.out.println(test_result + " --> " + p.toString() + " // " + pt.toString());


			try {
				Thread.sleep(1100);
			} catch ( InterruptedException e ) {
				e.printStackTrace();
			}
		} while ( true );
	}
}
