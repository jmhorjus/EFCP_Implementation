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
 *
 * @author Jan Horjus
 */
public class UdpConnectionManager 
{
    Map<Integer, UdpConnector> m_connMap = new HashMap<>();
    int m_lastHandleValue = 0;
    
    // Returns the index of the
    public int AllocateFlow(int localPort, int destPort, InetAddress destAddr)
    {
        synchronized(this)
        {
            UdpConnector newConn = new UdpConnector(localPort);
            newConn.SetPeerAddress(destAddr, destPort);
            newConn.StartReceiveThread(); 
            m_connMap.put(++m_lastHandleValue, newConn);
            System.out.print("Endpoint Allocated: Handle:"+m_lastHandleValue+" Listening port:"+localPort+"\n");
            return m_lastHandleValue;
        }
    }
    
    public boolean DeallocateFlow(int flowHandle)
    {
        synchronized(this)
        {
            UdpConnector conn = m_connMap.get(flowHandle);
            conn.StopReceiveThread();
            m_connMap.remove(flowHandle);
            return true;
        }
    }
    
    public boolean Send(int flowHandle, byte[] data)
    {
        UdpConnector conn = m_connMap.get(flowHandle);
        try{
            return conn.Send(data);
        }
        catch(Exception e){
            return false;
        }
    }
    public boolean Send(int flowHandle, String data)
    {
        UdpConnector conn = m_connMap.get(flowHandle);
        try{
            return conn.Send(data);
        }
        catch(Exception e){
            System.out.print("ConnectionManager Send: Exception: " + e.getMessage());
            return false;
        }
    }
    
    public List<byte[]> Receive(int flowHandle) 
    {
        UdpConnector conn = m_connMap.get(flowHandle);
        
        return conn.Receive(0 /*no blocking*/ );
    }
    
}
