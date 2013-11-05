package UDPConnector;

import java.net.*;
import java.util.*;
import java.io.*;

/**
 * Class simulating a socket that can both send and receive.
 * Primarily uses the java DatagramSocket and DatagramPacket,
 * but holds some state in order to give the sense of a persistent
 * connection.  Also keeps its own receive buffer. 
 * @author Jan Horjus
 */
public class UdpConnector 
{
    public UdpConnector( 
            int portToListenOn
            )
    {
        m_isPeerSet = false;
        // TODO: This could probably be a lot more memory efficient.
        m_receiveBuffer = new byte[128][2048];
        m_receiveBufferReadIndex = 0;
        m_receiveBufferWriteIndex = 0;
        m_packetsReady = 0 ; 
        m_portToListenOn = portToListenOn;
        m_receiverThread = null;
    }
    
    public UdpConnector( 
            int portToListenOn,
            int maxReceivePacketSize, 
            int maxReceivePacketsToBuffer
            )
    {
        m_isPeerSet = false;
        // TODO: This could probably be a lot more memory efficient.
        m_receiveBuffer = new byte[maxReceivePacketsToBuffer][maxReceivePacketSize];
        m_receiveBufferReadIndex = 0;
        m_receiveBufferWriteIndex = 0;
        m_packetsReady = 0 ;
        m_portToListenOn = portToListenOn;
        m_receiverThread = null;
    }
    
    
    /// Has the peer been identified? 
    /// (explicitly set or identified via receiving a datagram)
    /// This pust be true in order to send data. 
    boolean m_isPeerSet;
    
    /// The peer ip address/port. This is where we send to. 
    InetAddress m_peerAddress;
    int m_peerPort;
    /// The port to listen on when receiving.
    int m_portToListenOn;
    
    /// The receive buffer vector. The is where data stays while it waits 
    /// for receive to be called. 
    final byte[][] m_receiveBuffer;  
    int m_receiveBufferReadIndex;
    int m_receiveBufferWriteIndex;
    int m_packetsReady;
    
    
    Thread m_receiverThread;
    
    /// Inner class defining a runnable thread with the receive loop.
    /// Contains the receiving DatagramSocket, and writes to the next empty 
    /// place in the receive buffer. 
    class ReceiverThreadTask implements Runnable 
    {
        public boolean AbortThread = false;
        int m_packetsDroppedDueToBufferFull = 0;
        
        @Override
        public void run() 
        {

            AbortThread = false;
            try 
            {
                DatagramSocket recvSocket = new DatagramSocket(m_portToListenOn);
                System.out.print("ReceiverThreadTask started. Listening on ");
                System.out.print(m_portToListenOn);
                System.out.print(".\n");
                while(!AbortThread)
                {
                    /// This should ensure the packet data is written to the right buffer.
                    DatagramPacket recvPacket = new DatagramPacket(
                            m_receiveBuffer[m_receiveBufferWriteIndex], 
                            m_receiveBuffer[m_receiveBufferWriteIndex].length);
                    
                    // This should be a blocking receive. 
                    recvSocket.receive(recvPacket);
                    
                    System.out.print("ReceiverThreadTask: Packet received.\n");
                    synchronized(m_receiveBuffer)
                    {   
                        // Make sure there is room in the packet buffer.
                        if(m_packetsReady >= m_receiveBuffer.length)
                        {
                            // Buffer full.  Need to define behavior here.
                            // For now just silently drop the packet.
                            ++m_packetsDroppedDueToBufferFull; 
                            ++m_packetsReady;
                            // Sanity check with some assertions. 
                            assert m_packetsReady == m_receiveBuffer.length;
                            assert m_receiveBufferWriteIndex == m_receiveBufferReadIndex;
                        }
                        else
                        {
                            // We have room for this packet. Increment the
                            // write index.
                            System.out.print("ReceiverThreadTask: buffer: ");
                            System.out.print(m_receiveBufferWriteIndex + " data: ");
                            System.out.print(new String(recvPacket.getData()) + "\n");
                            m_receiveBufferWriteIndex = 
                                    (m_receiveBufferWriteIndex+1)%m_receiveBuffer.length;
                        }
                        // Signal the parent thread (which may be waiting to read)
                        m_receiveBuffer.notify();
                    }
                }
                
                System.out.print("ReceiverThreadTask: Receive thread terminated.\n");
            }
            catch (Exception ex)
            {
                System.out.print("ReceiverThreadTask: Exception caught!\n");
                AbortThread = true;
            }
        }
    }
    

    /// Set the peer address and port 
    public void SetPeerAddress(
            InetAddress peerAddress, 
            int port)
    {
        m_peerAddress = peerAddress;
        m_peerPort = port;
        m_isPeerSet = true;
    }
    
    /// Non-blocking receive.
    /// Returns either a list of available packets or null.
    /// maxBlockingTimeInMs: may be either zero (for non-blocking receive) 
    /// or a positive number of milliseconds - the longest you are willing to 
    /// wait for at least one packet to be available.
    public List<byte[]> Receive(int maxBlockingTimeInMs) throws Exception
    {
        if(m_receiverThread == null)
        {
            m_receiverThread = new Thread(
                    this.new ReceiverThreadTask(), 
                    "Receiver Thread");
            System.out.println("Starting thread...");
            m_receiverThread.start();
            System.out.println("Thread started...");
        }
        
        List<byte[]> retVal = new LinkedList<byte[]>(); 
        
        synchronized(m_receiveBuffer)
        {
            // If no packets are ready, wait up to maxBlockingTime for a signal.
            if (m_packetsReady == 0 && maxBlockingTimeInMs > 0)
            {
                m_receiveBuffer.wait(maxBlockingTimeInMs);
            }
        }
        
        while (m_packetsReady > 0)
        {
            synchronized(m_receiveBuffer)
            {
                retVal.add(m_receiveBuffer[m_receiveBufferReadIndex]);
                
                m_receiveBufferReadIndex = 
                        (m_receiveBufferReadIndex+1)%m_receiveBuffer.length;   
                --m_packetsReady;
            }
        }
        return retVal;
    }
    
    
    /// Sending is much easier than Receiving!
    /// Just send it and don't care!
    public boolean Send(String sendString) throws Exception
    {   
        return Send(sendString.getBytes());
    }
    
    public boolean Send(byte[] sendBuffer) throws Exception
    {     
        if (!this.m_isPeerSet)
            return false;
        
        DatagramSocket sendSocket = new DatagramSocket();
        DatagramPacket sendPacket = new DatagramPacket(
                sendBuffer, 
                sendBuffer.length, 
                this.m_peerAddress, 
                this.m_peerPort);
        
        sendSocket.send(sendPacket);
        
        return true;
    }
}
