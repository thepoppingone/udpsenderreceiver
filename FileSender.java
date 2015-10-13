import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;
import java.util.concurrent.*;
import java.io.*;

public class FileSender {
	
	public static void main(String[] args) throws Exception 
	{
		if (args.length != 4) {
			System.err.println("Usage: SimpleUDPSender <host> <port> <source path> <destination path>");
			System.exit(-1);
		}
		
		String fromFileName = args[2];
		
		String destinationName = args[3];
		try{
			
			BufferedInputStream in  = new BufferedInputStream(new FileInputStream(fromFileName));	
			InetSocketAddress addr = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
			int num = 10;
			
			long chksum = (long) 1;
			
			DatagramSocket sk = new DatagramSocket();
			sk.setSoTimeout(500);
			DatagramPacket pkt;
			byte[] data = new byte[950];
			byte[] dataR = new byte[200];
			byte[] dataF = new byte[200];
			byte[] dataS = new byte[1000];
			
			ByteBuffer b = ByteBuffer.wrap(dataS);
			ByteBuffer bbFileName = ByteBuffer.wrap(dataF);
			
			CRC32 crc = new CRC32();
			boolean stillWaitingForName = true;
			
			/********************************************************************************************************
			PART ONE - SEND FILE NAME UNTIL SUCCESS 						
			*****************************************************************************************************************/
			
			/**
			*  SENDS THE FILE NAME - WILL KEEP TRYING UNTIL THE ACK -1 is received
			*/
			while(true){
				String response = "empty response at filename";
				bbFileName.clear();
				bbFileName.putLong(0);
				bbFileName.putInt(-1); //sequence number 
				bbFileName.put(destinationName.getBytes());
				crc.reset();
				crc.update(dataF, 8, dataF.length-8);
				chksum = crc.getValue();
				bbFileName.rewind();
				bbFileName.putLong(chksum);
				
				DatagramPacket fileNameSave = new DatagramPacket(dataF, dataF.length, addr);
				//System.out.println("Sent CRC:" + chksum + " Contents:" + bytesToHex(dataR));
				sk.send(fileNameSave);
				/**
				* FOR ENSURING THAT THE FILE NAME IS RECEIVED
				*/
				
				pkt = new DatagramPacket(dataR, dataR.length, addr);
				pkt.setLength(dataR.length);
				while(stillWaitingForName)
				{
					try{
					//	pkt = new DatagramPacket(dataR, dataR.length, addr);
						//pkt.setLength(dataR.length);
						sk.receive(pkt);
						stillWaitingForName = false;
						
					}	
					catch(SocketTimeoutException e){
						fileNameSave = new DatagramPacket(dataF, dataF.length, addr);
						sk.send(fileNameSave);
					}
				}
				response = new String(pkt.getData(), 0, pkt.getLength());
				System.out.println("fileName: "+response);
				stillWaitingForName = true;
				if(response.equals("ACK -1"))
				{break;}
			}
			
			
			/********************************************************************************************************
			PART TWO - SEND DATA					
			*****************************************************************************************************************/
			
			/**
			* SENDS DATA ITSELF
			*/
			int length = 0;
			int i = 1;
			int flagLast = 0;
			boolean stillWaiting = true;
			
			while ((length = in.read(data,0,data.length))!= -1)
			{
				String response = "empty response at data";
				int dLength = length;
				if(dLength < data.length){flagLast = 1;}
				b.clear();
				// reserve space for checksum
				b.putLong(0);
				b.putInt(i); //sequence number  
				b.putInt(dLength); //data length
				b.putInt(flagLast);
				b.put(data);
				crc.reset();
				crc.update(dataS, 8, dataS.length-8);
				chksum = crc.getValue();
				b.rewind();
				b.putLong(chksum);
				
				pkt = new DatagramPacket(dataS, dataS.length, addr);
				// Debug output
				//System.out.println("Sent CRC:" + chksum + " Contents:" + bytesToHex(data));
				sk.send(pkt);
				
				dataR = new byte[200];
				
				while(stillWaiting)	
				{
					try{
						pkt = new DatagramPacket(dataR, dataR.length, addr);
						pkt.setLength(dataR.length);
						sk.receive(pkt);
						stillWaiting = false;
						
					}
					catch(SocketTimeoutException e){
						pkt = new DatagramPacket(dataS, dataS.length, addr);
						sk.send(pkt);
					}
				}
				response = new String(pkt.getData(), 0, pkt.getLength());
				System.out.println("dataResp: "+response);
				stillWaiting = true;
				while(!response.equals("ACK "+i)) //corrupted packet so resend
				{
					System.out.print("Resending packet! --- ");
					pkt = new DatagramPacket(dataS, dataS.length, addr);
					sk.send(pkt);
					dataR = new byte[200];
					while(stillWaiting)
					{
						try{
							pkt = new DatagramPacket(dataR, dataR.length, addr);
							pkt.setLength(dataR.length);
							sk.receive(pkt);
							stillWaiting = false;
						
						}	catch(SocketTimeoutException e){
							pkt = new DatagramPacket(dataS, dataS.length, addr);
							sk.send(pkt);
						}
					}
						response = new String(pkt.getData(), 0, pkt.getLength());
						System.out.println("dataResp: "+response);
						stillWaiting = true;
				}
				i++; //increment for seqeuence number
				
			}
			
			// while (true)
			// {
			// 	dataR = new byte[200];
			// 	pkt = new DatagramPacket(dataR, dataR.length, addr);
			// 	pkt.setLength(dataR.length);
			// 	sk.receive(pkt);
			// 	 
			// 	String response = new String(pkt.getData(), 0, pkt.getLength());
			// 	//bbFileName.rewind();
			// 	System.out.println(response);
			// 	
			// }
		}catch(Exception e){
			e.printStackTrace(System.out);
		}
		
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	public static void sendOutData(BufferedInputStream in, byte[] data, ByteBuffer b ){
		
	}
}
