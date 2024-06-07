package main.java.torrentmaster;

import java.io.File;
import java.io.FileOutputStream;

public class Readerthread implements Runnable {

    Downloader sr;

    public Readerthread(Downloader sr) {
        this.sr = sr;
    }

    public void run() {
        int r=999;
        //System.out.println("lll");
        while(Downloader.recievepiece<Downloader.number_of_pieces) {
            //System.out.println("ooo");
            while (!Downloader.host_initialized) Thread.onSpinWait();
            //System.out.println("yo");
            synchronized (sr) {
                try {
                    //sr.wait();
                    r = sr.read_message();
                    if (r == 9) {
                        r = sr.read_message(); //read the bitfield
                        sr.notify();
                        sr.wait();
                    }
                } catch (Exception e) {
                    System.out.println(e);
                    System.exit(9);
                }

                try {
                    while(r!=1) {
                        r = sr.read_message();
                    }
                    sr.notify();
                    sr.wait();

                } catch (Exception e) {
                    System.exit(1);
                }
            }
            while(Downloader.recievepiece<Downloader.number_of_pieces){
                try {
                    synchronized (sr) {
                        while(r!=3 && r!=0)
                            r = sr.read_message();

                        if(r!=3) r = sr.read_message();

                        if (r == 3) {
                            Downloader.choke_done = true;
                            Downloader.host_initialized = false;
                            r=999;
                            if(Downloader.recieveblock!=0){
                                int index = Downloader.recievepiece;
                                File f = new File("C:/Custom_Bittorrent_Client/Bencoding Decoder/Pieces/piece-"+index);
                                try (FileOutputStream fos = new FileOutputStream(f)) {
                                    fos.write("".getBytes());
                                }
                                System.out.println(Constants.ANSI_RED + "Piece " + index + " RESETTED" + Constants.ANSI_RESET);
                            }
                            sr.notify();
                            break;
                        }
                        r = 999;
                        sr.notify();
                        if(!Downloader.writer_exited)sr.wait();
                    }
                }
                catch(Exception e){
                    System.out.println(e);
                    //System.exit(12);
                }

            }
        }
    }
}