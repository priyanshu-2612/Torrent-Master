# TorrentMaster - A Bittorrent Implementation in Java

## Overview

**TorrentMaster** is an implementation of the BitTorrent protocol in Java. Using it you can understand the protocol clearly and even contribute to future developments.The program parses a .torrent file and gets the list of peers from the tracker, connection is established with the peers and the pieces are downloaded in the Pieces directory.

## Features

- **Torrent Parsing**: Parse .torrent files to extract metadata and tracker information.
- **Piece Management**: Handle piece downloading, verification, and assembly.
- **Tracker Communication**: Announce to trackers and obtain peer lists.
- **Error Handling**: Robust error handling and recovery mechanisms.
- **Peer-to-Peer Communication**: Efficiently connect to multiple peers to download files.

## Usage

In Constants class , initialize filePath and pathPieces to the path of the .torrent file and the folder where you would like to download its pieces.

## Working of the Project
![image](https://github.com/priyanshu-2612/TorrentMaster-A-BitTorrent-Implementation/assets/136080688/977193fa-f7fd-4570-a8e8-5c50dbd2fe21)
![image](https://github.com/priyanshu-2612/TorrentMaster-A-BitTorrent-Implementation/assets/136080688/0bb54e3d-26bc-4cd0-a113-ad0a417907b1)
![image](https://github.com/priyanshu-2612/TorrentMaster-A-BitTorrent-Implementation/assets/136080688/8c5c6647-9c41-46d1-bde6-ad931aa7f0a7)
![image](https://github.com/priyanshu-2612/TorrentMaster-A-BitTorrent-Implementation/assets/136080688/aca3c6e7-dfe9-45cf-b778-9c214599969a)


## Contact

For any questions or inquiries, please contact [priyanshu.sharma2612@gmail.com](mailto:priyanshu.sharma2612@gmail.com).

Enjoy playing with **TorrentMaster** and happy torrenting!
