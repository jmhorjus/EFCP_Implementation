/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPConnector;

import java.io.*;
import java.net.*;
import java.util.*;
/**
 *
 * @author jhorjus
 */
public class UdpConnectorTest {

    public static void main(String [ ] args)
    {
        //System.out.print("Sanity Test:\n");
        //String abc = "abc";
        //System.out.print("String abc:" + abc + "\n");
        //System.out.print("String abc.getBytes():" + abc.getBytes() + "\n" );
        //System.out.print("String abc.getBytes().toString():" + abc.getBytes().toString() + "\n" );
        //System.out.print("String new String(abc.getBytes()):" + new String(abc.getBytes()) + "\n\n" );
        
        
        System.out.println("UdpConnectorTest");
        
        // Part 1: Test a UdpConnector pair.
        try
        {        
            UdpConnector connection1 = new UdpConnector(1186);
            UdpConnector connection2 = new UdpConnector(1184);
            System.out.println("\n\n*** UdpConnectorTest: Section 1  ***");
            // start listening thread.
            System.out.println("start listening thread.");
            connection1.Receive(0);
             
            // send a packet
            System.out.println("send a packet");
            InetAddress localhost = InetAddress.getByName("127.0.0.1");
            connection2.SetPeerAddress(localhost, 1186);
            connection2.Send("first_packet");      
            connection2.Send("packet2"); 
            
            //pick it up.
            System.out.println("pick it up.");
            List<byte[]> dataPacketsReceived = connection1.Receive(2000);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test 1: Receive 1: ");
                System.out.print(new String(data));
                System.out.print("\n");
            }
            dataPacketsReceived = connection1.Receive(2000);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test 1: Receive 2: ");
                System.out.print(new String(data));
                System.out.print("\n");
            }
            System.out.print("Finished.\n");
        }
        catch(Exception e)
        {
            System.out.println("Test1 Error:" + e.getMessage());
        }
        
        
        
        //Part 2: Test the connection manager 
        try
        {     
            System.out.println("\n\n*** UdpConnectorTest: Section 2  ***");
            UdpConnectionManager connMgr = new UdpConnectionManager();
            int flow1 = connMgr.AllocateFlow(1181, 1183, InetAddress.getLocalHost());
            int flow2 = connMgr.AllocateFlow(1183, 1181, InetAddress.getLocalHost());

            
            for (int ii = 0; ii<20; ii++)
            {
                System.out.print("Test2 Process Send "+ ii +".\n");
                connMgr.Send(flow1, "Test2_Packet_"+ii+".");
            }
            
            List<byte[]> dataPacketsReceived = connMgr.Receive(flow2);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test2 Process Receive 1: " + new String(data) + "\n");
            }
            
            /// First receive may not have been able to pick up all packets (not yet arrived)
            try {Thread.sleep(1000);} catch(InterruptedException ex) {}
            dataPacketsReceived = connMgr.Receive(flow2);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test2 Process Receive 2: " + new String(data) + "\n");
            }
            
        }
        catch(Exception e)
        {
            System.out.println("Test2 Error:" + e.getMessage());
        }        
        

    }
    
    // Refuse traffic from other sources than our defined partner??
    // How do I interact with delimiting? Error and flow control apply only to PDU not SDU right?
    // (I can assume any buffer I'm given will always fit in one udp packet?)
    // Will I be creating/removing the PDU header or just reading/modifying it? 
    // Who allocates the space for the header?
    // What is my interface with regards to PDU headers? 
    // 
    // Where exactly will I fit into the existing code? I still don't really know.
    // What class will instantiate my class?  What are the names of any interfaces
    // in the existing code that my class should be inheriting?  
    // 
    // Draw a system diagram without my code and with my code.

}
