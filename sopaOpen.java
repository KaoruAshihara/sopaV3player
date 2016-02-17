//	sopaOpen.java
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

import java.io.*;
import java.io.IOException;
import java.util.*;
import java.util.Arrays;

class sopaOpen
{
	BufferedInputStream bis;
	int iOverlap,iSampleRate,iVersion;
	long lChunkSize;

	public sopaOpen(BufferedInputStream bis)
	{
		this.bis = bis;
	}

	public boolean readHeader()
	{
		long lLong;
		final int headerSize = 44;
		DataInputStream dis = new DataInputStream(bis);
		int iNum,iBytesRead;
		int nTerm0[] = {82,73,70,70};				// RIFF
		int nTerm1[] = {83,79,80,65};				// SOPA
		int nTerm2[] = {102,109,116};				// fmt
		int nTmp[] = new int[4];
		int nFmt[] = new int[3];
		byte[] headerByte = new byte[headerSize];

		try
		{
			iBytesRead = dis.read(headerByte,0,headerSize);
			dis.close();
		}
		catch(IOException e)
		{
			return false;
		}

		for(iNum = 0;iNum < 4;iNum ++)
		{
			nTmp[iNum] = headerByte[iNum];
		}
		if(!Arrays.equals(nTmp,nTerm0))
			return false;
		for(iNum = 0;iNum < 4;iNum ++)
			nTmp[iNum] = headerByte[8 + iNum];
		if(!Arrays.equals(nTmp,nTerm1))
			return false;
		for(iNum = 0;iNum < 3;iNum ++)
			nFmt[iNum] = headerByte[12 + iNum];
		if(!Arrays.equals(nFmt,nTerm2))
			return false;
		if(headerByte[16] != 16)
			return false;
		if(headerByte[20] != 1)
			return false;
		iOverlap = (int)headerByte[22];
		if(iOverlap != 2 && iOverlap != 4)
			return false;
		iSampleRate = (int)headerByte[25] & 0x000000ff;
		iSampleRate *= 256;
		iSampleRate += headerByte[24] & 0xff;
		System.out.println("Sample rate " + iSampleRate + " Hz");
		for(iNum = 0;iNum < 4;iNum ++)
			nTmp[iNum] = headerByte[36 + iNum];
		System.out.println("SOPA file version " + nTmp[3] + "." + nTmp[2] + "." + nTmp[1] + "." + nTmp[0]);
		iVersion = nTmp[3];
		if(nTmp[3] < 2){
			System.out.println("Sorry. This version is not available!");
			return false;
		}
		else if(nTmp[3] > 2)
			iOverlap = 2;
		lChunkSize = (long)headerByte[43] & 0x000000ff;
		lChunkSize *= 16777216;
		lLong = (long)headerByte[42] & 0x000000ff;
		lLong *= 65536;
		lChunkSize += lLong;
		lLong = (long)headerByte[41] & 0x000000ff;
		lLong *= 256;
		lChunkSize += lLong;
		lChunkSize += (long)headerByte[40];

/*		lChunkSize = (long)headerByte[40];
		lChunkSize += (long)(headerByte[41] << 8);
		lChunkSize += (long)(headerByte[42] << 16);
		lChunkSize += (long)(headerByte[43] << 24);	*/
		System.out.println("Data chunk size " + lChunkSize);
		return true;
	}
}
