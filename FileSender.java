import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;
import java.io.*;

public class SimpleUDPSender {

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 3) {
			System.err.println("Usage: SimpleUDPSender <host> <port> <file path and name>");
			System.exit(-1);
		}

		String fileName = args[2];

		InetSocketAddress addr = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
		int num = 10;
		DatagramSocket sk = new DatagramSocket();
		DatagramPacket pkt;
		byte[] data = new byte[20];
		byte[] dataR = new byte[200];
		ByteBuffer b = ByteBuffer.wrap(data);
		ByteBuffer b2 = ByteBuffer.wrap(dataR);

		CRC32 crc = new CRC32();

		for (int i = 1; i <= num; i++)
		{
			b.clear();
			// reserve space for checksum
			b.putLong(0);
			b.putInt(i);
			crc.reset();
			crc.update(data, 8, data.length-8);
			long chksum = crc.getValue();
			b.rewind();
			b.putLong(chksum);

			pkt = new DatagramPacket(data, data.length, addr);
			// Debug output
			System.out.println("Sent CRC:" + chksum + " Contents:" + bytesToHex(data));
			sk.send(pkt);
			
		}
		
		while (true)
		{
			pkt = new DatagramPacket(dataR, dataR.length, addr);
			pkt.setLength(dataR.length);
			sk.receive(pkt);
			 
			String response = new String(pkt.getData(), 0, pkt.getLength());
			//b2.rewind();
			System.out.println(response);
			
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
