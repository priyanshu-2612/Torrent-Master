package main.java.torrentmaster;

import jBittorrentAPI.TorrentFile;
import jBittorrentAPI.TorrentProcessor;
import java.io.*;
import java.util.ArrayList;
import java.util.TreeMap;

public class Main {

    public static void main(String[] args) throws IOException {
            Torrent_Master();
    }

    public static void downloadAllPieces(ArrayList<String> ips , TorrentFile torrent) throws Exception {
        Downloader.setIpList(ips);
        Downloader.setTorrent(torrent);
        Downloader sr = new Downloader();
        Thread t1 = new Thread(new Writerthread(sr));
        Thread t2 = new Thread (new Readerthread(sr));

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    public static void Torrent_Master() throws IOException {


        System.out.print(Constants.ANSI_BLUE + Constants.banner);
        System.out.println(Constants.instructions + Constants.ANSI_RESET);

        File file = new File((Constants.filePath));

        TorrentFile torrent = new TorrentFile();

        TorrentProcessor p = new TorrentProcessor(torrent);
        p.addFile(file);
        torrent = p.getTorrentFile(p.parseTorrent(file));
        System.out.println( "Info Hash : "+ torrent.info_hash_as_hex + "\n");
        System.out.println("Total length of torrent : " + torrent.total_length + " bytes");
        System.out.println("SHA1 hash of Piece values " + torrent.piece_hash_values_as_hex + "\n");

        byte[] bytes = new byte[(int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(bytes);
        }

        TreeMap<String, Object> map = null;
        try {

            Bencode ben = new Bencode(bytes);

            map = (TreeMap<String, Object>) ben.parse();
            System.out.println("Torrent File : " + map + "\n");
            TreeMap<String, Object> info = (TreeMap<String, Object>) map.get("info");
            System.out.println("Info Directory : "+ map.get("info") + "\n");

        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println("Number of pieces : " + (long)Math.ceil((double) torrent.total_length / torrent.pieceLength));
        System.out.println("Total size : " + torrent.total_length + " bytes");
        System.out.println("Piece size : " + torrent.pieceLength + " bytes");
        System.out.println("Last piece size : " + torrent.total_length % torrent.pieceLength + " bytes");
        System.out.println("Block size : " + (long)Math.pow(2,14) + " bytes");
        System.out.println("Last block size : " + (long)((torrent.total_length % torrent.pieceLength) % (Math.pow(2, 14))) + " bytes\n");

        SendRequest.setTorrent(torrent);
        String peers = SendRequest.getPeers();
        //System.out.println("Peer Ips/ports [HEX] : " + peers);

        ArrayList<String> Ips = SendRequest.find_ips(peers);
        System.out.println("Peer Ips/ports : " + Ips + "\n");

        try {
            downloadAllPieces(Ips,torrent);
        }
        catch (Exception e){
            System.exit(8);
        }
    }
}


