/* @copyright 2013 Computer Science Department, Recursive InterNetworking Architecture (RINA) laboratory, Boston University. 
 * All rights reserved. Permission to use, copy, modify, and distribute this software and its documentation
 * for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all 
 * copies and that both the copyright notice and this permission notice appear in supporting documentation. 
 * The RINA laboratory of the Computer Science Department at Boston University makes no 
 * representations about the suitability of this software for any purpose.
 */
package UDPConnector;

import java.net.InetAddress;
import java.util.List;

/**
 *
 * @author Jan Horjus
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
