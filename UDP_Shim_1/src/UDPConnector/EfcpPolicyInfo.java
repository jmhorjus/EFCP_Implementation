/* @copyright 2013 Computer Science Department, Recursive InterNetworking Architecture (RINA) laboratory, Boston University. 
 * All rights reserved. Permission to use, copy, modify, and distribute this software and its documentation
 * for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all 
 * copies and that both the copyright notice and this permission notice appear in supporting documentation. 
 * The RINA laboratory of the Computer Science Department at Boston University makes no 
 * representations about the suitability of this software for any purpose.
 */
package UDPConnector;

/**
 *
 * @author Jan Horjus
 */
public class EfcpPolicyInfo {
    
    
    /// This class to be full of a ton of flags and numbers.
    /// It could probably just be a struct.
    public boolean RetransmitEnabled = true;         
    public int RetransmitDelayInMs = 1000;
    public int RetransmitMaxTimes = 15;
    
    public int AckDelayInMs = 0;
    
    
    public boolean WindowFlowControlEnabled = true;
    public int WindowDefaultInitialSize = 10;

    public int ClosedWindowQueueMaxSize = 200;
    
    
    public boolean RateFlowControlEnabled = true;
    public short RateDefaultPaketsPerPeriod = 20;
    public short RateDefaultPeriodInMs = 2000;
    
}
