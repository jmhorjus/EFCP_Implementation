/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPConnector;

/**
 *
 * @author jhorjus
 */
public class EfcpConsts
{
	//PDU types
	public static final byte PDU_TYPE_EFCP           = (byte)0x80;
	//DATA - DTP + user data - SDU
	public static final byte PDU_TYPE_DATA           = (byte)0x81;
	//CONTROL - DTCP - PDUs go from 0x82 to 0x8F
	public static final byte PDU_TYPE_CONTROL        = (byte)0x82;
	public static final byte PDU_TYPE_SELECTIVE_ACK  = (byte)0x84;
	public static final byte PDU_TYPE_NACK           = (byte)0x86;
	public static final byte PDU_TYPE_FLOW_ONLY      = (byte)0x89;
	public static final byte PDU_TYPE_ACK_ONLY       = (byte)0x8C;
	public static final byte PDU_TYPE_FLOW_ACK       = (byte)0x8D;

	//MANAGEMENT PDUs contain CDAP
	public static final int PDU_TYPE_MANAGEMENT     = 0xC0;
	public static final int PDU_TYPE_IDENTIFYSENDER = 0xC1;
	// used for PDU to send over TCP connection to identify sender
}
       
