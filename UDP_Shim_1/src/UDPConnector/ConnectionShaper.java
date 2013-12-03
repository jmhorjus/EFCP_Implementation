/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPConnector;

import java.net.InetAddress;
import java.util.List;

/**
 *
 * @author jhorjus
 */
public class ConnectionShaper implements ConnectorInterface
{
    ConnectorInterface m_connectionToShape;
    int m_sendDelayInMs;
    int m_receiveDelayInMs;
    int packetsToDropPer10000;
    
    
    ConnectionShaper(
            ConnectorInterface connectionToShape, 
            int sendDelayInMs, 
            int receiveDelayInMs,
            int packetsToDropPer10000)
    {
        m_connectionToShape = connectionToShape;
    }
    
    
    public void SetPeerAddress(
        InetAddress peerAddress, 
        int port)
    {
        m_connectionToShape.SetPeerAddress(peerAddress, port);
    }
        
    @Override
    public List<byte[]> Receive(int maxBlockingTimeInMs) throws Exception
    {
        return m_connectionToShape.Receive(maxBlockingTimeInMs);
    }
    
    @Override
    public boolean Send(String sendString) throws Exception
    {
        return m_connectionToShape.Send(sendString);
    }
    
    @Override
    public boolean Send(byte[] sendBuffer) throws Exception
    {
        return m_connectionToShape.Send(sendBuffer);
    }
    
}
