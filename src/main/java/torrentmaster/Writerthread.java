package main.java.torrentmaster;

public class Writerthread implements Runnable {
    Downloader sr;

    public Writerthread(Downloader sr) {
        this.sr = sr;
    }

    public void run() {
        while(Downloader.sendpiece<Downloader.number_of_pieces){
            synchronized (sr){
                try {
                    sr.initialize_streams();
                }
                catch (Exception e){
                    System.exit(4);
                }
            //}
            //synchronized (sr) {
                try {
                    sr.send_handshake();
                    sr.notify();
                    sr.wait();

                    sr.send_interested();
                    sr.notify();
                    sr.wait();
                }
                catch (Exception e){
                    System.err.println(e);
                    System.exit(5);
                }
            }
            while(Downloader.sendpiece<Downloader.number_of_pieces){

                try {
                    synchronized (sr) {
                        sr.send_request();
                        sr.notify();
                        sr.wait();
                        if (Downloader.choke_done) {
                            System.out.println("Creating new Connection...");
                            break;
                        }
                    }
                }
                catch(Exception e){
                    System.out.println(e);
                    System.exit(6);
                }


            }
        }
        Downloader.writer_exited = true;
    }
}