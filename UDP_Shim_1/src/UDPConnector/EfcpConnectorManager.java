/* @copyright 2013 Computer Science Department, Recursive InterNetworking Architecture (RINA) laboratory, Boston University. 
 * All rights reserved. Permission to use, copy, modify, and distribute this software and its documentation
 * for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all 
 * copies and that both the copyright notice and this permission notice appear in supporting documentation. 
 * The RINA laboratory of the Computer Science Department at Boston University makes no 
 * representations about the suitability of this software for any purpose.
 */
package UDPConnector;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jan Horjus
 */
public class EfcpConnectorManager {

    Map<Integer, ConnectorInterface> m_connMap = new HashMap<>();
    int m_lastHandleValue = 0;
    
    // Returns the index of the
    public int AllocateFlow(
            int localPort, 
            int destPort, 
            InetAddress destAddr,
            boolean flowHasShaper,
            boolean flowUsesEfcp
            )
    {
        synchronized(this)
        {
            ConnectorInterface newConn = new UdpConnector(localPort);
            newConn.SetPeerAddress(destAddr, destPort);
            
            if(flowHasShaper)
                newConn = new ConnectionShaper(newConn, 100, 10, 1000);
            if(flowUsesEfcp)
                newConn = new EfcpConnector(newConn, new EfcpPolicyInfo());
            
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
            ConnectorInterface conn = m_connMap.get(flowHandle);
            conn.StopReceiveThread();
            m_connMap.remove(flowHandle);
            return true;
        }
    }
    
    public boolean Send(int flowHandle, byte[] data)
    {
        ConnectorInterface conn = m_connMap.get(flowHandle);
        try{
            return conn.Send(data);
        }
        catch(Exception ex){
            System.out.print("ConnectionManager Send: Exception: " + ex.getMessage());
            return false;
        }
    }
    public boolean Send(int flowHandle, String data)
    {
        ConnectorInterface conn = m_connMap.get(flowHandle);
        try{
            return conn.Send(data);
        }
        catch(Exception ex){
            System.out.print("ConnectionManager Send: Exception: " + ex.getMessage());
            return false;
        }
    }
    
    public List<byte[]> Receive(int flowHandle, int maxBlockTimeInMs) 
    {
        ConnectorInterface conn = m_connMap.get(flowHandle);
        
        return conn.Receive(maxBlockTimeInMs /*no blocking*/ );
    }
    

}
