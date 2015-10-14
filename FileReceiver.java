import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;
import java.io.*;

public class FileReceiver {

/*  public static class DataObject{
    
    public int seq;
    public byte[] data;
    
    //constructor
    public DataObject(int num, byte[]rData)
    {
      seq = num;
      data = rData;
    }
    //get
    public int getSeq() {
      return seq;
    }
      
    public byte[] getData() {
      return data;
    }
    
  }
*/
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
  
    //ArrayList<DataObject> dataList = new ArrayList<DataObject>();
    ArrayList<Integer> seqList = new ArrayList<Integer>();
    
    while(!allPacketsReceived)
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
        //int rByteLength = b.remaining();
        //byte[] byteRemaining = new byte[rByteLength];
        //b.get(byteRemaining);
        
        System.out.println("Pkt corrupt");
        //fileName = new String(byteRemaining, 0, rByteLength);	
        //System.out.println("Corrupted Filename to save: "+fileName);
        
        //b2.clear();
        //b2.putInt(ackId);
        //response = "NAK " + ackId;
        //dataToSender = response.getBytes();
        //DatagramPacket ack = new DatagramPacket(dataToSender, dataToSender.length,
        //pkt.getSocketAddress());
        //sk.send(ack);
      }
      else{
        ackId = b.getInt();
        if ((ackId == 0) && !fileNameReceived)
        {
          numOfPackets = b.getLong();
          System.out.println("received file name and packet size");
          dataList = new ArrayList<byte[]>(Collections.nCopies((int)numOfPackets, new byte[1000]));
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
            if (ackId == 0)
            {
               System.out.println(new String(dataItself, 0, dataItself.length)+" "+numOfPackets);  
            }
            
            dataList.set(ackId,dataItself);
          //  DataObject dataObj = new DataObject(ackId, dataItself);
          //  dataList.add(dataObj);
          }

        //response = "ACK"; 
        //int rLength = response.getBytes().length;
        //
        ByteBuffer b2 = ByteBuffer.wrap(dataToSender);
        System.out.println("Pkt id received: " + ackId);
        System.out.println("Sequence List Size: "+seqList.size()+" num of pkts: "+numOfPackets);
        b2.clear();
        b2.putLong(0);
        b2.putInt(ackId);
        //b2.put(response.getBytes());
        crc.reset();
        crc.update(dataToSender, 8, dataToSender.length-8);
        chksum = crc.getValue();
        b2.rewind();
        b2.putLong(chksum);
        //b2.rewind();
        //System.out.println("CHKSUM: "+chksum);
        //System.out.println("b2 long: "+b2.getLong());
        //System.out.println("b2 ack: "+b2.getInt());
        
        DatagramPacket ack = new DatagramPacket(dataToSender, dataToSender.length,pkt.getSocketAddress());
        sk.send(ack);
        
        }
      }
    
    if (fileNameReceived && (seqList.size() == numOfPackets))
    {
      //Sorting alogrithm
    /*
      Collections.sort(dataList, new Comparator<DataObject>() {
      @Override
      public int compare(DataObject d1, DataObject d2)
      {
          return  ((Integer)d1.getSeq()).compareTo(d2.getSeq());
      }
      });
     */
     fileName = new String(dataList.get(0), 0, dataList.get(0).length);
     System.out.println("filename is : "+fileName);
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
     
      
    }
      
    }
    
    
  }

}
