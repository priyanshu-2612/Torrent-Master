package main.java.torrentmaster;

import jBittorrentAPI.TorrentFile;
import jBittorrentAPI.Utils;
import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;

public class Downloader {

    static volatile boolean host_initialized=false , choke_done=false , writer_exited = false;
    static int recievepiece=0 ,sendpiece=0, sendblock=0 , recieveblock=0;
    static InputStream in;
    static OutputStream out;

    private static ArrayList<String> ips;
    private static long total_length,block_size,piece_length , lastpiecelength , lastblocklength;
    static long number_of_pieces;
    static int[] hosts;
    private static TorrentFile torrent;

    //volatile BitSet bitmap; reason for commenting : no use currently
    public static void setTorrent(TorrentFile torrent){
        Downloader.torrent = torrent;
        total_length = torrent.total_length;
        piece_length = torrent.pieceLength;
        number_of_pieces = total_length/piece_length;
        block_size = (long) Math.pow(2,14);
        lastpiecelength = total_length % piece_length;
        lastblocklength = piece_length % block_size;
    }
    public static void setIpList(ArrayList<String> ips){

        Downloader.ips = ips;
        Downloader.hosts = new int[ips.size()];
    }
    public static Socket connectToNewHost(){

        int host=0;
        Socket socket;
        InetSocketAddress sockAddr ;
        socket = new Socket();

        while(!socket.isConnected()) {
            try {
                socket = new Socket();
                host = getHostIndex();
                if(host==-1){
                    System.out.println("Hosts Exhausted");
                    System.exit(0);
                }
                String ipandport = ips.get(host);
                String[] ip_port = ipandport.split(":");
                sockAddr = new InetSocketAddress(ip_port[0], Integer.parseInt(ip_port[1]));
                socket.connect(sockAddr,1000);
            } catch (IOException e) {
                System.out.println("couldn't connect to " + ips.get(host).split(":")[0]);
            }
        }
        return socket;
    }
    public void initialize_streams() throws Exception {

        Socket socket = connectToNewHost();
        in = socket.getInputStream();
        out = socket.getOutputStream();
        Downloader.host_initialized = true;
    }

    public static int getHostIndex() {
        for(int i=0; i<hosts.length ; i++){
            if(hosts[i]==0){
                hosts[i]=1;
                return i;
            }
        }
        return -1;
    }
    public void send_handshake() throws IOException {

        byte[] l = {(byte) 19}, name = "BitTorrent protocol".getBytes(), reserved_bytes = new byte[8],
                info_hash = torrent.info_hash_as_binary, peer_id = "12345678900987654321".getBytes();

        ByteArrayOutputStream combine = new ByteArrayOutputStream();
        combine.write(l);
        combine.write(name);
        combine.write(reserved_bytes);
        combine.write(info_hash);
        combine.write(peer_id);

        byte[] c = combine.toByteArray(), moff = new byte[4];

        out.write(c);

        System.out.println("***** Sent Handshake *****");

    }

    public void send_interested() throws IOException {

        byte[] mssg_len = new byte[4], mssg_id = {(byte) 2};
        mssg_len[3] = (byte) 1;
        ByteArrayOutputStream interested_req = new ByteArrayOutputStream();
        interested_req.write(mssg_len);
        interested_req.write(mssg_id);

        out.write(interested_req.toByteArray());

        System.out.println("***** Sent Interested Message *****");

    }

    public int send_request() throws Exception {

        long pieceLength;
        if(sendpiece==number_of_pieces-1 && lastpiecelength!=0) pieceLength = lastpiecelength;
        else pieceLength = piece_length;

        long numofblocks = (long)Math.ceil((double)pieceLength/block_size) ;
        if(sendpiece==number_of_pieces-1)System.out.println(Constants.ANSI_GREEN + "Number of blocks : " + numofblocks + Constants.ANSI_RESET);

        //long lastblocklen = pieceLength % block_size;

        byte[] mssg_len = new byte[4], mssg_id = new byte[1], index_of_piece = ByteBuffer. allocate(4). putInt(sendpiece).array(),
                bytes_offset , block_length;

        mssg_len[3] = (byte)13;
        mssg_id[0] = 0x06;

        for (int i = 0; i < numofblocks ; i++) {


            bytes_offset = ByteBuffer.allocate(4).putInt(i*(int)Math.pow(2,14)).array();
            block_length = ByteBuffer.allocate(4).putInt((int)Math.pow(2,14)).array();
            if(lastblocklength!=0 && i==numofblocks-1){
                block_length = ByteBuffer.allocate(4).putInt((int)lastblocklength).array();
                System.out.println(Constants.ANSI_BLUE + "Last block length is  :  " + new BigInteger(block_length).intValueExact() + " its size : "+ block_length.length + Constants.ANSI_RESET);
            }

            ByteArrayOutputStream request_req = new ByteArrayOutputStream();
            request_req.write(mssg_len);
            request_req.write(mssg_id);
            request_req.write(index_of_piece);
            request_req.write(bytes_offset);
            request_req.write(block_length);

            out.write(request_req.toByteArray());
            System.out.println("***** Sent request Message for block " + i + " of piece " + sendpiece +" *****");
            sendblock++;
            if(sendblock==numofblocks){
                sendblock=0;
                sendpiece+=1;
                return 0;
            }
            if(sendblock%2==0){  //sending 2 blocks at once to increase speed and avoid connection timeout
                this.notify();
                this.wait();
            }
        }
        return 0;
    }

    public int read_message() throws Exception {

        boolean handshake = false;
        byte[] len = new byte[4];
        int offset=0;
        while(offset<len.length) {
            int t = in.read(len, offset, len.length - offset);
            if(t>0)offset += t;
        }
        if((int)len[0]==19 && len[1] =='B') handshake = true;

        int length_of_message = new BigInteger(len).intValue();
        //int message_id = (int) in.read();
        byte[] messageid = new byte[1];
        int rep = in.read(messageid,0,1);
        int message_id = new BigInteger(messageid).intValueExact();

        if(handshake){
            //HANDSHAKE MESSAGE
            System.out.println("***** Handshake message recieved *****");
            int count=0 , read;
            int ofset=0;
            byte[] hand = new byte[63];
            while(ofset<hand.length) {
                int t = in.read(hand, ofset, hand.length - ofset);
                if(t>0)ofset += t;
            }
            System.out.println("Handshake complete");
            return 9;

        }
        else if (message_id == 5) {
            try {
                return read_Bitfield(length_of_message-1);
            }
            catch (Exception e){
            }
        }
        else if (message_id == 4) {
            return read_Have(length_of_message-1);

        }
        else if (message_id == 1) {
            //UNCHOKE MESSAGE
            System.out.println("***** unchoke message *****");

            //hosts[hostIndex] = 2;
            return 1; //unchoke

        }
        else if(message_id == 0){
            //CHOKE MESSAGE
            System.out.println(Constants.ANSI_RED+ " choke message " + Constants.ANSI_RESET);
            return 3;
        }
        else if(message_id == 7){
            return read_piece(length_of_message-1);
        }
        else if(message_id == 8){
            //CANCEL MESSAGE
            return read_cancel(length_of_message-1);
        }
        else {
            System.out.println("message id not matched - message id : " + message_id);
        }
        return 10;
    }

    private int read_cancel(int length_of_message) throws IOException {
        System.out.println("---------------------cancel message---------------------");
        byte[] index_of_piece = new byte[4];
        byte[] bytes_sent = new byte[4];
        byte[] data = new byte[4];
        int count = 0;
        while (count < length_of_message-1) {
            if(count < 4){
                index_of_piece[count] = (byte) in.read();
            }
            else if(count < 8){
                bytes_sent[count-4] = (byte)in.read();
            }
            else if(count < 12){
                data[count-8] = (byte)in.read();
            }
            count++;
        }
        return 10;
    }

    public int read_piece(int length_of_message) throws Exception {
        //PIECE MESSAGE
        long numofblocks = (long)Math.ceil((double)piece_length/block_size);
        System.out.println("---------------------piece message---------------------");
        byte[] index_of_piece = new byte[4];
        byte[] bytes_sent = new byte[4];
        byte[] data = new byte[length_of_message-8];
        int off=0;
        while(off<index_of_piece.length) {
            int t1 = in.read(index_of_piece, off, index_of_piece.length - off);
            if (t1 > 0) off += t1;
        }
        off = 0;
        while(off<bytes_sent.length) {
            int t1 = in.read(bytes_sent, off, bytes_sent.length - off);
            if (t1 > 0) off += t1;
        }
        off = 0;
        while(off<data.length) {
            int t1 = in.read(data, off, data.length - off);
            if (t1 > 0) off += t1;
        }
        int index = new BigInteger(index_of_piece).intValueExact();


        File f = new File(Constants.pathPieces + "/piece-" +index);
        try (FileOutputStream fos = new FileOutputStream(Constants.pathPieces + "/piece-" +index, true)) {
            fos.write(data);
        }


        int bytes_sent_integer = new BigInteger(bytes_sent).intValueExact();
        double blocknum = (bytes_sent_integer/Math.pow(2,14));

        System.out.println(" Written (Appended) block " + blocknum + " to piece file " + index);
        recieveblock++;
        if(blocknum==numofblocks-1){
            String hash = Utils.bytesToHex(createSha1(new File(Constants.pathPieces + "/piece-" +index))).toUpperCase();
            //System.out.println(" ////// We now have piece "+ index + " //////");
            if(torrent.piece_hash_values_as_hex.contains(hash)
            ) {
                //System.out.println(hash + " " + torrent.piece_hash_values_as_hex.get(index)); to check if createSha1 is working properly
                System.out.println(Constants.ANSI_BLUE +"CORRECTLY DOWNLOADED PIECE " + index + Constants.ANSI_RESET);
                recieveblock=0;
                recievepiece++;
                return 0;
            }
            else{
                System.out.println("Piece file " + index + " has Hash mismatch....Resetting file");
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    fos.write("".getBytes());
                }
                recieveblock=0;
                System.out.println(Constants.ANSI_RED + "Piece file " + index + " RESETTED" + Constants.ANSI_RESET);
                return -7;
            }
        }
        return 0;
    }

    public int read_Have(int lengthOfMessage) throws IOException {
        //HAVE MESSAGE
        //System.out.println("have message");
        byte[] payload = new byte[4];
        for (int i = 0; i < 4; i++)
            payload[i] = (byte) in.read();
        int index_of_newly_added_piece_by_peer = new BigInteger(payload).intValue();
        return 10;
        /*synchronized (bitmap){
            bitmap.set(index_of_newly_added_piece_by_peer);
        }*/
        //System.out.println("peer now has " + index_of_newly_added_piece_by_peer + "th piece");

    }

    public int read_Bitfield(int length_of_message) throws IOException {

        //BITFIELD MESSAGE
        System.out.println("***** Bitfield message recieved *****");
        int count = 0;
        byte[] bitvals = new byte[length_of_message];
        int offs=0;
        while(offs<bitvals.length) {
            int t = in.read(bitvals, offs, bitvals.length - offs);
            offs += t;
            if (t == -1) break;
        }

        //this.bitmap = BitSet.valueOf(bitvals); A bitmap can be added for an improved implementation

        System.out.println(length_of_message + " bytes of bitfield data read");
        return 10;
    }


    public static synchronized byte[] createSha1(File file) throws Exception  {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        InputStream fis = new FileInputStream(file);
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fis.read(buffer);
            if (n > 0) {
                digest.update(buffer, 0, n);
            }
        }
        return digest.digest();
    }

}
