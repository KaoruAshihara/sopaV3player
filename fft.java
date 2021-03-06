//	fft.java
//	Programmed by K. Ashihara

/*********************************************************************************

Copyright AIST, 2015

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the ?gSoftware?h), to deal in the
Software without restriction, including without limitation the rights to use, copy,
modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the
following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED ?gAS IS?h, WITHOUT WARRANTY OF ANY
KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

**********************************************************************************/

class fft {
	int iTap;
	final double dWpi = 2 * Math.PI;
	
	boolean fastFt(double dData[],double dImg[],boolean isRev){
		double sc,f,c,s,t,c1,s1,x1,kyo1;
		double dHan,dPower,dPhase;
		int n,j,i,k,ns,l1,i0,i1;
		int iInt;

		if(!isPowerOfTwo(iTap))
			return false;
		if(!isRev){
			for(iInt = 0;iInt < iTap;iInt ++)
			{
				dImg[iInt] = 0;														// Imaginary part 
				dHan = (1 - Math.cos((dWpi * (double)iInt) / (double)iTap)) / 2;	// Hanning Window 
				dData[iInt] *= dHan;												// Real part 
			}
		}	
		/*	printf("******************** Arranging BIT ******************\n"); */

		n = iTap;	/* NUMBER of DATA */
		sc = Math.PI;
		j = 0;
		for(i = 0;i < n - 1;i ++)
		{
			if(i <= j)
			{
				t = dData[i];  dData[i] = dData[j];  dData[j] = t;
				t = dImg[i];   dImg[i] = dImg[j];   dImg[j] = t;
			}
			k = n / 2;
			while(k <= j)
			{
				j = j - k;
				k /= 2;
			}
			j += k;
		}
		/*	printf("******************** MAIN LOOP **********************\n"); */
		ns = 1;
		if(isRev)															// inverse
			f = 1.0;
		else
			f = -1.0;
		while(ns <= n / 2)
		{
			c1 = Math.cos(sc);
			s1 = Math.sin(f * sc);
			c = 1.0;
			s = 0.0;
			for(l1 = 0;l1 < ns;l1 ++)
			{
				for(i0 = l1;i0 < n;i0 += (2 * ns))
				{
					i1 = i0 + ns;
					x1 = (dData[i1] * c) - (dImg[i1] * s);
					kyo1 = (dImg[i1] * c) + (dData[i1] * s);
					dData[i1] = dData[i0] - x1;
					dImg[i1] = dImg[i0] - kyo1;
					dData[i0] = dData[i0] + x1;
					dImg[i0] = dImg[i0] + kyo1;
				}
				t = (c1 * c) - (s1 * s);
				s = (s1 * c) + (c1 * s);
				c = t;
			}
			ns *= 2;
			sc /= 2.0;
		}
		if(!isRev)
		{
			for(iInt = 0;iInt < iTap;iInt ++)
			{
				dData[iInt] /= (double)iTap;
				dImg[iInt] /= (double)iTap;
				dPower = Math.sqrt(dData[iInt] * dData[iInt] + dImg[iInt] * dImg[iInt]);
				dPhase = Math.atan2(dImg[iInt],dData[iInt]);
				dData[iInt] = dPower;
				dImg[iInt] = dPhase;
			}
		}
		return true;
	}

	public static boolean isPowerOfTwo(int x)
	{  

		return x > 0 && (x & (x - 1)) == 0;  

	}
}
