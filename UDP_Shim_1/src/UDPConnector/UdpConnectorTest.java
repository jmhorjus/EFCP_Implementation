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
        UdpConnector connection1 = new UdpConnector(1186);
        UdpConnector connection2 = new UdpConnector(1184);
        
        try
        {
            // start listening thread.
            System.out.println("start listening thread.");
            connection1.Receive(2000);
             
            // send a packet
            System.out.println("send a packet");
            InetAddress localhost = InetAddress.getByName("127.0.0.1");
            connection2.SetPeerAddress(localhost, 1186);
            String packet1 = "first_packet";
            System.out.print("Sending:" + packet1 + "\n"); 
            connection2.Send(packet1);      
            System.out.print("Sending:" + "gagaga" + "\n"); 
            connection2.Send("gagaga"); 
            
            //pick it up.
            System.out.println("pick it up.");
            List<byte[]> dataPacketsReceived = connection1.Receive(20000);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Received packet: ");
                System.out.print(new String(data));
                System.out.print("\n");
            }
            System.out.print("Finished.\n");
        }
        catch(Exception e)
        {
            System.out.println("Error!");
        }

    }

}
