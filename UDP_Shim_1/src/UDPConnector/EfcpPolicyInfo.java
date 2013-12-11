/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPConnector;

/**
 *
 * @author jhorjus
 */
public class EfcpPolicyInfo {
    
    
    /// This class to be full of a ton of flags and numbers.
    /// It could probably just be a struct.
    public boolean RetransmitEnabled = true;         
    public int RetransmitDelayInMs = 300;
    public int RetransmitMaxTimes = 15;
    
    public int AckDelayInMs = 10;
    
    
    public boolean WindowFlowControlEnabled = true;
    public int WindowDefaultInitialSize = 20;

    public int ClosedWindowQueueMaxSize = 100;
    
    
    public boolean RateFlowControlEnabled = false;
    public short RateDefaultPaketsPerPeriod = 200;
    public short RateDefaultPeriodInMs = 2000;
    
}
