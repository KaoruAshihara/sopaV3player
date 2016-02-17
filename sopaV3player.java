//	sopaV3player.java
//  version 1.0
//	Programmed by K. Ashihara

/*********************************************************************************

Copyright AIST, 2015

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the ÅgSoftwareÅh), to deal in the
Software without restriction, including without limitation the rights to use, copy,
modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the
following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED ÅgAS ISÅh, WITHOUT WARRANTY OF ANY
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
import java.util.Date;

import java.awt.*;
import java.awt.event.*;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

class sopaV3player
{
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			printUsageAndExit();
		}

		final int EXTERNAL_BUFFER_SIZE = 8192;
		final int DB_SIZE = 130048;						// Number of data in the database
		String	strFilename = args[0];
		File	sopaFile = null;
		FileInputStream inStream = null;
		BufferedInputStream bis = null;
		AudioFormat audioFormat = null;
		URL sopaUrl = null;
		HttpURLConnection huc = null;

		boolean isLocal = false;
		sopaOpen so;
		sopaRead sr;
		Starter starter;
		int iSt = 0;
		long lNanoSec;
		int nOverlap,nSampleRate,nVersion;
		int iNum = 0;
		int nChannels = 2;
		short[] sHrtf = new short[DB_SIZE];				// HRTF database (amplitude)
		short[] sPhase = new short[DB_SIZE];			// HRTF database (phase)
		byte[] dsByte;

// Prepare database ****************
		DataInputStream din = null;
		try
		{
			din = new DataInputStream(new FileInputStream("hrtf3d512.bin"));
			for(iNum = 0;iNum < DB_SIZE;iNum ++) 
			{															// BigEndian to LittleEndian
				sHrtf[iNum] = (short)((din.readByte() & 0xff) + (din.readByte() << 8));
			}
			din.close();
		}
		catch(Exception e)
		{
			System.out.println("HRTF data error! : " + e);
			System.exit(1);
		}

		try
		{
			din = new DataInputStream(new FileInputStream("phase3d512.bin"));
			for(iNum = 0;iNum < DB_SIZE;iNum ++) 
			{															// BigEndian to LittleEndian
				sPhase[iNum] = (short)((din.readByte() & 0xff) + (din.readByte() << 8));
			}
			din.close();
		}
		catch(Exception e)
		{
			System.out.println("PHASE data error! : " + e);
			System.exit(1);
		}
		System.out.println("HRTF data number " + iNum + "\n");

// Read header of the SOPA file ****************
		try{
			URLConnection connection = new URL(strFilename).openConnection();
			// Get last modified date
			lNanoSec = connection.getLastModified();
		}
		catch(Exception e){
			System.out.println("Search for a local file" + e);
			isLocal = true;
			sopaFile = new File(strFilename);
			// Get last modified date
			lNanoSec = sopaFile.lastModified();
			if(lNanoSec == 0L){
				System.out.println("URL connection error!" + e);
				System.exit(0);
			}
		}
		Date modifiedDate = new Date(lNanoSec);
		System.out.println("This file was modified at " + modifiedDate.toString());

		if(isLocal){					// Reproduce a SOPA file in the local directory
			try{
				inStream = new FileInputStream(sopaFile);
				bis = new BufferedInputStream(inStream);
			}
			catch(Exception e){
				System.out.println("File error! : " + e);
				System.exit(1);
			}
		}
		else{							// Reproduce a SOPA file from the URL
			try{
				sopaUrl = new URL(strFilename);
				sopaUrl.openConnection();
				huc = (HttpURLConnection)sopaUrl.openConnection();
				bis = new BufferedInputStream(huc.getInputStream());
			}
			catch(Exception e){
				System.out.println("URL error! : " + e);
				System.exit(1);
			}
		}

		so = new sopaOpen(bis);
		if(!so.readHeader()){
			System.out.println("Header error!");
			System.exit(0);
		}

		nOverlap = so.iOverlap;						// Overlap factor
		nSampleRate = so.iSampleRate;				// Sampling rate
		nVersion = so.iVersion;						// SOPA version
			
		System.out.println("Data length " + so.lChunkSize + " bytes");	
		try
		{
			bis.close();
			if(isLocal)
				inStream.close();
			else
				huc.disconnect();
		}
		catch(IOException e)
		{
			System.out.println("Stream error! : " + e);
			System.exit(1);
		}

// Read data stream from the SOPA file ****************
		if(isLocal){
			try{
				inStream = new FileInputStream(sopaFile);
				bis = new BufferedInputStream(inStream);
			}
			catch(Exception e){
				System.out.println("File error! : " + e);
				System.exit(1);
			}
		}
		else{
			try{
				sopaUrl = new URL(strFilename);
				sopaUrl.openConnection();
				bis = new BufferedInputStream(sopaUrl.openStream());
			}
			catch(Exception e){
				System.out.println("URL error! : " + e);
				System.exit(1);
			}
		}
		dsByte = new byte[(int)so.lChunkSize];
		sr = new sopaRead(bis,dsByte);
		sr.length = so.lChunkSize;
		sr.start();

		while(!sr.isFinished){
			lNanoSec = System.nanoTime();
			System.out.print("*");
		}
		System.out.println("\nReady to go");

		try
		{
			bis.close();
			if(isLocal)
				inStream.close();
		}
		catch(IOException e)
		{
			System.out.println("Stream error! : " + e);
			System.exit(1);
		}

// Reproduction ****************
		byte[] bRet = new byte[2];
		final int nBit = 16;
		final int iBYTE = nBit / 8;
		int nBytesWritten = 0;
		int iInt,iProc,iRem,iHlf,iSize = 1024;
		int iRatio = 44100 / nSampleRate;
		int iNumb,iNumImage;
		int iNumber,iNumberImage;
		int[] iTmp = new int[EXTERNAL_BUFFER_SIZE];
		int[] nTmp = new int[EXTERNAL_BUFFER_SIZE / iBYTE];
		double dSpL,dSpR,dSpImageL,dSpImageR;
		double dPhaseL,dPhaseR,dPhaseImageL,dPhaseImageR;
    		
		fft trans = new fft();							// Fft class instance
    		
		long lBuffSize = EXTERNAL_BUFFER_SIZE / iBYTE;
		int iOff = 0;
		int iOffset = 0;

		starter = new Starter();
		starter.start();

		Date date = new Date();
		while(iSt == 0)
		{
			try{
				Thread.sleep(20);
			}catch(InterruptedException e){}

			date.getTime();
			if(starter.isStarted || starter.isStopped)
				iSt = 1;
		}
		if(starter.isStopped)
			System.exit(0);
    		
		iNum = 0;
    		
		audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,nSampleRate,nBit,nChannels,
			nChannels * nBit / 8,nSampleRate,false);

		SourceDataLine	line = null;
		DataLine.Info	info = new DataLine.Info(SourceDataLine.class,audioFormat);
		try
		{
			line = (SourceDataLine) AudioSystem.getLine(info);

			line.open(audioFormat);
		}
		catch (LineUnavailableException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Line opened");
		line.start();
		System.out.println("Line started");

		while(iNum < lBuffSize)
		{
			iTmp[iOff + 1] = (short)dsByte[iNum * 4] & 0xff;									// Directional data
			iTmp[iOff] = (short)dsByte[iNum * 4 + 1] & 0xff;									// Directional data
			nTmp[iNum] = dsByte[iNum * 4 + 3] * 256 + (dsByte[iNum * 4 + 2] & 0x000000ff);		// PCM data
			if(iTmp[iOff] == 255 && iOff != 0)
			{
				iSize = iOff * 2;					// Frame size
				lBuffSize = iNum;
				System.out.println("Frame size " + iSize);
			}
			else
			{
				iNum ++;
				iOff += 2;
				iOffset += 4;
			}
		}
		iProc = iSize / nOverlap;
		iRem = iSize - iProc;
		iHlf = iSize / 2;
		trans.iTap = iSize;
		iRatio *= iSize / 512;
    		
		int iAnglLeft;
		int iFrm,iSet,iLastFrm,iFrmCnt;
		int[][] iAngl = new int[nOverlap][iProc * 2];		// Sound image direction
		short[][] sData = new short[nChannels][iSize];		// Synthesized data
		int[] abData = new int[iSize];
		byte[] abByte = new byte[iSize * 4];				// Byte array of synthesized data
		double dTmp,dRamp;
		double dAtt = 4096;
		double dReL[] = new double[iSize + 1];
		double dImL[] = new double[iSize + 1];
		double dReR[] = new double[iSize + 1];
		double dImR[] = new double[iSize + 1];
		double[] dHann = new double[iSize];					// Hann window function
		boolean bIsEnd = false;
			
		iLastFrm = nOverlap - 1;

// Prepare Hanning window ****************
		dRamp = (double)iSize / 8;

		for(iInt = 0;iInt < iSize;iInt ++)
		{		// Hann window
			if(iInt < (short)dRamp){
				dHann[iInt] = (1 - Math.cos(Math.PI * (double)iInt / dRamp)) / ((double)nOverlap * 2);	
			}
			else if(iInt >= iSize - (short)dRamp){
				dHann[iInt] = (1 - Math.cos(Math.PI * ((double)iSize - (double)iInt) / dRamp)) / ((double)nOverlap * 2);
			}
			else{
				dHann[iInt] = 1 / (double)nOverlap;
			}
			dImR[iInt] = 0;
		}	
		
		starter.isPlaying = true;

// Prepare initial data ****************	
		iOff = iSet = iFrm = 0;
		for(iNum = 0;iNum < iSize;iNum ++)
		{
			if(iNum < iProc)
			{
				iAngl[iFrm][iOff] = iTmp[iSet];
				iAngl[iFrm][iOff + 1] = iTmp[iSet + 1];
				dReR[iNum] = abData[iNum] = nTmp[iNum];		// PCM data
			}
			else
			{
				iAngl[iFrm][iOff + 1] = (short)dsByte[iOffset] & 0xff;
				iAngl[iFrm][iOff] = (short)dsByte[iOffset + 1] & 0xff;
				abData[iNum] = dsByte[iOffset + 3] * 256 + (dsByte[iOffset + 2] & 0x000000ff);
				dReR[iNum] = abData[iNum];					// PCM data
			}
			iOff += 2;
			iOffset += 4;
			if(iOff == iProc * 2)
			{
				iFrm ++;
				iOff = 0;
			}
			iSet += 2;
		}

		while(nBytesWritten < sr.length && !starter.isStopped)
		{
			if(trans.fastFt(dReR,dImR,false))				// FFT
			{
				dReR[iSize] = dReR[0];
				dImR[iSize] = dImR[0];

				for(iNum = 0;iNum < iHlf;iNum ++)
				{
					int nImage = iSize - iNum;
					int iFreq = iNum / iRatio;
					int iImg = 511 - iFreq;

					if(iFreq == 0){
						dSpR = dSpL = dReR[iNum];
						dSpImageL = dSpImageR = dReR[nImage];
						dPhaseL = dPhaseR = dImR[iNum];
						dPhaseImageL = dPhaseImageR = dImR[nImage];
					}
					else if(nVersion > 2){
						if(iAngl[0][iNum] >= 254 && iAngl[0][iHlf + iNum] >= 254){
							dSpR = dSpL = dReR[iNum];
							dSpImageL = dSpImageR = dReR[nImage];
							dPhaseL = dPhaseR = dImR[iNum];
							dPhaseImageL = dPhaseImageR = dImR[nImage];
						}
						else{
							if(iAngl[0][iNum] >= 254){
								iAngl[0][iNum] = iAngl[0][iHlf + iNum];
							}
							else if(iAngl[0][iHlf + iNum] >= 254){
								iAngl[0][iHlf + iNum] = iAngl[0][iNum];
							}

							iNumb = 512 * iAngl[0][iNum] + iFreq;		// Address in database
							iNumImage = 512 * iAngl[0][iNum] + iImg;
							iNumber = 512 * iAngl[0][iNum + iHlf] + iFreq;		// Address in database
							iNumberImage = 512 * iAngl[0][iNum + iHlf] + iImg;

							dTmp = (double)sHrtf[iNumb] + (double)sHrtf[iNumber] / 2;
							dSpR = dReR[iNum] * dTmp / dAtt;
							dTmp = ((double)sPhase[iNumb] + (double)sPhase[iNumber]) / 20000;
							if(Math.abs(sPhase[iNumb] - sPhase[iNumber]) > 31415){
								if(dTmp < 0)
									dTmp += Math.PI;
								else
									dTmp -= Math.PI;
							}
							dPhaseR = dImR[iNum] + dTmp;

							dTmp = (double)sHrtf[iNumImage] + (double)sHrtf[iNumberImage] / 2;
							dSpImageR = dReR[nImage] * dTmp / dAtt;
							dTmp = ((double)sPhase[iNumImage] + (double)sPhase[iNumberImage]) / 20000;
							if(Math.abs(sPhase[iNumImage] - sPhase[iNumberImage]) > 31415){
								if(dTmp < 0)
									dTmp += Math.PI;
								else
									dTmp -= Math.PI;
							}
							dPhaseImageR = dImR[nImage] + dTmp;

							iAnglLeft = opposit(iAngl[0][iNum]);
							iNumb = 512 * iAnglLeft + iFreq;
							iNumImage = 512 * iAnglLeft + iImg;
							iAnglLeft = opposit(iAngl[0][iNum + iHlf]);
							iNumber = 512 * iAnglLeft + iFreq;		// Address in database
							iNumberImage = 512 * iAnglLeft + iImg;

							dTmp = (double)sHrtf[iNumb] + (double)sHrtf[iNumber] / 2;
							dSpL = dReR[iNum] * dTmp / dAtt;
							dTmp = ((double)sPhase[iNumb] + (double)sPhase[iNumber]) / 20000;
							if(Math.abs(sPhase[iNumb] - sPhase[iNumber]) > 31415){
								if(dTmp < 0)
									dTmp += Math.PI;
								else
									dTmp -= Math.PI;
							}
							dPhaseL = dImR[iNum] + dTmp;

							dTmp = (double)sHrtf[iNumImage] + (double)sHrtf[iNumberImage] / 2;
							dSpImageL = dReR[nImage] * dTmp / dAtt;
							dTmp = ((double)sPhase[iNumImage] + (double)sPhase[iNumberImage]) / 20000;
							if(Math.abs(sPhase[iNumImage] - sPhase[iNumberImage]) > 31415){
								if(dTmp < 0)
									dTmp += Math.PI;
								else
									dTmp -= Math.PI;
							}
							dPhaseImageL = dImR[nImage] + dTmp;
						}
					}

					else if(iAngl[0][iNum] >= 254)
					{
						dSpR = dSpL = dReR[iNum];
						dSpImageL = dSpImageR = dReR[nImage];
						dPhaseL = dPhaseR = dImR[iNum];
						dPhaseImageL = dPhaseImageR = dImR[nImage];
					}
					else
					{
						iNumb = 512 * iAngl[0][iNum] + iFreq;		// Address in database
						iNumImage = 512 * iAngl[0][iNum] + iImg;
						if(iNumImage >= DB_SIZE || iNumImage < 0)
							iNumImage = iImg;
						if(iNumb >= DB_SIZE || iNumb < 0)
							iNumb = iFreq;

						dTmp = (double)sHrtf[iNumb];				// HRTF (amplitude) value
						dSpR = dReR[iNum] * dTmp / dAtt;

						dTmp = (double)sPhase[iNumb];				// HRTF (phase) value
						dPhaseR = dImR[iNum] + dTmp / 10000;
    								
						dTmp = (double)sHrtf[iNumImage];
						dSpImageR = dReR[nImage] * dTmp / dAtt;
    								
						dTmp = (double)sPhase[iNumImage];
						dPhaseImageR = dImR[nImage] + dTmp / 10000;

						iAnglLeft = opposit(iAngl[0][iNum]);
    								
						iNumb = 512 * iAnglLeft + iFreq;
						iNumImage = 512 * iAnglLeft + iImg;
						if(iNumImage >= DB_SIZE || iNumImage < 0)
							iNumImage = iImg;
						if(iNumb >= DB_SIZE || iNumb < 0)
							iNumb = iFreq;

						dTmp = (double)sHrtf[iNumb];
						dSpL = dReR[iNum] * dTmp / dAtt;
    								
						dTmp = (double)sPhase[iNumb];
						dPhaseL = dImR[iNum] + dTmp / 10000;
    								
						dTmp = (double)sHrtf[iNumImage];
						dSpImageL = dReR[nImage] * dTmp / dAtt;

						dTmp = (double)sPhase[iNumImage];
						dPhaseImageL = dImR[nImage] + dTmp / 10000;
					}

					dReL[iNum] = dSpL * Math.cos(dPhaseL);
					dReR[iNum] = dSpR * Math.cos(dPhaseR);
					dImL[iNum] = dSpL * Math.sin(dPhaseL);
					dImR[iNum] = dSpR * Math.sin(dPhaseR);
					dReL[nImage] = dSpImageL * Math.cos(dPhaseImageL);
					dReR[nImage] = dSpImageR * Math.cos(dPhaseImageR);
					dImL[nImage] = dSpImageL * Math.sin(dPhaseImageL);
					dImR[nImage] = dSpImageR * Math.sin(dPhaseImageR);	
				}
				dReL[iHlf] = dReR[iHlf];
				dImL[iHlf] = dImR[iHlf];
				if(trans.fastFt(dReL,dImL,true))			// Inverse FFT (Left channel)
				{
					if(trans.fastFt(dReR,dImR,true))		// Inverse FFT (Right channel)
					{
						for(iNum = 0;iNum < iSize;iNum ++)
						{
							// Hann window
							dReL[iNum] *= dHann[iNum];
							dReR[iNum] *= dHann[iNum];
							// Overlap and add
							sData[0][iNum] += dReL[iNum];
							sData[1][iNum] += dReR[iNum];
						}
					}
				}
				bRet[0] = bRet[1] = 0;
				for(iNum = 0;iNum < iProc;iNum ++)
				{
					iOff = iNum * 4;
					intToByte(sData[0][iNum],bRet);
					abByte[iOff] = bRet[0];
					abByte[iOff + 1] = bRet[1];
					intToByte(sData[1][iNum],bRet);
					abByte[iOff + 2] = bRet[0];
					abByte[iOff + 3] = bRet[1];
				}
				for(iInt = 0;iInt < iSize;iInt ++)
				{
					if(iInt < iRem)
					{
						sData[0][iInt] = sData[0][iInt + iProc];
						sData[1][iInt] = sData[1][iInt + iProc];
						dReR[iInt] = abData[iInt] = abData[iInt + iProc];
					}
					else
					{
						sData[0][iInt] = sData[1][iInt] = 0;
					}
					dImR[iInt] = 0;
				}
				for(iFrmCnt = 0;iFrmCnt < nOverlap - 1;iFrmCnt ++)
				{
					for(iInt = 0;iInt < iProc * 2;iInt ++)
					{
						iAngl[iFrmCnt][iInt] = iAngl[iFrmCnt + 1][iInt];
					}
				}
				nBytesWritten += line.write(abByte,0,iProc * iBYTE * nChannels);
			}
			iOff = 0;
			for(iNum = 0;iNum < iProc;iNum ++)
			{
				if(bIsEnd)
				{
					iAngl[iLastFrm][iOff] = iAngl[iLastFrm][iOff + 1] = 254;
					dReR[iRem + iNum] = abData[iRem + iNum] = 0;
				}
				else if(iOffset >= sr.length + iRem * 4 || nBytesWritten >= sr.iLength)
				{
					iAngl[iLastFrm][iOff + 1] = iAngl[iLastFrm][iOff] = 254;
					dReR[iRem + iNum] = abData[iRem + iNum] = 0;
					bIsEnd = true;
				}
				else
				{
					if(iOffset >= sr.length)
					{
						iAngl[iLastFrm][iOff + 1] = iAngl[iLastFrm][iOff] = 254;
						dReR[iRem + iNum] = abData[iRem + iNum] = 0;
					}
					else
					{
						iAngl[iLastFrm][iOff + 1] = (int)dsByte[iOffset] & 0xff;
						iAngl[iLastFrm][iOff] = (int)dsByte[iOffset + 1] & 0xff;
						abData[iRem + iNum] = dsByte[iOffset + 3] * 256 + (dsByte[iOffset + 2] & 0x000000ff);
						dReR[iRem + iNum] = abData[iRem + iNum];
					}
				}
				iOff += 2;
				iOffset += 4;
			}
		}
		line.drain();

		line.close();

		System.out.println(nBytesWritten + " bytes reproduced");
		if(starter.isStopped)
			System.out.println("Reproduction terminated");
		else
			System.out.println("Reproduction finished");

		System.exit(0);
	}

	private static void intToByte(int iDt,byte bRet[])
	{
		bRet[0] = (byte)(iDt & 0x000000ff);
		bRet[1] = (byte)(iDt >>> 8 & 0x000000ff);
	}

	private static void printUsageAndExit()
	{
		System.out.println("sopa3d_player: usage:");
		System.out.println("\tjava sopa3d_player <SOPA file>");
		System.exit(1);
	}
	private static int opposit(int right){
		if(right == 0 || right >= 253)
			return(right);
		else if(right < 9){
			if(right == 1)
				return(right);
			else
				return(10 - right);
		}
		else if(right < 25){
			if(right == 9)
				return(right);
			else
				return(34 - right);
		}
		else if(right < 49){
			if(right == 25)
				return(right);
			else
				return(74 - right);
		}
		else if(right < 79){
			if(right == 49)
				return(right);
			else
				return(128 - right);
		}
		else if(right < 111){
			if(right == 79)
				return(right);
			else
				return(190 - right);
		}
		else if(right < 127){
			if(right == 111)
				return(right);
			else
				return(15 + right);
		}
		else if(right < 143){
			if(right == 142)
				return(right);
			else
				return(right - 15);
		}
		else if(right < 175){
			if(right == 174)
				return(right);
			else
				return(316 - right);
		}
		else if(right < 205){
			if(right == 204)
				return(right);
			else
				return(378 - right);
		}
		else if(right < 229){
			if(right == 228)
				return(right);
			else
				return(432 - right);
		}
		else if(right < 245){
			if(right == 244)
				return(right);
			else
				return(472 - right);
		}
		else{
			if(right == 252)
				return(right);
			else
				return(496 - right);
		}
	}

}
