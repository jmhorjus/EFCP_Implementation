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
public interface ConnectorInterface {
    public void SetPeerAddress(
            InetAddress peerAddress, 
            int port);
    public List<byte[]> Receive(int maxBlockingTimeInMs);
    public boolean Send(String sendString) throws Exception;
    public boolean Send(byte[] sendBuffer) throws Exception;
    
    public interface ReceiveNotifyInterface {
        public void Notify(ConnectorInterface connection);
    }
    public boolean AddReceiveNotify(ReceiveNotifyInterface notifyMe);
    
    public void StartReceiveThread();
    public void StopReceiveThread();
}
