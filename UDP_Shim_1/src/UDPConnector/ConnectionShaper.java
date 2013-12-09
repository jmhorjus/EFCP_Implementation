/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPConnector;

import static java.lang.Thread.sleep;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;

/**
 *
 * @author jhorjus
 */
public class ConnectionShaper implements ConnectorInterface
{
    ConnectorInterface m_connectionToShape;
    int m_sendDelayInMs;
    int m_sendJitterInMs;
    int m_packetsToDropPer10000;
    
    
    ConnectionShaper(
            ConnectorInterface connectionToShape, 
            int sendDelayInMs, 
            int sendJitterInMs,
            int packetsToDropPer10000)
    {
        m_connectionToShape = connectionToShape;
        m_sendDelayInMs = sendDelayInMs;
        m_sendJitterInMs = sendJitterInMs;
        m_packetsToDropPer10000 = packetsToDropPer10000;
    }
    
    @Override
    public void SetPeerAddress(
        InetAddress peerAddress, 
        int port)
    {
        m_connectionToShape.SetPeerAddress(peerAddress, port);
    }
        
    @Override
    public List<byte[]> Receive(int maxBlockingTimeInMs)
    {
        return m_connectionToShape.Receive(maxBlockingTimeInMs);
    }
    
    @Override
    public boolean Send(String sendString) throws Exception
    {
        return Send(sendString.getBytes());
    }
    
    @Override
    public boolean Send(byte[] sendBuffer) throws Exception
    {
        Thread newSendThread = new Thread(
                this.new ShapedSendTask(sendBuffer));
        newSendThread.start();
        
        return true;
    }
    
    @Override
    public boolean AddReceiveNotify(ReceiveNotifyInterface notifyMe)
    { return m_connectionToShape.AddReceiveNotify(notifyMe); }
    @Override
    public void StopReceiveThread()
    { m_connectionToShape.StopReceiveThread(); }
    
    
    /// This inner class is a runnable thread that performs a delayed,
    /// uncertain send and then terminates.
    private class ShapedSendTask implements Runnable
    {
        String m_stringToSend;
        byte[] m_bufferToSend;
        boolean m_sendBuffer;
        
        ShapedSendTask(String stringToSend)
        {
            m_stringToSend = stringToSend;
            m_sendBuffer = false;
        }
        ShapedSendTask(byte[] bufferToSend)
        {
            m_bufferToSend = bufferToSend;
            m_sendBuffer = true;
        }
        
        
        @Override
        public void run()
        {
            Random r = new Random();
            int jitter = r.nextInt(m_sendJitterInMs);
            try{ 
            sleep(m_sendDelayInMs + jitter);
            }
            catch (Exception ex){
                System.out.print("Exception caught while waiting in shaped send.");
            }
            
            boolean dropPacket = (r.nextInt(10000) <= m_packetsToDropPer10000);
            
            if(!dropPacket)
            {
                try
                {
                if(m_sendBuffer)
                    m_connectionToShape.Send(m_bufferToSend);
                else
                    m_connectionToShape.Send(m_stringToSend);
                }
                catch (Exception ex){
                    System.out.print("Shaped send: Exception caught while sending." +
                            ex.getMessage());
                }
            }
        }
    }    
    
    
}
