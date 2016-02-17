//	sopaRead.java
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

class sopaRead extends Thread
{
	BufferedInputStream bis;
	byte[] dataByte;
	final int EXT_BUF_SIZE = 8192;
	long length;
	long iLength;
	boolean isFinished = false;

	public sopaRead(BufferedInputStream bis,byte dataByte[])
	{
		this.bis = bis;
		this.dataByte = dataByte;
	}

	public void run()
	{
		DataInputStream diStream = new DataInputStream(bis);
		int iBytesRead;
		byte[] dummyByte = new byte[44];

		iLength = 0;
		try
		{
			iBytesRead = diStream.read(dummyByte,0,44);
			if(iBytesRead == 44)
			{
				System.out.println("Start loading data");
				iBytesRead = 0;
				while(iBytesRead >= 0 && iLength < length)
				{
					iBytesRead = diStream.read(dataByte,(int)iLength,(int)length - (int)iLength);
					iLength += iBytesRead;
				}
				System.out.println(iLength + " bytes loaded");	
			}
			isFinished = true;
			diStream.close();
		}
		catch(IOException e)
		{
			System.out.println("error");
			System.exit(1);
		}
	}
}
