//	starter.java
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

import java.util.*;

class Starter extends Thread
{
	boolean isStarted = false;
	boolean isStopped = false;
	boolean isPlaying = false;

	public Starter()
	{
	}
	public void run()
	{
		String ans;
		java.util.Scanner scan;

		while(!isStarted && !isStopped)
		{
			System.out.println("Start reproduction? (Y or N)");
			scan = new java.util.Scanner(System.in);
			ans = scan.next();
			if(ans.equals("Y") || ans.equals("y")){
				isStarted = true;
				isPlaying = true;
			}
			else if(ans.equals("N") || ans.equals("n"))
				isStopped = true;
		}

		while(!isPlaying){
			if(isStopped)
				System.out.println("Cancelled");
		}

		while(isPlaying && !isStopped)
		{
			System.out.println("Stop reproduction? (Y or N)");
			scan = new java.util.Scanner(System.in);
			ans = scan.next();
			if(ans.equals("Y") || ans.equals("y"))
				isStopped = true;
		}
	}

}
