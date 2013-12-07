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
        return m_innerConnection.Send(sendString);
    }
    @Override
    public boolean Send(byte[] sendBuffer) throws Exception
    {
        return m_innerConnection.Send(sendBuffer);
    }
    @Override
    public void SetReceiveNotify(ConnectorInterface.ReceiveNotifyInterface notifyMe)
    {
        m_innerConnection.SetReceiveNotify(notifyMe);
    }
    @Override
    public void StopReceiveThread()
    {
        m_innerConnection.StopReceiveThread();
    }
    
    
    /// Internal state, timers
    ScheduledThreadPoolExecutor m_timedTaskExecutor;
    
    
    

    
    
}
