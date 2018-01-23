/*
 * macTxTimer.java
 *
 * Created on July 9, 2008, 4:04 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package sidnet.stack.std.mac.ieee802_15_4;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.mac.MacAddress;

/**
 *
 * @author Oliver
 * Java adaptation after NS-2 C++ implementation
 */
/*
 * Copyright (c) 2003-2004 Samsung Advanced Institute of Technology and
 * The City University of New York. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *	This product includes software developed by the Joint Lab of Samsung 
 *      Advanced Institute of Technology and The City University of New York.
 * 4. Neither the name of Samsung Advanced Institute of Technology nor of 
 *    The City University of New York may be used to endorse or promote 
 *    products derived from this software without specific prior written 
 *    permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE JOINT LAB OF SAMSUNG ADVANCED INSTITUTE
 * OF TECHNOLOGY AND THE CITY UNIVERSITY OF NEW YORK ``AS IS'' AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN 
 * NO EVENT SHALL SAMSUNG ADVANCED INSTITUTE OR THE CITY UNIVERSITY OF NEW YORK 
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class macTxTimer implements TimerInterface802_15_4
{
    protected boolean started;
    protected boolean busy;
    protected boolean paused;
    protected boolean canceled;
    protected double stime = 0.0;		//start time
    protected double wtime = 0.0;		//waiting time
    protected double lastMomentOfPause = 0.0;
    protected double totalPauseTime = 0.0;
    protected int id; // for debugging purposes only
    protected long timerSequence = 0;
    
    // PROXIES -----------------------------------------
    
    private Mac802_15_4	macEntity;
    private TimerInterface802_15_4 self;
     
    public macTxTimer(Mac802_15_4 macEntity, int id) // macTxTimer(Mac802_15_4Impl *m) 
    {
        if(!JistAPI.isEntity(macEntity)) throw new IllegalArgumentException("expected entity");
        this.macEntity = macEntity;
        // Dominic Lerbs: 2010-08-25. This can create Java Heap problems since lots of macTxTimers are created
        //self = (TimerInterface802_15_4)JistAPI.proxy(this, TimerInterface802_15_4.class);
        self = this; // changing to this does not change simulation behavior, but solves the memory problem and boosts speed
        
        this.id = id;
    }
    
    public void	timeout(long currentTimerSequence)
    {
        if (currentTimerSequence == timerSequence)
            if (!canceled && !paused)
            {
                if (Def.DEBUG802_15_4_timer && ( Def.DEBUG802_15_4_nodeid==(MacAddress.NULL) || Def.DEBUG802_15_4_nodeid.equals(id) ))
                    System.out.println("[" + JistAPI.getTime() + "][" + id + "][macTxTimer.timeout()]");     
                resetTimer();
                // Rey: only call the handler if the timer hasn't been canceled in the meanwhile
                if (!canceled)
                        macEntity.txHandler();
                canceled = false;   
            }
    }
    
    
     // ------------------------------------------
    
    public void resetTimer()
    {
        busy       = false;
        paused     = false;
        stime = 0;
        wtime = 0;
        timerSequence++;
    }
    
    public void startTimer(double wtime, MacMessage_802_15_4 p)
    {
        // DO NOT IMPLEMENT
    }
    
    public void start()
    {
        // DO NOT IMPLEMENT
    }
      
    public void	start(double time, boolean onlycap)
    {
        // DO NOT IMPLEMENT> Only for macExtractTimer
    }
   
     public void	start(boolean reset, boolean fortx, double wt)
    {
        // DO NOT IMPLEMENT> Only for macBcnTxTimer
    }
    
    /**
     * Start timer
     */
    public void startTimer(double time)
    {
        assert(!busy && !started);
	busy = true;
	//Rey
	//canceled = false;
	stime = ((double)JistAPI.getTime())/Constants.SECOND;
	wtime = time;
	assert(wtime >= 0);
	//s.schedule(this, /* & */event, wtime);
        if (Def.DEBUG802_15_4_timer && ( Def.DEBUG802_15_4_nodeid==(MacAddress.NULL) || Def.DEBUG802_15_4_nodeid.equals(id) ))
        {
            System.out.println("wtime = " + wtime);
            System.out.println("Constants.SECOND = " + Constants.SECOND);
            System.out.println("wtime * Constants.SECOND = " + (wtime * Constants.SECOND));
            System.out.println("[" + JistAPI.getTime() + "][" + id + "][macTxTimer.startTimer()]. Timer scheduled to expire at " + (long)(JistAPI.getTime() + wtime * Constants.SECOND) + " (wtime = " + wtime);
        }
        // Rey: adjusted to proper timing
        //JistAPI.sleepBlock((long)(wtime * 1000 * 10000 * 100 * Constants.SECOND)); // ????
        timerSequence++;
        long currentTimerSequence = timerSequence;
        JistAPI.sleep((long)(wtime * Constants.SECOND)); // ????
        self.timeout(currentTimerSequence);
    }
    
    /**
     * Cancel timer
     */ 
    public void cancel()
    {
    	//assert(started);   commented on 2010-03-15
        canceled = true;
    }
    
    public boolean canceled() throws JistAPI.Continuation
    {
        return canceled;
    }
    
    public boolean bussy() throws JistAPI.Continuation
    {
        return busy;
    }
    
    public boolean paused() throws JistAPI.Continuation
    {
        return paused;
    }
    
    private double expire()
    {
        return ((double)JistAPI.getTime())/Constants.SECOND - stime - totalPauseTime > wtime ? 0 : wtime - (((double)JistAPI.getTime())/Constants.SECOND - stime - totalPauseTime);
    }
   
    /** 
     * Pause timer
     */
    public void pause()
    {
        assert(started && !canceled && !paused);
        paused = true;
        lastMomentOfPause = ((double)JistAPI.getTime())/Constants.SECOND;
    }
    
    /** 
     * Resume timer
     */
    public void resume()
    {
        assert(started &&  !canceled);
        totalPauseTime += ((double)JistAPI.getTime())/Constants.SECOND - lastMomentOfPause;
        paused = false;
        // sleep for the remaining amount of time in the timer
        long currentTimerSequence = timerSequence;
        JistAPI.sleepBlock((long)(expire() * Constants.SECOND));       
        self.timeout(currentTimerSequence);
    }
    
    public void stopTimerr()
    {
         cancel();
	 resetTimer();
    }
    
     public TimerInterface802_15_4 getProxy()
    {
        return self;
    }
}