package main.java.torrentmaster;

import jBittorrentAPI.TorrentFile;
import jBittorrentAPI.Utils;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SendRequest {

    static int  number_of_pieces;
    static int[] pieces;
    private static TorrentFile torrent;


    public static void setTorrent(TorrentFile torrent){
        SendRequest.torrent = torrent;
    }
    public static String getPeers() throws IOException {

        number_of_pieces = (int) Math.ceil((double) torrent.total_length /torrent.pieceLength);
        pieces = new int[number_of_pieces];
        BufferedReader br;
        StringBuffer responsecontent = new StringBuffer();
        String line;

        if(torrent.announceURL.contains("udp://")) return connect_with_UDP();
        String url_link = getUrl();
        System.out.println(url_link + "\n");
        URL url = new URL(url_link);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        //con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        System.out.println("Connection code : " + con.getResponseCode() + "\n");
        byte[] bytes;

        if (con.getResponseCode() > 299) {
            br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            line = br.readLine();

            InputStream is = con.getErrorStream();
            bytes = org.apache.commons.io.IOUtils.toByteArray(is);


            while (line != null) {
                responsecontent.append(line);
            }
            br.close();
        } else {
            //br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            //line = br.readLine();


            InputStream ins = con.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int t;
            byte[] data = new byte[16384];

            while ((t = ins.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, t);
            }

            bytes = buffer.toByteArray();
        }

        Bencode b = new Bencode(bytes);
        //System.out.println(bytes.length);
        TreeMap<String, Object> response = (TreeMap<String, Object>) b.parse();
        //System.out.println(response);  Can be used to see complete response from tracker
        return response.get("peers").toString();
    }

    public static String connect_with_UDP() throws IOException {
        int port = getPortFromUDPurl(torrent.announceURL);
        System.out.println("Port of tracker : " + port);
        SocketAddress socket = new InetSocketAddress(17172);//random port
        DatagramSocket datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket(17696);
        } catch (Exception e) {
            System.exit(98);
        }
        byte[] sendData = null;
        byte[] recieveData = new byte[65508];
        sendData = createConnectRequest();
        String hex = Utils.bytesToHex(Arrays.copyOfRange(sendData,0,7));
        System.out.println(hex);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length);

        datagramSocket.setSoTimeout(8000);// if no data arrives within 8000ms a SocketTimeoutException will be thrown
        datagramSocket.connect(getHost(torrent.announceURL), port);

        datagramSocket.setSoTimeout(1000);
        datagramSocket.send(sendPacket);

        DatagramPacket recievePacket = new DatagramPacket(recieveData, recieveData.length);
        try {
            datagramSocket.receive(recievePacket);
        }
        catch(Exception e){

        }
        byte[] response = recievePacket.getData();
        System.out.println("Recieved");

        int transaction_id = new BigInteger(Arrays.copyOfRange(sendData, 12, 15)).intValueExact();

        long connection_id = readConnectionResponse(response, transaction_id);

        System.out.println("Connection id  is : " + connection_id);

        byte[] peer_id = "12345678900987654321".getBytes(StandardCharsets.UTF_8);
        int listeningport = 16000;
        sendData = createAnnounceRequest(connection_id, transaction_id, peer_id, listeningport);

        sendPacket = new DatagramPacket(sendData, sendData.length);
        datagramSocket.send(sendPacket);
        recievePacket = new DatagramPacket(recieveData, recieveData.length);
        datagramSocket.receive(recievePacket);

        byte[] announce_response = recievePacket.getData();

        return readAnnounceRequest(announce_response);
    }

    private static InetAddress getHost(String announceURL) throws UnknownHostException {
        String[] h = announceURL.split(":");
        System.out.println("Tracker url is : " + h[1].substring(2));
        System.out.println("Inet is "+ InetAddress.getByName(h[1].substring(2)));
        return InetAddress.getByName(h[1].substring(2));
    }

    private static String readAnnounceRequest(byte[] announceResponse) {
        ByteArrayInputStream input = new ByteArrayInputStream(announceResponse,20,announceResponse.length-1);
        StringBuilder sb = new StringBuilder();

        int r = input.read() , count=0;
        while(r!=0){
            sb.append(r);
            count++;
            if(count==6) sb.append(",");
            r = input.read();
        }
        return sb.toString();
    }

    private static byte[] createAnnounceRequest(long connectionId, int transactionId, byte[] peerId , int listeningport) throws IOException {

        byte[] connection_id = ByteBuffer.allocate(8).putLong(connectionId).array();
        byte[] action_id = ByteBuffer.allocate(4).putInt(1).array();
        byte[] transaction_id = ByteBuffer.allocate(4).putInt(transactionId).array();
        byte[] infohash = torrent.info_hash_as_binary;
        //peer id already recieved
        byte[] downloaded = ByteBuffer.allocate(8).putInt(0).array();
        byte[] left = ByteBuffer.allocate(8).putLong(torrent.total_length).array();
        byte[] uploaded = ByteBuffer.allocate(8).putInt(0).array();
        byte[] event = ByteBuffer.allocate(4).putInt(2).array();
        byte[] ip = ByteBuffer.allocate(4).putInt(0).array();
        byte[] key = ByteBuffer.allocate(4).putInt(3833).array(); // reandom key
        byte[] num_want = ByteBuffer.allocate(4).putInt(-1).array();
        byte[] port = new byte[2];
        port[0] = (byte) (listeningport & 0xFF);
        port[1] = (byte) ((listeningport >> 8) & 0xFF);

        ByteArrayOutputStream combine = new ByteArrayOutputStream();
        combine.write(connection_id);
        combine.write(action_id);
        combine.write(transaction_id);
        combine.write(infohash);
        combine.write(downloaded);
        combine.write(left);
        combine.write(uploaded);
        combine.write(event);
        combine.write(ip);
        combine.write(key);
        combine.write(num_want);
        combine.write(port);

        return combine.toByteArray();
    }

    private static long readConnectionResponse(byte[] response , int trans_id) {
        int action = new BigInteger(Arrays.copyOfRange(response ,0,3)).intValueExact();
        if(action!=0) throw new RuntimeException(); // error : action did not match

        int transaction_id = new BigInteger(Arrays.copyOfRange(response ,4,7)).intValueExact();
        if(transaction_id!=trans_id) throw new RuntimeException();

        return new BigInteger(Arrays.copyOfRange(response,8,15)).longValueExact();
    }

    private static byte[] createConnectRequest() throws IOException {
        long h = Long.parseLong("41727101980",16);
        long i = 0x41727101980L;
        //System.out.println(Utils.bytesToHex(new BigInteger(String.valueOf(h)).toByteArray()));
        byte[] connection_id = ByteBuffer.allocate(8).putLong(i).array();
        System.out.println(Utils.bytesToHex(connection_id) + " kkkk " + connection_id.length);
        //byte[] connection_id =  "\\x00\\x00\\x04\\x17\\x27\\x10\\x19\\x80".getBytes();
        //System.out.println(new BigInteger(connection_id).longValueExact());
        byte[] action_id = ByteBuffer.allocate(4).putInt(0).array();
        byte[] transaction_id = ByteBuffer.allocate(4).putInt(112233).array();

        ByteArrayOutputStream combine = new ByteArrayOutputStream();
        combine.write(connection_id);
        combine.write(action_id);
        combine.write(transaction_id);

        return combine.toByteArray();
    }

    public static int getPortFromUDPurl(String announceURL){
        String[] h = announceURL.split(":");
        int a=0;
        char[] url = h[2].toCharArray();// ex : udp://tracker.leechers-paradise.org:6969 if tracker does not have /announce in url
        int i=0;
        while(i<url.length && Character.isDigit(url[i])){
            a *= 10;
            a += (url[i]-'0');
            i++;
        }
        return a;
    }

    public static String getUrl() {

        return torrent.announceURL +
                "?" +
                "info_hash=" +
                torrent.info_hash_as_url +
                "&peer_id=" +
                "12345678900987654321" +
                "&port=6871" +
                "&uploaded=0" +
                "&downloaded=0" +
                "&left=" +
                torrent.total_length +
                "&compact=1";
    }

    public static ArrayList<String> find_ips(String hex) {
        ArrayList<String> ip_list = new ArrayList<>();
        hex = hex.replace(",", "");
        char[] hexchars = hex.toCharArray();
        int count = 0, head = 0;
        int len = hexchars.length;
        StringBuilder sb = new StringBuilder();
        StringBuilder ips = new StringBuilder();
        while (head < len) {
            sb.append(hexchars[head]);
            head++;
            count++;
            try {
                if (count % 2 == 0 && count <= 8) {
                    ips.append(Integer.parseInt(sb.toString(), 16));
                    if (count != 8) ips.append(".");
                    sb.setLength(0);
                }
                if (count == 12) {
                    ips.append(":");
                    ips.append(Integer.parseInt(sb.toString(), 16));
                    sb.setLength(0);
                    ip_list.add(ips.toString());
                    ips.setLength(0);
                    count = 0;
                }
            } catch (Exception e) {
                return ip_list;
            }
        }
        return ip_list;
    }

}