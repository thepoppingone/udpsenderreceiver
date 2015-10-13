import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;
import java.io.*;

public class FileReceiver {
	
	public static void main(String[] args) throws Exception 
	{
		if (args.length != 1) {
			System.err.println("Usage: SimpleUDPReceiver <port>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		DatagramSocket sk = new DatagramSocket(port);
		byte[] data = new byte[1000];
		byte[] dataToSender = new byte[200];
		byte[] buffer = new byte[500];
		DatagramPacket pkt = new DatagramPacket(data, data.length);
		ByteBuffer b = ByteBuffer.wrap(data);
		ByteBuffer b2 = ByteBuffer.wrap(dataToSender);
		CRC32 crc = new CRC32();
		int ackId = -2;
		boolean fileNameReceived = false;
		boolean fileCreated = false;
		String response = "";
		String fileName = "";
		int dLength = 0;
		int flagLast = 0;
		int rmbSeq = 0;
		
		long startTime = System.currentTimeMillis(); //fetch starting time
		while (!fileNameReceived)
		{
			pkt.setLength(data.length);
			sk.receive(pkt);
			
			b.rewind();
			long chksum = b.getLong();
			crc.reset();
			crc.update(data, 8, pkt.getLength()-8);
			//System.out.println("THE SENT CHECKSUM: "+chksum);
			// Debug output
			//System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
			if (crc.getValue() != chksum){
				ackId = b.getInt();
				int rByteLength = b.remaining();
				byte[] byteRemaining = new byte[rByteLength];
				b.get(byteRemaining);
				
				System.out.println("Pkt corrupt");
				fileName = new String(byteRemaining, 0, rByteLength);	
				System.out.println("Corrupted Filename to save: "+fileName);
				
				//b2.clear();
				//b2.putInt(ackId);
				response = "NAK " + ackId;
				dataToSender = response.getBytes();
				DatagramPacket ack = new DatagramPacket(dataToSender, dataToSender.length,
				pkt.getSocketAddress());
				sk.send(ack);
			}
			else{
				ackId = b.getInt();
				int rByteLength = b.remaining();
				byte[] byteRemaining = new byte[rByteLength];
				b.get(byteRemaining);
				
				fileName = new String(byteRemaining, 0, rByteLength);	
				System.out.println("Clean Filename to save: "+fileName);
				//out.write(byteRemaining,0,rByteLength);
				//System.out.println("Filename to save: "+fileName);
				response = "ACK " + ackId; 
				System.out.println("Pkt id received: " + ackId);
				dataToSender = response.getBytes();
				DatagramPacket ack = new DatagramPacket(dataToSender, dataToSender.length,pkt.getSocketAddress());
				sk.send(ack);
				
				fileNameReceived = true;
				rmbSeq = ackId;
			}
			
		}
		
		try{
		
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName.trim()));
			// 
			while(flagLast == 0)
			{
				pkt.setLength(data.length);
				sk.receive(pkt);
				
				b.rewind();
				long chksum = b.getLong();
				crc.reset();
				crc.update(data, 8, pkt.getLength()-8);

				if (crc.getValue() != chksum)
				{
					System.out.println("Pkt corrupt");
					
					ackId = b.getInt();
					response = "NAK " + ackId;
					dataToSender = response.getBytes();
					DatagramPacket ack = new DatagramPacket(dataToSender, dataToSender.length,
					pkt.getSocketAddress());
					sk.send(ack);
				}
				else
				{
					ackId = b.getInt();
					if (ackId != rmbSeq) //write only if its correct sequence
					{
						rmbSeq = ackId;
						response = "ACK " + ackId; 
						dLength = b.getInt();
						flagLast = b.getInt();
						
						System.out.println("Pkt " + ackId + " Data Length: "+dLength+" flagLast: "+flagLast);
						//b2.clear();
						//b2.putInt(ackId);
						dataToSender = response.getBytes();
						
						byte[] dataItself = new byte[dLength];
						b.get(dataItself);
						out.write(dataItself);
						
						DatagramPacket ack = new DatagramPacket(dataToSender, dataToSender.length,
						pkt.getSocketAddress());
						sk.send(ack);
					}
					else{
						response = "ACK " + ackId; 
						System.out.println("Pkt id sent again: " + ackId);
						System.out.println("Resending response code of: "+response);
						dataToSender = response.getBytes();
						DatagramPacket ack = new DatagramPacket(dataToSender, dataToSender.length,pkt.getSocketAddress());
						sk.send(ack);
					}
				}	
			}//end of while
			
			out.flush();
			out.close();
			
			System.out.println("The File "+fileName+" written successfully.");
			
		} // end of try
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
		
		while(flagLast == 1)
	 {
		 pkt.setLength(data.length);
		 sk.receive(pkt);
		 response = "ACK " + rmbSeq; 
		 System.out.println("Pkt id to close sender: " + rmbSeq);
		 System.out.println("Sending response code of: "+response);
		 dataToSender = response.getBytes();
		 DatagramPacket ack = new DatagramPacket(dataToSender, dataToSender.length,pkt.getSocketAddress());
		 sk.send(ack);
	 }
		
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes, int len) {
		char[] hexChars = new char[len * 2];
		for ( int j = 0; j < len; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}
