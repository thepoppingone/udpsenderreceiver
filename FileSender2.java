import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;
import java.util.concurrent.*;
import java.io.*;

public class FileSender2 {
	
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
			long fileSize = new File(fromFileName).length();
			long numOfPackets = (fileSize/950)+2;
			
			long chksum = (long) 1;
			
			DatagramSocket sk = new DatagramSocket();
			sk.setSoTimeout(200);
			DatagramPacket pkt;
			byte[] data = new byte[950];
			byte[] dataR = new byte[200];
			byte[] dataF = new byte[200];
			byte[] dataS = new byte[1000];
			byte[] dataRS = new byte[1000];
			byte[] dataAck = new byte[200];
			
			ByteBuffer b = ByteBuffer.wrap(dataS);
			ByteBuffer b2 = ByteBuffer.wrap(dataRS);
			ByteBuffer bbFileName = ByteBuffer.wrap(dataF);
			ByteBuffer bbAck = ByteBuffer.wrap(dataAck);
			
			CRC32 crc = new CRC32();
			boolean stillWaitingForName = true;
			ArrayList<byte[]> packetSendList = new ArrayList<byte[]>();
			ArrayList<Integer> seqList = new ArrayList<Integer>();
			
			/**
			*  SENDS THE FILE NAME - WILL KEEP TRYING UNTIL THE ACK -1 is received
			*/
			int dLength = destinationName.getBytes().length;
			//String response = "empty response at filename";
			bbFileName.clear();
			bbFileName.putLong(0);
			bbFileName.putInt(0); //sequence number 
			bbFileName.putLong(numOfPackets);
			bbFileName.putInt(dLength);
			bbFileName.put(destinationName.getBytes());
			crc.reset();	
			crc.update(dataF, 8, dataF.length-8);
			chksum = crc.getValue();
			bbFileName.rewind();
			bbFileName.putLong(chksum);
			
			DatagramPacket fileNameSave = new DatagramPacket(dataF, dataF.length, addr);
			//System.out.println("Sent CRC:" + chksum + " Contents:" + bytesToHex(dataR));
			sk.send(fileNameSave);
			seqList.add(0);
			packetSendList.add(dataF);
			
			pkt = new DatagramPacket(dataR, dataR.length, addr);
			pkt.setLength(dataR.length);		
			
			/********************************************************************************************************
			PART TWO - SEND DATA					
			*****************************************************************************************************************/
			
			/**
			* SENDS DATA ITSELF
			*/
			int length = 0;
			int i = 1;
			boolean stillWaiting = true;
			
			while ((length = in.read(data,0,data.length))!= -1)
			{
				String response = "empty response at data";
				dLength = length;
				b.clear();
				// reserve space for checksum
				b.putLong(0);
				b.putInt(i); //sequence number  
				b.putInt(dLength); //data length
				b.put(data);
				crc.reset();
				crc.update(dataS, 8, dataS.length-8);
				chksum = crc.getValue();
				b.rewind();
				b.putLong(chksum);
				
				byte[] newByte = new byte[1000];
			 	System.arraycopy(dataS, 0, newByte, 0, dataS.length);
				seqList.add(new Integer(i));
				packetSendList.add(newByte);
				
				pkt = new DatagramPacket(dataS, dataS.length, addr);
				// Debug output
				//System.out.println("Sent CRC:" + chksum + " Contents:" + bytesToHex(data));
				sk.send(pkt);

				System.out.println("Sending packet first time: "+i);
				dataR = new byte[200];
				
				if ((i+1)%5 == 0 || (i+1) == numOfPackets )
				{
					while(!seqList.isEmpty())	
					{
							System.out.println("In while loop");
						try{
							pkt = new DatagramPacket(dataAck, dataAck.length);
							pkt.setLength(dataAck.length);
							sk.receive(pkt);
							
							bbAck.rewind();
							chksum = bbAck.getLong();
							int indexRemove = bbAck.getInt();
							System.out.println("ACK NUMBER: "+indexRemove);
							System.out.println("CHKSUM: "+chksum);
							crc.reset();
							crc.update(dataAck, 8, pkt.getLength()-8);
							if (crc.getValue() != chksum){
								System.out.println("ACK message corrupted");
							}else{
						
								seqList.remove(new Integer(indexRemove));
								System.out.println("removing PACKET: "+indexRemove);
							}
							//response = new String(pkt.getData(), 0, pkt.getLength());
							//System.out.println(response);
							//if(response.substring(0,3).equals("ACK"))
							//{
							//	int	indexRemove = Integer.parseInt(response.substring(4));
							//	seqList.remove(new Integer(indexRemove));
							//	System.out.println("removing PACKET: "+indexRemove);
							//}
						}
						catch(SocketTimeoutException e){
							for(int j=0;j<seqList.size();j++)
							{ 
								dataRS = packetSendList.get(seqList.get(j));
							//	System.out.println("resending packet: "+seqList.get(j)+" of DATA \n"+(new String(dataRS, 0, dataRS.length)));
								System.out.println("resending packet: "+seqList.get(j));
								pkt = new DatagramPacket(dataRS, dataRS.length, addr);
								sk.send(pkt);
							}
						}
					} // end of while
				}
				i++; //increment for seqeuence number	
			}			
			
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

}
