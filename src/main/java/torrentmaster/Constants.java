package main.java.torrentmaster;

public abstract class Constants {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    final static String banner =
            "  _______                        _     __  __           _            \n" +
                    " |__   __|                      | |   |  \\/  |         | |           \n" +
                    "    | | ___  _ __ _ __ ___ _ __ | |_  | \\  / | __ _ ___| |_ ___ _ __ \n" +
                    "    | |/ _ \\| '__| '__/ _ \\ '_ \\| __| | |\\/| |/ _` / __| __/ _ \\ '__|\n" +
                    "    | | (_) | |  | | |  __/ | | | |_  | |  | | (_| \\__ \\ ||  __/ |   \n" +
                    "    |_|\\___/|_|  |_|  \\___|_| |_|\\__| |_|  |_|\\__,_|___/\\__\\___|_|   \n" +
                    "                                                                     \n" +
                    "                                                                     \n"
;
    final static String instructions ="Disclaimer : \n\n------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n" +
            "\n" +
            "TorrentMaster is developed solely for educational and academic purposes as part of my personal learning and skill development. The primary objective of this project is to learn and demonstrate the concepts of programming, software development, and networking protocols, specifically the BitTorrent protocol.\n" +
            "\n" +
            "TorrentMaster is not intended for commercial use, distribution, or any other purpose beyond personal educational enrichment. All components and materials used in the creation of this software adhere to fair use policies and are utilized in compliance with educational guidelines.\n" +
            "\n" +
            "Any resemblance to existing software products is purely coincidental. I do not claim any ownership over trademarks, registered trademarks, or proprietary technologies used or referenced in this project. All rights to these trademarks and technologies remain with their respective owners.\n" +
            "\n" +
            "The use of this software for illegal activities, including but not limited to copyright infringement and distribution of pirated content, is strictly prohibited. I disclaim any liability for misuse of the software by end-users.\n" +
            "\n" +
            "By using this software, you agree to the terms and conditions outlined in this disclaimer.\n" +
            "\n" +
            "------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n";

    public static String pathPieces = "C:/Custom_Bittorrent_Client/Torrent Master/Pieces";
    public static String filePath = "C:/Custom_Bittorrent_Client/Torrent Master/src/test/java/resources/KNOPPIX_V9.1DVD-2021-01-25-EN.torrent";

}
