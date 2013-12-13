/* @copyright 2013 Computer Science Department, Recursive InterNetworking Architecture (RINA) laboratory, Boston University. 
 * All rights reserved. Permission to use, copy, modify, and distribute this software and its documentation
 * for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all 
 * copies and that both the copyright notice and this permission notice appear in supporting documentation. 
 * The RINA laboratory of the Computer Science Department at Boston University makes no 
 * representations about the suitability of this software for any purpose.
 */

package UDPConnector;

import java.net.*;
import java.util.*;

/**
 * Class simulating a socket that can both send and receive.
 * Primarily uses the java DatagramSocket and DatagramPacket,
 * but holds some state in order to give the sense of a persistent
 * connection.  Also keeps its own receive buffer. 
 * @author Jan Horjus
 */
public class UdpConnector implements ConnectorInterface
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
   
    /// Pointer to an object which is notified whenever a new packet is received.
    List<ConnectorInterface.ReceiveNotifyInterface>  m_notifyOnReceive = 
            new ArrayList<>();
    
    /// The thread that listens for incoming packets.
    Thread m_receiverThread;
    DatagramSocket m_recvSocket;
    //TODO: Don't know if this is neccessary or helpful. May remove.
    boolean m_StopReceiveThread = false;
    
    /// Inner class defining a runnable thread with the receive loop.
    /// Contains the receiving DatagramSocket, and writes to the next empty 
    /// place in the receive buffer. 
    class ReceiverThreadTask implements Runnable 
    {
        boolean exceptionCaught = false;
        int m_packetsDroppedDueToBufferFull = 0;
        
        @Override
        public void run() 
        {
            m_StopReceiveThread = false;
            try 
            {
                m_recvSocket = new DatagramSocket(m_portToListenOn);
                System.out.print("ReceiverThreadTask started. Listening on " + m_portToListenOn + ".\n");
                while(!m_StopReceiveThread && !exceptionCaught)
                {
                    /// This should ensure the packet data is written to the right buffer.
                    DatagramPacket recvPacket = new DatagramPacket(
                            m_receiveBuffer[m_receiveBufferWriteIndex], 
                            m_receiveBuffer[m_receiveBufferWriteIndex].length);
                    
                    // This should be a blocking receive. 
                    m_recvSocket.receive(recvPacket);
                    
                    //System.out.print("ReceiverThreadTask: Packet received.\n");
                    synchronized(m_receiveBuffer)
                    {   
                        // Make sure there is room in the packet buffer.
                        if(m_packetsReady >= m_receiveBuffer.length)
                        {
                            // Buffer full.  Need to define behavior here.
                            // For now just silently drop the packet.
                            ++m_packetsDroppedDueToBufferFull; 
                            // Sanity check with some assertions. 
                            assert m_packetsReady == m_receiveBuffer.length;
                            assert m_receiveBufferWriteIndex == m_receiveBufferReadIndex;
                        }
                        else
                        {
                            // We have room for this packet. Increment the write index.
                            
                            //System.out.print("ReceiverThreadTask port:" + m_portToListenOn
                            //        + " buffer:" + m_receiveBufferWriteIndex 
                            //        + " data:" + new String(recvPacket.getData()) + "\n");

                            m_receiveBufferWriteIndex = 
                                    (m_receiveBufferWriteIndex+1)%m_receiveBuffer.length;
                            ++m_packetsReady;
                        }
                        // Signal the parent thread (which may be waiting to read)
                        m_receiveBuffer.notify();
                    }
                    
                    // If we have a notifyOnReceive set, then call notify and 
                    // reset the notify pointer to null.
                    synchronized(m_notifyOnReceive)
                    {
                        if(!m_notifyOnReceive.isEmpty())
                        {
                            //System.out.print("ReceiverThreadTask: Start Notify  Loop\n");
                            for(ConnectorInterface.ReceiveNotifyInterface tempNotifyPtr : m_notifyOnReceive)
                            {
                                //System.out.print("ReceiverThreadTask: Notify client of receive event.\n");
                                m_notifyOnReceive.remove(tempNotifyPtr);
                                tempNotifyPtr.Notify(UdpConnector.this);
                            }
                        }
                    }
                }
                System.out.print("ReceiverThreadTask: Receive thread terminated.\n");
            }
            catch (Exception ex)
            {
                System.out.print("***ReceiverThreadTask***: Exception caught:\"" + ex.getMessage() + "\"\n");
                exceptionCaught = true;
                // make sure the connection object knows its receive thread is no longer valid.
                UdpConnector.this.m_receiverThread = null; // 
            }
        }
    }
    

    /// Set the peer address and port.
    /// This also (re)starts the receive thread.
    @Override
    public void SetPeerAddress(
            InetAddress peerAddress, 
            int port)
    {
        if (m_isPeerSet)
            StopReceiveThread();
        
        m_peerAddress = peerAddress;
        m_peerPort = port;
        m_isPeerSet = true;
        
        StartReceiveThread();
    }
    
    /// Set the object to be notified when the next packet is received.
    /// Only one notification will be issued, then m_notifyOnReceive will be 
    /// cleared.  
    @Override 
    public boolean AddReceiveNotify(ReceiveNotifyInterface notifyMe)
    {
        //System.out.print("UdpConnector.AddReceiveNotify: Enter.\n");
        boolean retVal = false;
        synchronized(m_notifyOnReceive){         
            retVal = m_notifyOnReceive.add(notifyMe);
        }
        return retVal;
    }
    
    /// Non-blocking receive.
    /// Returns either a list of available packets or null.
    /// maxBlockingTimeInMs: may be either zero (for non-blocking receive) 
    /// or a positive number of milliseconds - the longest you are willing to 
    /// wait for at least one packet to be available.
    @Override
    public List<byte[]> Receive(int maxBlockingTimeInMs)
    {
        // Make sure receive thread is started.
        StartReceiveThread();

        
        List<byte[]> retVal = new LinkedList<>(); 
        
        synchronized(m_receiveBuffer)
        {
            // If no packets are ready, wait up to maxBlockingTime for a signal.
            if (m_packetsReady == 0 && maxBlockingTimeInMs > 0)
            {
                //System.out.print("UdpConnector.Receive: waiting on receive...\n");
                try {
                    m_receiveBuffer.wait(maxBlockingTimeInMs);
                } catch (Exception ex) { 
                    System.out.print("**ERROR** Exception waiting for next packet in Receive: \""+ex.getMessage()+"\"\n");
                }
                System.out.print("UdpConnector.Receive: Done waiting on receive packetsReady=" + m_packetsReady + "\n");
            }
            else
            {
                //System.out.print("UdpConnector.Receive: NOT waiting. Packetsready=" + m_packetsReady + "\n");
            }
        }
        
        while (m_packetsReady > 0)
        {
            synchronized(m_receiveBuffer)
            {
                //System.out.print("UdpConnector.Receive: adding:\"" + new String(m_receiveBuffer[m_receiveBufferReadIndex]) + "\"\n");
                
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
    @Override
    public boolean Send(String sendString) throws Exception
    {   
        return Send(sendString.getBytes());
    }
    
    @Override
    public boolean Send(byte[] sendBuffer) throws Exception
    {     
        //System.out.print("UdpConnection.Send: Enter.  buf="+ new String(sendBuffer) +" destPort="+ m_peerPort +".\n");
        if (!this.m_isPeerSet)
        {
            System.out.print("ERROR: Peer not set; cannot send.");
            return false;
        }
        
        DatagramSocket sendSocket = new DatagramSocket();
        DatagramPacket sendPacket = new DatagramPacket(
                sendBuffer, 
                sendBuffer.length, 
                this.m_peerAddress, 
                this.m_peerPort);
        
        sendSocket.send(sendPacket);
        
        return true;
    }
    
    
    @Override
    public void StopReceiveThread()
    {
        //TODO: Don't know if this is neccessary or helpful. May remove.
        m_StopReceiveThread = true;

        // This should cause an exception, resulting in the thread closing.
        if (m_receiverThread != null)
        { 
            m_recvSocket.close();
            m_receiverThread.interrupt(); 
            // Try and 
            try {
                m_receiverThread.join(1000);
            } catch (Exception ex) { 
                System.out.print("Exception in thread.join: "+ex.getMessage()+"\n");
            } 
            System.out.print("Stopped Receive Thread\n");
        }
    }

    
    @Override
    public void StartReceiveThread()
    {
        if(m_receiverThread == null)
        {
            m_receiverThread = new Thread(
                    this.new ReceiverThreadTask(), 
                    "Receiver Thread");
            System.out.println("Starting thread...");
            m_receiverThread.start();
            
            try {Thread.sleep(20);} catch(InterruptedException ex) {}
        }        
    }
    
}
