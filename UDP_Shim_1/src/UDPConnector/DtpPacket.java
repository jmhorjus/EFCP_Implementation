/**
 * @copyright 2013 Computer Science Department, Recursive InterNetworking Architecture (RINA) laboratory, Boston University. 
 * All rights reserved. Permission to use, copy, modify, and distribute this software and its documentation
 * for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all 
 * copies and that both the copyright notice and this permission notice appear in supporting documentation. 
 * The RINA laboratory of the Computer Science Department at Boston University makes no 
 * representations about the suitability of this software for any purpose. 
 */
package UDPConnector;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
	//PDU types
	#define    PDU_TYPE_EFCP            0x80
	//DATA - DTP + user data - SDU
	#define    PDU_TYPE_DATA            0x81
	//CONTROL - DTCP - PDUs go from 0x82 to 0x8F
	#define    PDU_TYPE_CONTROL        0x82
	#define PDU_TYPE_SELECTIVE_ACK    0x84
	#define PDU_TYPE_NACK            0x86
	#define PDU_TYPE_FLOW_ONLY        0x89
	#define PDU_TYPE_ACK_ONLY        0x8C
	#define PDU_TYPE_FLOW_ACK        0x8D

	//MANAGEMENT PDUs contain CDAP
	#define    PDU_TYPE_MANAGEMENT        0xC0
	#define PDU_TYPE_IDENTIFYSENDER 0xC1    
	// used for PDU to send over TCP connection to identify sender
 **/

/**
 * This is an implementation of DTP message, which defines the header format
 * @author Yuefeng Wang and Flavio Esposito. Computer Science Department, Boston University
 *   
 */
public class DtpPacket {

	private Log log = LogFactory.getLog(DtpPacket.class);

	private short destAddr; //2 bytes
	private short srcAddr;  //2 bytes
	private short destCEPid;//2 bytes
	private short srcCEPid; //2 bytes
	private byte qosid; // 1 byte 
	private byte pdu_type; //1 bytes
	private byte flags; //1  bytes
	private int seqNum; //4
	private byte[] payload;
	private  int length;



	public DtpPacket (short destAddr, short srcAddr, short destCEPid, short srcCEPid, byte qosid, byte pdu_type, 	byte flags, int seqNum, byte[] payload)
	{
		this.destAddr = destAddr;
		this.srcAddr = srcAddr;
		this.destCEPid = destCEPid;
		this.srcCEPid = srcCEPid;
		this.qosid = qosid;
		this.pdu_type = pdu_type;
		this.flags = flags;
		this.seqNum = seqNum;
		this.payload = payload;
		this.length = 15 + this.payload.length;
	}

	public DtpPacket ( short destAddr, short srcAddr, short destCEPid, short srcCEPid, byte pdu_type, byte[] payload)
	{
		this.destAddr = destAddr;
		this.srcAddr = srcAddr;
		this.destCEPid = destCEPid;
		this.srcCEPid = srcCEPid;
		this.qosid = 1;
		this.pdu_type = pdu_type;
		this.flags = 0;
		this.seqNum = 0;
		this.payload = payload;
		this.length = 15 + this.payload.length;
	}
	
	
	public DtpPacket ( short destAddr, short srcAddr, short destCEPid, short srcCEPid, byte[] payload)
	{
		this.destAddr = destAddr;
		this.srcAddr = srcAddr;
		this.destCEPid = destCEPid;
		this.srcCEPid = srcCEPid;
		this.qosid = 1;
		this.flags = 0;
		this.seqNum = 0;
		this.payload = payload;
		this.length = 15 + this.payload.length;
	}

	public DtpPacket ( short destAddr, short srcAddr, short destCEPid, short srcCEPid, byte pdu_type)
	{
		this.destAddr = destAddr;
		this.srcAddr = srcAddr;
		this.destCEPid = destCEPid;
		this.srcCEPid = srcCEPid;
		this.qosid = 1;
		this.pdu_type = pdu_type;
		this.flags = 0;
		this.seqNum = 0;
		this.length = 15;
	}

	/**
	 * Construct a DTP message from bytes received, this will be used on the receiving side
	 * @param bytes
	 */
	public DtpPacket(byte[] bytes) {

		ByteBuffer buf = ByteBuffer.wrap(bytes, 0, 15);

		buf.order(ByteOrder.LITTLE_ENDIAN);

		this.destAddr = buf.getShort(0); //2 bytes
		this.srcAddr = buf.getShort(2);  //2 bytes
		this.destCEPid =  buf.getShort(4);//2 bytes
		this.srcCEPid = buf.getShort(6); //2 bytes
		this.qosid = buf.get(8); // 1 byte 
		this.pdu_type = buf.get(9); //1 bytes
		//	this.pdu_type = (byte) (this.pdu_type & 0xFF); // converted to 0x
		this.flags = buf.get(10); //1  bytes
		this.seqNum = buf.getInt(11); //4
		this.payload = this.getPayloadFromBytes(bytes);
		this.length = bytes.length;
	}

	/**
	 * Construct a DTP message from a CDAP message
	 * @param cdapMessage
	 */
//	public DtpPacket(CDAP.CDAPMessage cdapMessage)
//	{
//		this.destAddr = 0;
//		this.srcAddr = 0;
//		this.destCEPid = 0;
//		this.srcCEPid = 0;
//		this.qosid = 0;
//		this.pdu_type = (byte)0xC0;//CDAP
//		this.flags = 0;
//		this.seqNum = 0;
//		this.payload = cdapMessage.toByteArray();
//		this.length = 15 + this.payload.length;
//	}

	/**
	 * This one is used for BU DIF0 case
	 * In this case, address is not used, only portID(or CEPid)
	 * @param srcCEPid
	 * @param destCEPid
	 * @param msg
	 */
	public DtpPacket(short srcCEPid, short destCEPid, byte[] msg)
	{
		this.destAddr = 0;
		this.srcAddr = 0;
		this.destCEPid = destCEPid;
		this.srcCEPid = srcCEPid;
		this.qosid = 0;
		this.flags = 0;
		this.seqNum = 0;
		this.payload = msg;
		this.length = 15 + this.payload.length;
	}
	
	public byte[] toBytes()
	{
		ByteBuffer bbuf = ByteBuffer.allocate(length);
		bbuf.order(ByteOrder.LITTLE_ENDIAN);

		bbuf.putShort(this.destAddr);
		bbuf.putShort(this.srcAddr);
		bbuf.putShort(this.destCEPid);
		bbuf.putShort(this.srcCEPid);
		bbuf.put(this.qosid);
		bbuf.put(this.pdu_type);
		bbuf.put(this.flags);
		bbuf.putInt(this.seqNum);
		if(this.payload !=null)
		{
			bbuf.put(this.payload);
		}

		return bbuf.array();
	}



	private byte[] getPayloadFromBytes (byte[] msg) {

		int newLength = msg.length;

		byte[] newPayload = new byte[newLength-15]; 

		for(int i =0; i<newLength -15; i++)
		{
			newPayload[i] = msg[15+i];
		}

		return newPayload;
	}

	public void printDTPHeader()
	{
		log.debug("DTP Header: destAddr:" +   destAddr + ", srcAddr: " +   srcAddr + ", destCEPid: " +   destCEPid + ", srcCEPid: " +   srcCEPid
				+  ",qosid: " +   qosid + ", pdu_type: " +   Integer.toHexString( (byte) pdu_type & 0xFF ) + ", seqNum is " +   seqNum);

	}

	public synchronized Log getLog() {
		return log;
	}

	public synchronized void setLog(Log log) {
		this.log = log;
	}

	public synchronized short getDestAddr() {
		return destAddr;
	}

	public synchronized void setDestAddr(short destAddr) {
		this.destAddr = destAddr;
	}

	public synchronized short getSrcAddr() {
		return srcAddr;
	}

	public synchronized void setSrcAddr(short srcAddr) {
		this.srcAddr = srcAddr;
	}

	public synchronized short getDestCEPid() {
		return destCEPid;
	}

	public synchronized void setDestCEPid(short destCEPid) {
		this.destCEPid = destCEPid;
	}

	public synchronized short getSrcCEPid() {
		return srcCEPid;
	}

	public synchronized void setSrcCEPid(short srcCEPid) {
		this.srcCEPid = srcCEPid;
	}

	public synchronized byte getQosid() {
		return qosid;
	}

	public synchronized void setQosid(byte qosid) {
		this.qosid = qosid;
	}

	public synchronized byte getPdu_type() {
		return pdu_type;
	}

	public synchronized void setPdu_type(byte pdu_type) {
		this.pdu_type = pdu_type;
	}

	public synchronized byte getFlags() {
		return flags;
	}

	public synchronized void setFlags(byte flags) {
		this.flags = flags;
	}

	public synchronized int getSeqNum() {
		return seqNum;
	}

	public synchronized void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}

	public synchronized byte[] getPayload() {
		return payload;
	}

	public synchronized void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public synchronized int getLength() {
		return length;
	}

	public synchronized void setLength(int length) {
		this.length = length;
	}

}