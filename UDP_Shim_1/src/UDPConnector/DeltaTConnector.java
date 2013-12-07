/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPConnector;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 *
 * @author jhorjus
 */
public class DeltaTConnector implements ConnectorInterface
{
    ConnectorInterface m_innerConnection;

    
    @Override
    public void SetPeerAddress(
            InetAddress peerAddress, 
            int port)
    {
        m_innerConnection.SetPeerAddress(peerAddress, port);
    }
    @Override
    public List<byte[]> Receive(int maxBlockingTimeInMs) throws Exception
    {
        return m_innerConnection.Receive(maxBlockingTimeInMs);
    }
    @Override
    public boolean Send(String sendString) throws Exception
    {
        return Send(sendString.getBytes());
    }
    @Override
    public boolean Send(byte[] sendBuffer) throws Exception
    {
        // Check the window - do we have room or "credit" to send this packet?
        
        // 1.) Wrap the data up in the packet.  Use a packet with DTP and EFCP fields.
        //  This means giving it a sequence number, etc. 
        
        // 2.) Put a copy into the retransmission queue.
        
        // 3.) Start the retransmission timer.

        // 4.) Send it!  Yay!     
        return m_innerConnection.Send(sendBuffer);
    }
    @Override
    public boolean AddReceiveNotify(ConnectorInterface.ReceiveNotifyInterface notifyMe)
    {
        return m_innerConnection.AddReceiveNotify(notifyMe);
    }
    @Override
    public void StopReceiveThread()
    {
        m_innerConnection.StopReceiveThread();
    }
    
    
    /// Internal state, timers
    ScheduledThreadPoolExecutor m_timedTaskExecutor;
    
    
    

    
    
}
