/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPConnector;

import java.util.concurrent.ScheduledFuture;

/**
 *
 * @author jhorjus
 */
public class EfcpPacketInfo 
{
    /// The packet object - which serializes and deserializes itself.
    public DtpPacket m_dtpPacket;
    
    /// The retransmission task - sceduled to happen periodically, but is 
    /// hopefully soon canceled when an ack is received.  
    public ScheduledFuture m_retransmitTask;   
}
