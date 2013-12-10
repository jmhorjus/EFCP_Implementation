/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPConnector;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jhorjus
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
        catch(Exception e){
            return false;
        }
    }
    public boolean Send(int flowHandle, String data)
    {
        ConnectorInterface conn = m_connMap.get(flowHandle);
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
        ConnectorInterface conn = m_connMap.get(flowHandle);
        
        return conn.Receive(0 /*no blocking*/ );
    }
    

}
