/* @copyright 2013 Computer Science Department, Recursive InterNetworking Architecture (RINA) laboratory, Boston University. 
 * All rights reserved. Permission to use, copy, modify, and distribute this software and its documentation
 * for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all 
 * copies and that both the copyright notice and this permission notice appear in supporting documentation. 
 * The RINA laboratory of the Computer Science Department at Boston University makes no 
 * representations about the suitability of this software for any purpose.
 */
package UDPConnector;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Jan Horjus
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
    public List<byte[]> Receive(int maxBlockingTimeInMs)
    {
        // This is for delivering data up to...delimiting, I think. It needs 
        // to deliver a list of the buffers from the DtpPacket objects.         
        List<byte[]> retVal = new ArrayList<>();
        
        synchronized(m_receiverPacketsReady)
        {   
            if (m_receiverPacketsReady.isEmpty())
            {
                System.out.print("Efcp.Receive: Nothing available; waiting.\n");
                try{
                    m_receiverPacketsReady.wait(maxBlockingTimeInMs);
                } catch (Exception ex) { System.out.print("Efcp.Receive: ERROR:"+ex.getMessage()+".\n"); }
            }
            retVal.addAll(m_receiverPacketsReady);
            m_receiverPacketsReady.clear();
        }
        
        return retVal;
    }
    @Override
    public boolean Send(String sendString) throws Exception
    {
        return Send(sendString.getBytes());
    }
    
    protected boolean IsRateBasedFlowControlWindowOpen(){
        Date now = new Date();
        if (!m_policyInfo.RateFlowControlEnabled){
            return true;
        }
        if(now.getTime() > m_senderRateCurrentPeriodStartTime.getTime() + m_policyInfo.RateDefaultPeriodInMs){
            return true;
        }
        if(m_senderSendsSoFarThisPeriod < m_policyInfo.RateDefaultPaketsPerPeriod) {
            return true;
        }
        System.out.print("Efcp: Rate based flow control engaged to close window.\n");
        return false;
    }
    
    protected void RateBasedFlowSend(){
        if (!m_policyInfo.RateFlowControlEnabled){
            return;
        }
        synchronized(m_senderRateCurrentPeriodStartTime)
        {
            Date now = new Date();
            if(now.getTime() > m_senderRateCurrentPeriodStartTime.getTime() + m_policyInfo.RateDefaultPeriodInMs)
            {

                m_senderRateCurrentPeriodStartTime = now;
                m_senderSendsSoFarThisPeriod = 0;
                System.out.print("***Efcp: Rate based flow control: new period started.\n");

                // Set an event to fire off at the end of this period.
                RatePeriodExpiredEvent event = this.new RatePeriodExpiredEvent(m_senderRateCurrentPeriodStartTime);
                s_timedTaskExecutor.schedule(
                    event, // Runnable task
                    m_policyInfo.RateDefaultPeriodInMs, // int initialDelay
                    TimeUnit.MILLISECONDS // TimeUnit 
                    );
            }
        }
        ++m_senderSendsSoFarThisPeriod;
    }
    
    @Override
    public boolean Send(byte[] sendBuffer) throws Exception
    {  
        DtcpPacket packetToSend;
        synchronized(this){
        
        // 1.) Wrap the data up in the packet.  Use a packet with DTP and EFCP fields.
        //  This means giving it a sequence number, etc. 
        packetToSend = new DtcpPacket(
                (short)0, //short destAddr 
                (short)0, //short srcAddr
                (short)0, //short destCEPid
                (short)0, //short srcCEPid, 
                (byte)0, //byte qosid, 
                EfcpConsts.PDU_TYPE_DATA, //byte pdu_type, 
                (byte)0, //byte flags, 
                m_senderNextSequenceNumber++, //int seqNum
                sendBuffer //byte[] payload
                ); 
        } 
        return SendPacket(packetToSend);
    }
        
    boolean SendPacket(DtcpPacket packetToSend) throws Exception
    { synchronized (this){
        
        System.out.print("Efcp:   SendPacket: Enter. \n");
        // Check the window - do we have room or "credit" to send this packet?
        if ((m_policyInfo.WindowFlowControlEnabled &&
            packetToSend.getSeqNum() >= m_senderRightWindowEdge) ||
            (!this.IsRateBasedFlowControlWindowOpen()))
        {
            if(m_senderClosedWindowQueue.size() >= m_policyInfo.ClosedWindowQueueMaxSize)
            {
                System.out.print("Efcp Send: Cannot send; send window exhausted and backup queue full.\n");
                return false;
            }
            
            m_senderClosedWindowQueue.add(packetToSend);
            System.out.print("Efcp Send: Send window closed: waiting packets="
                    + m_senderClosedWindowQueue.size() 
                    + " Buffer="+new String(packetToSend.getPayload())+".\n");
            return true;
        }
        
        // Assuming we have a green light to send, do the following:
        
    
        // 2.) Shedule retransmission.
        RetransmitEvent retransmitEvent = this.new RetransmitEvent(packetToSend);
        ScheduledFuture retransTaskHandle = s_timedTaskExecutor.scheduleAtFixedRate(
                retransmitEvent, // Runnable task
                m_policyInfo.RetransmitDelayInMs, // int initialDelay
                m_policyInfo.RetransmitDelayInMs, // int period
                TimeUnit.MILLISECONDS // TimeUnit 
                );
        retransmitEvent.SetSelfCancelHandle(retransTaskHandle);
        
        // 3.) Put the retransTaskHandle into the retransmission queue, so the 
        //  retransmit task can be canceled when an ack is received.
        m_senderRetransmitQueue.put(packetToSend.getSeqNum(), retransTaskHandle);

        // 4.) Update rate based flow comtrol.
        RateBasedFlowSend();
        
        // 5.) Send it!  Yay!     
        return m_innerConnection.Send(packetToSend.toBytes());
    }}
    @Override
    public boolean AddReceiveNotify(ConnectorInterface.ReceiveNotifyInterface notifyMe)
    {
        return m_innerConnection.AddReceiveNotify(notifyMe);
    }
    @Override
    public void StopReceiveThread()
    { m_innerConnection.StopReceiveThread();    }
    @Override
    public void StartReceiveThread()
    { m_innerConnection.StartReceiveThread(); }



    /// Member variables 
    ConnectorInterface m_innerConnection;
    EfcpPolicyInfo m_policyInfo;
    
    /// The thread pool executor is shared across instances. 
    static ScheduledThreadPoolExecutor s_timedTaskExecutor = 
            new ScheduledThreadPoolExecutor(8); // 8 should be plenty
    
    /// Variables related to sender state.
    Map<Integer, ScheduledFuture> m_senderRetransmitQueue = new HashMap<>();
    Queue<DtcpPacket> m_senderClosedWindowQueue = new LinkedList<>();
    Date m_senderRateCurrentPeriodStartTime = new Date(0);
    int m_senderSendsSoFarThisPeriod = 0;
    int m_senderLastPacketAcked = -1;
    int m_senderNextSequenceNumber = 0; // initial test value
    int m_senderRightWindowEdge = 0; // initial test value
    
    /// Variables related to receiver state.
    final List<byte[]> m_receiverPacketsReady = new ArrayList<>();
    Map<Integer, byte[]> m_receiverPacketsOutOfOrder = new HashMap<>();
    int m_receiverNextPacketToDeliver = 0; // initial test value
    int m_receiverWindowSize = 0; // initial test value
 
    
    /// Constructor
    EfcpConnector(ConnectorInterface unreliableConnection, EfcpPolicyInfo policyInfo) 
    {
        m_innerConnection = unreliableConnection;
        
        m_policyInfo = policyInfo;
        m_senderRightWindowEdge = m_policyInfo.WindowDefaultInitialSize;
        m_receiverWindowSize = m_policyInfo.WindowDefaultInitialSize;
        
        // We need to register to be notified whenever data is available to reveive 
        // from the managed connection, so we can immediately pick it up, process the
        // headers and send an ack.  
        m_innerConnection.AddReceiveNotify(this.new PacketReceivedEvent());
        
        // Efcp needs to be able to receive acks. No good talking without listening.
        // Doing this in the constructor prevents errors in common use.
        StartReceiveThread();
    }
    
    /// The RetransmitEvent implements Runnable
    class RetransmitEvent implements Runnable
    {
        DtcpPacket m_packetToRetransmit; 
        int m_timesRestransmitted = 0; 
        RetransmitEvent(DtcpPacket packetToTransmit)
        {
            m_packetToRetransmit = packetToTransmit;
        }
        
        ScheduledFuture m_selfCancelHandle;
        void SetSelfCancelHandle(ScheduledFuture handle)
        {
            m_selfCancelHandle = handle;
        }
        
        @Override 
        public void run()
        {
            // Just send it - most of the accounting is only done once.
            // The exception is rate based flow control, which we still need to
            // look out for...            
            try {
                if(++m_timesRestransmitted > m_policyInfo.RetransmitMaxTimes){
                    throw new Exception("Failed to deliver packet "
                            +m_packetToRetransmit.getSeqNum()+" after "
                            +m_policyInfo.RetransmitMaxTimes+" tries!");
                }
                RateBasedFlowSend();
                m_innerConnection.Send(m_packetToRetransmit.toBytes());
            }
            catch(Exception ex) { 
                System.out.print("RetransmitEvent exception:" + ex.getMessage() + "\n"); 
                m_selfCancelHandle.cancel(false);
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
            //System.out.print("Efcp:PacketReceivedEvent: Enter.\n");
            
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
                DtcpPacket packet = new DtcpPacket(rawPacket);
            
                // 2.) Check the sequence number of the incoming packet. If it's the 
                // next expected packet, then we can immediately make it available 
                // (notify receivers above us) and send an Ack.  May do both of these 
                // things for other packets if this one filled a gap.
                switch(packet.getPdu_type())
                {
                    case EfcpConsts.PDU_TYPE_DATA:
                    {
                        if (packet.getSeqNum() == m_receiverNextPacketToDeliver)
                        {
                            synchronized(m_receiverPacketsReady)
                            {
                                System.out.print("Efcp received packet " + packet.getSeqNum() + " in order!\n");
                                m_receiverPacketsReady.add(packet.getPayload());
                                ++m_receiverNextPacketToDeliver;

                                // Check the out-of-order packets for any next packets.
                                while (m_receiverPacketsOutOfOrder.containsKey(m_receiverNextPacketToDeliver))
                                {
                                    // Move the packet from the out of order list to the ready list
                                    // and increment m_receiverNextPacketToDeliver.
                                    System.out.print("Efcp returning packet " + m_receiverNextPacketToDeliver + " after filled gap!\n");
                                    m_receiverPacketsReady.add(
                                            m_receiverPacketsOutOfOrder.get(m_receiverNextPacketToDeliver));
                                    m_receiverPacketsOutOfOrder.remove(m_receiverNextPacketToDeliver);
                                    
                                    ++m_receiverNextPacketToDeliver;
                                }
                                
                                // Wake up any waiting receive.
                                m_receiverPacketsReady.notify();
                            }

                            SendAck();
                        }
                        // 3.) If its an out of order packet we save it for later, but only 
                        // ack if selective acks are enabled. We may nack the next expected
                        // packet in this case also, depending on policy.
                        else if (packet.getSeqNum() < m_receiverNextPacketToDeliver)
                        { // We already have this packet; resend the 
                            System.out.print("Received duplicate packet "+packet.getSeqNum()+":"+new String(packet.getPayload())+"\n");
                            SendAck();
                        }
                        else // packet.getSeqNum() > m_receiverNextPacketToDeliver, so there's a gap.
                        {
                            // Don't add duplicates to the out-of-order packets list.
                            if (!m_receiverPacketsOutOfOrder.containsKey(packet.getSeqNum()))
                            {
                                m_receiverPacketsOutOfOrder.put(packet.getSeqNum(), packet.getPayload());
                            }
                            //TODO: Sellective ack logic and nack logic.
                        }
                        break;
                    }
                    case EfcpConsts.PDU_TYPE_ACK_ONLY:
                    {
                        ReceiveAck(packet.AckSeqNum);
                        break;
                    }
                    case EfcpConsts.PDU_TYPE_FLOW_ACK:
                    {
                        ReceiveAck(packet.AckSeqNum);
                        ReceiveFlowControl(packet);
                        break;
                    }
                    case EfcpConsts.PDU_TYPE_FLOW_ONLY:
                    {
                        ReceiveFlowControl(packet);
                        break;
                    }     
                }
            }
            
            // 3.) Add ourselves back onto the inner connections notify list, so we'll
            // be notified of future receives.  
            m_innerConnection.AddReceiveNotify(this);
        }
        
        
        void SendAck()
        {
            byte pduType;
            if (m_policyInfo.WindowFlowControlEnabled || m_policyInfo.RateFlowControlEnabled)
                pduType = m_policyInfo.RetransmitEnabled ? 
                        EfcpConsts.PDU_TYPE_FLOW_ACK : EfcpConsts.PDU_TYPE_FLOW_ONLY;
            else
                pduType = m_policyInfo.RetransmitEnabled ? 
                        EfcpConsts.PDU_TYPE_ACK_ONLY : EfcpConsts.PDU_TYPE_CONTROL;
            
            // Send an ack back to the sender.
            DtcpPacket ackToSend = new DtcpPacket(
                    (short)0, //short destAddr 
                    (short)0, //short srcAddr
                    (short)0, //short destCEPid
                    (short)0, //short srcCEPid
                    (byte)0,  //byte qosid
                    pduType,  //byte pdu_type
                    (byte)0,  //byte flags
                    (int)0,   //int seqNum - control packets not yet using this.
                    "CTRL".getBytes() //byte[] payload
                    ); 
            
            // Set the actual control data fields in the packet.
            if (m_policyInfo.RetransmitEnabled)
                ackToSend.AckSeqNum = m_receiverNextPacketToDeliver-1; //int seqNum
            if (m_policyInfo.WindowFlowControlEnabled)
                ackToSend.NewRightWindowEdge = m_receiverNextPacketToDeliver + m_policyInfo.WindowDefaultInitialSize;
            if (m_policyInfo.RateFlowControlEnabled) {
                ackToSend.NewDataPeriodInMs = m_policyInfo.RateDefaultPeriodInMs;
                ackToSend.NewDataRate = m_policyInfo.RateDefaultPaketsPerPeriod;
            }
            

            try {
                m_innerConnection.Send(ackToSend.toBytes());
                System.out.print("Efcp Sending Ack:" + (m_receiverNextPacketToDeliver-1) + "\n"); 
            }
            catch(Exception ex) { 
                System.out.print("Exception Sending Ack:" + ex.getMessage() + "\n"); 
            }
        }
        
        
        void ReceiveAck(int sequenceNum)
        {
            System.out.print("Efcp: Ack Received: seq="+sequenceNum+"\n");
            if (m_senderLastPacketAcked >= sequenceNum) {
                System.out.print("Ignoring redundant Ack. lastAck="+m_senderLastPacketAcked+"\n");
                return;
            }
            //1.) Cancel all retransmission events for packets with 
            // sequence numbers <= the acked sequence number.
            while(m_senderLastPacketAcked < sequenceNum)
            {
                ScheduledFuture task = m_senderRetransmitQueue.get(++m_senderLastPacketAcked);
                task.cancel(false);
            }
        }
        
        
        void ReceiveFlowControl(DtcpPacket packet)
        {
            if (m_senderRightWindowEdge < packet.NewRightWindowEdge) {
                System.out.print("Efcp: Flow control received. right window edge from "
                        + m_senderRightWindowEdge + " to " + packet.NewRightWindowEdge + ".\n");
                // Set the new right window edge.
                m_senderRightWindowEdge = packet.NewRightWindowEdge;
            
                // Rate based control:
                if(m_policyInfo.RateFlowControlEnabled){
                    m_policyInfo.RateDefaultPeriodInMs = packet.NewDataPeriodInMs;
                    m_policyInfo.RateDefaultPaketsPerPeriod = packet.NewDataRate;
                }
                    
            }
            
            //2.) If there are packet waiting on the closed window queue, 
            // immediately send as many of them as the new window allows.
            while(!m_senderClosedWindowQueue.isEmpty() &&
                    m_senderRightWindowEdge > m_senderClosedWindowQueue.peek().getSeqNum() &&
                    IsRateBasedFlowControlWindowOpen())
            {
                try { SendPacket(m_senderClosedWindowQueue.remove()); }
                catch(Exception ex) { System.out.print("Exception Sending: "+ex.getMessage()+"\n"); }
            }
            
        }
        
    }

    class RatePeriodExpiredEvent implements Runnable 
    {
        Date m_scheduleTime;
        RatePeriodExpiredEvent(Date now)
        {
            m_scheduleTime = now;
        }
        
        @Override
        public void run()
        {
            System.out.print("***Efcp: Rate based flow control period expired. Packets Waiting="
                   + m_senderClosedWindowQueue.size() 
                   + " windowEdge=" + m_senderRightWindowEdge+"\n");
            
            //1.) Reset the rate period data.
            synchronized(m_senderRateCurrentPeriodStartTime)
            {
                if(m_senderRateCurrentPeriodStartTime == m_scheduleTime)
                {
                    if(!m_senderClosedWindowQueue.isEmpty())
                    {
                        m_senderSendsSoFarThisPeriod = 0;
                        m_senderRateCurrentPeriodStartTime = new Date();
                        m_scheduleTime = m_senderRateCurrentPeriodStartTime;

                        s_timedTaskExecutor.schedule(
                            this, // Runnable task 
                            m_policyInfo.RateDefaultPeriodInMs, // MAY HAVE CHANGED
                            TimeUnit.MILLISECONDS // TimeUnit 
                            );
                    }
                    else
                    {
                        // If this function has executed, the period MUST be expired or reset.
                        m_senderRateCurrentPeriodStartTime = new Date(0);
                    }
                }
            }
            
            //2.) If there are packet waiting on the closed window queue, 
            // immediately send as many of them as the new window allows.
            while(!m_senderClosedWindowQueue.isEmpty() &&
                    m_senderRightWindowEdge > m_senderClosedWindowQueue.peek().getSeqNum() &&
                    IsRateBasedFlowControlWindowOpen())
            {
                try { SendPacket(m_senderClosedWindowQueue.remove()); }
                catch(Exception ex) { System.out.print("Exception Sending: "+ex.getMessage()+"\n"); }
            }
        }
    }
    
    
}
