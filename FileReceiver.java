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
    ByteBuffer b = ByteBuffer.wrap(data);
    
    byte[] dataToSender = new byte[200];
    
    
    DatagramPacket pkt = new DatagramPacket(data, data.length);
    
    CRC32 crc = new CRC32();
    int ackId = -2;
    boolean fileNameReceived = false;
    boolean fileCreated = false;
    boolean allPacketsReceived = false;
    String response = "";
    String fileName = "";
    int dLength = 0;
    int rmbSeq = 0;
    long numOfPackets = 0;
    List<byte[]> dataList = new ArrayList<byte[]>();
    
    ArrayList<Integer> seqList = new ArrayList<Integer>();
    
    while(!allPacketsReceived)
    {
      pkt.setLength(data.length);
      sk.receive(pkt);
      
      b.rewind();
      long chksum = b.getLong();
      crc.reset();
      crc.update(data, 8, pkt.getLength()-8);
      if (crc.getValue() != chksum){
        ackId = b.getInt();
        //System.out.println("Pkt corrupt - pkt dropped");
        
      }
      else{
        ackId = b.getInt();
        if ((ackId == 0) && !fileNameReceived)
        {
          numOfPackets = b.getLong();
          System.out.println("received file name and packet size");
          // Initialize with Arraylist with all empty bytes inside
          dataList = new ArrayList<byte[]>(Collections.nCopies((int)numOfPackets, new byte[0]));
          fileNameReceived = true;
          
        }
        if (fileNameReceived)
        {
          dLength = b.getInt();
          byte[] dataItself = new byte[dLength];
          if(!seqList.contains(ackId))
          {
            seqList.add(ackId);
            b.get(dataItself);
          //  if (ackId == 0)
          //  {
          //    System.out.println(new String(dataItself, 0, dataItself.length)+" "+numOfPackets);  
          //  }
            
            dataList.set(ackId,dataItself);
            
          }
          
          ByteBuffer b2 = ByteBuffer.wrap(dataToSender);
          //System.out.println("Pkt id received: " + ackId);
          System.out.println("Seq List Size: "+seqList.size()+" # of pkts: "+numOfPackets+" Progress: "+((seqList.size()*100)/numOfPackets)+"%");
          b2.clear();
          b2.putLong(0);
          b2.putInt(ackId);
          
          crc.reset();
          crc.update(dataToSender, 8, dataToSender.length-8);
          chksum = crc.getValue();
          b2.rewind();
          b2.putLong(chksum);
          
          
          DatagramPacket ack = new DatagramPacket(dataToSender, dataToSender.length,pkt.getSocketAddress());
          sk.send(ack);
          
        }
      }
      
      if (fileNameReceived && (seqList.size() == numOfPackets))
      {
        fileName = new String(dataList.get(0), 0, dataList.get(0).length);
        
        try{
          BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName.trim()));
          for(int j=1; j<dataList.size();j++)
          {
            out.write(dataList.get(j));
          }
          out.flush();
          out.close();
        }catch(Exception e){
          e.printStackTrace(System.out);
        }
        
        System.out.println("File Written, filename is : "+fileName);
        seqList.add(seqList.size()); // ONLY WRITE THE FILE ONL
      }
      
    }
    
    
  }
  
}
