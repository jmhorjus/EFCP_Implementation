/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPConnector;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author jhorjus
 */
public class EfcpConnector implements ConnectorInterface
{
    /// The ConnectorInterface functions:
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
        // This is for delivering data up to...delimiting, I think. It needs 
        // to deliver a list of the buffers from the DtpPacket objects. 
        
        
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
        if (m_senderNextSequenceNumber >= m_senderRightWindowEdge)
        {
            System.out.print("Efcp Send: Cannot send; send window exhausted.\n");
            return false;
        }
        
        // Assuming we have a green light to send, do the following:
        
        // 1.) Wrap the data up in the packet.  Use a packet with DTP and EFCP fields.
        //  This means giving it a sequence number, etc. 
        DtpPacket packetToSend = new DtpPacket(
                (short)0, //short destAddr 
                (short)0, //short srcAddr
                (short)0, //short destCEPid
                (short)0, //short srcCEPid, 
                (byte)0, //byte qosid, 
                (byte)0, //byte pdu_type, 
                (byte)0, //byte flags, 
                m_senderNextSequenceNumber++, //int seqNum
                sendBuffer //byte[] payload
                ); 
        
        // 2.) Shedule retransmission.
        ScheduledFuture retransTaskHandle = s_timedTaskExecutor.scheduleAtFixedRate(
                this.new RetransmitEvent(packetToSend),
                500, 
                500, 
                TimeUnit.MILLISECONDS);
        // 3.) Put the retransTaskHandle into the retransmission queue, so the 
        //  retransmit task can be canceled when an ack is received.
        this.m_senderRetransQueue.put(packetToSend.getSeqNum(), retransTaskHandle);

        // 4.) Send it!  Yay!     
        return m_innerConnection.Send(packetToSend.toBytes());
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
    


    /// Member variables 
    ConnectorInterface m_innerConnection;
    EfcpPolicyInfo m_policyInfo;
    
    /// The thread pool executor is shared across instances. 
    static ScheduledThreadPoolExecutor s_timedTaskExecutor = 
            new ScheduledThreadPoolExecutor(8); // 8 should be plenty
    
    /// Variables related to sender state.
    Map<Integer, ScheduledFuture> m_senderRetransQueue = new HashMap<>();
    int m_senderNextSequenceNumber = 0; // initial test value
    int m_senderRightWindowEdge = 100; // initial test value
    
    /// Variables related to receiver state.
    List<EfcpPacketInfo> m_receivedPackets = new ArrayList<>();
 
    
    /// Constructor
    EfcpConnector(ConnectorInterface unreliableConnection, EfcpPolicyInfo policyInfo) 
    {
        this.m_innerConnection = unreliableConnection;
        this.m_policyInfo = policyInfo;
        
        // We need to register to be notified whenever data is available to reveive 
        // from the managed connection, so we can immediately pick it up, process the
        // headers and send an ack.  
        this.m_innerConnection.AddReceiveNotify(this.new PacketReceivedEvent());
    }
    
    /// The RetransmitEvent implements Runnable
    class RetransmitEvent implements Runnable
    {
        DtpPacket m_packetToRetransmit; 
        int m_timesRestransmitted = 0; 
        RetransmitEvent(DtpPacket packetToTransmit)
        {
            m_packetToRetransmit = packetToTransmit;
        }
        
        @Override 
        public void run()
        {
            // Just send it - most of the accounting is only done once.
            try {
                ++m_timesRestransmitted;
                m_innerConnection.Send(m_packetToRetransmit.toBytes());
            }
            catch(Exception ex) { 
                System.out.print("RetransmitEvent exception:" + ex.getMessage() + "\n"); 
            }
        }
    }
    
    
    /// PacketReceivedEvent: Inner class implements notify interface function 
    /// which is the primary receiving function.
    class PacketReceivedEvent implements ConnectorInterface.ReceiveNotifyInterface
    {
        @Override
        public void Notify(ConnectorInterface connection) 
        {
            // 1.) Call receive on the inner connection.
            List<byte[]> rawPacketsReceived = new ArrayList<>();
            try{
                rawPacketsReceived = m_innerConnection.Receive(0);
            } 
            catch(Exception ex) { System.out.print("PacketReceivedEvent: Error receiving from inner connection: " + ex.getMessage() + ".\n"); }
            
            // 2.) Loop through and process each packet buffer received.
            for (byte[] rawPacket : rawPacketsReceived)
            {
                // 1.) Deserialize the data into a packet object - so we can see its 
                // sequence number and other relevant header fields.
                DtpPacket packet = new DtpPacket(rawPacket);
            
                // 2.) Check the sequence number of the incoming packet. If it's the 
                // next expected packet, then we can immediately make it available 
                // (notify receivers above us) and send an Ack.  May do both of these 
                // things for other packets if this one filled a gap. 
                
            
                // 3.) If its an out of order packet we save it for later, but only 
                // ack if selective acks are enabled.  
            }
            
            // 3.) Add ourselves back onto the inner connections notify list, so we'll
            // be notified of future receives.  
            m_innerConnection.AddReceiveNotify(this);
        }
    }

    
}
