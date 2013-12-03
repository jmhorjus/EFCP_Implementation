/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package UDPConnector;

import java.net.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author Jan
 */
public class UdpConnectionManager {
    HashMap m_connMap = new HashMap();
    int m_lastHandleValue = 0;
    
    // Returns the index of the
    public int AllocateFlow(int localPort, InetAddress destAddr)
    {
        synchronized(this)
        {
            UdpConnector newConn = new UdpConnector(localPort);
            newConn.SetPeerAddress(destAddr, localPort);
            m_connMap.put(++m_lastHandleValue, newConn);
            return m_lastHandleValue;
        }
    }
    
    public boolean DeallocateFlow(int flowHandle)
    {
        synchronized(this)
        {
            m_connMap.remove(flowHandle);
            return true;
        }
    }
    
    public boolean Send(int flowHandle, byte[] data)
    {
        UdpConnector conn = (UdpConnector)m_connMap.get(flowHandle);
        try{
            return conn.Send(data);
        }
        catch(Exception e){
            return false;
        }
        
    }
    
    public List<byte[]> Receive(int flowHandle) 
    {
        UdpConnector conn = (UdpConnector)m_connMap.get(flowHandle);
        
        try{
            return conn.Receive(0 /*no blocking*/ );
        }
        catch(Exception e) {
            return new ArrayList<byte[]>();
        }
    }
    
    
    
    
    
}
