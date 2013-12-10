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
    public int RetransmitDelayInMs = 200;
    public int AckDelayInMs = 10;
    public int MaxTimesToRetransmit = 10;
    
    
    public int ClosedWindowQueueMaxSize = 50;
    
}
