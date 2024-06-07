package main.java.torrentmaster;

import jBittorrentAPI.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

public class Bencode {

    boolean pieces = false;
    boolean peers=false;
    int head = 0;
    int tail;
    byte[] input;
    public Bencode(byte[] in){
        input = in;
        tail = in.length-1;
    }
    public Object parse() {

        byte head = read(input);
        if (head == 'i')
            return parseInt();
        else if (Character.isDigit(head))
            return parseString();
        else if (head == 'l')
            return parseList();
        else if (head == 'd')
            return parseDictionary();

        return "-2";
    }

    public byte read(byte[] input) {
        if (input[head] == 'i') {
            head++;
            //tail--;
            return 'i';
        } else if (Character.isDigit(input[head])) {
            return input[head];
        } else if (input[head] == 'l') {
            head++;
            //tail--;
            return 'l';
        } else if (input[head] == 'd') {
            head++;
            tail--;
            return 'd';
        } else return 'e';
    }

    public long parseInt() {
        long ans = 0;
        while (input[head] != 'e' && input[head] != ':') {
            ans *= 10;
            ans += (char)input[head] - '0';
            head++;
        }
        head++;
        return ans;
    }

    public String parseString() {
        int bytesparsed = 0;
        long count = parseInt();

        StringBuilder sb = new StringBuilder();
        while(count!=0 && head<=tail){
            if(!pieces && !peers)sb.append((char) input[head]);
            else {
                //System.out.println("Byte : " + (int)input[head]);
                sb.append(Utils.byteArrayToByteString(new byte[] {input[head]}));
                bytesparsed++;
                if(bytesparsed==20 && pieces) {
                    sb.append(",");
                    bytesparsed=0;
                }
                if(bytesparsed==12 && peers) {
                    sb.append(",");
                    bytesparsed=0;
                }
            }
            head++;
            count--;
        }
        return sb.toString();
    }

    public ArrayList<Object> parseList(){
        ArrayList<Object> ans = new ArrayList<>();
        while(true){
            if(head > tail ) break;
            ans.add(parse());
            if(input[head]=='e') {
                head++;
                break;
            }
        }
        return ans;
    }

    public TreeMap<String,Object> parseDictionary(){
        TreeMap<String,Object> ans = new TreeMap<>();
        while(true){
            if(head>tail || input[head]=='e')
                    break;
            String key = parseString();
            if(key.equals("pieces")) pieces = true;
            if(key.equals("peers"))peers= true;
            ans.put(key, parse());
            if(input[head]=='e'){
                head++;
                break;
            }
        }
        return ans;
    }

    public byte[] bencode_again(Object info_dictionary){
        TreeMap<String,Object> info = (TreeMap<String,Object>) info_dictionary;
        info.put("pieces" , info.get("pieces").toString().replace(",",""));
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = info.descendingKeySet().descendingIterator() ;
        while(it.hasNext()){
            String temp = it.next();
            sb.append(count_chars(temp));
            sb.append(":");
            sb.append(temp);
            if(temp.equals("length") || temp.equals("piece length")){
                sb.append("i");
                sb.append(info.get(temp).toString());
                sb.append("e");
            }
            else if(temp.equals("pieces")){
                sb.append(count_chars(info.get("pieces").toString())/2);
                sb.append(":");
                String s = info.get("pieces").toString();

                sb.append(s);
            }
            else {
                sb.append(count_chars(info.get(temp).toString()));
                sb.append(":");
                sb.append(info.get(temp));
            }
        }
        sb.append("e");
        sb.insert(0,"d");
        System.out.println(sb);
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(sb.toString().getBytes(StandardCharsets.UTF_8));
            String sha1 = new BigInteger(1, crypt.digest()).toString(16);
            System.out.println(sha1);
        }
        catch(Exception e){

        }

        return null;

    }

    public int count_chars(String s){
        return s.length();
    }


    public boolean checkUTF8(byte[] barr){

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        ByteBuffer buf = ByteBuffer.wrap(barr);

        try {
            decoder.decode(buf);

        }
        catch(CharacterCodingException e){
            return false;
        }

        return true;
    }
}