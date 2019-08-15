

/**
 * Created by seodongmin on 2017-07-31.
 */

/**
 * @file name : TcpIpMultichatServer.java
 * @date : 2013. 9. 29.
 * @discription : Chatting Program - Server
 *
 */
//import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;



/**
 * @author Cremazer(cremazer@gmail.com)
 */
public class TcpIpMultichatServer {
    HashMap clients;
    HashMap waitingroom_pool;
    HashMap notipool;
    HashMap<String, UploadInfo> uploads;
    HashMap<String, RoomInfo> rooms;
    HashMap<String, String> userpool;
    HashMap<String, FireThread> fireduserpool;
    boolean gofourteen = false;
    public static final int DEFAULT_BUFFER_SIZE = 10000;

    TcpIpMultichatServer() {
        clients = new HashMap();
        waitingroom_pool = new HashMap();
        notipool = new HashMap();
        uploads = new HashMap<String, UploadInfo>();
        rooms = new LinkedHashMap<String, RoomInfo>();
        userpool = new HashMap<String, String>();
        fireduserpool = new HashMap<String, FireThread>();
        Collections.synchronizedMap(clients);
        Collections.synchronizedMap(waitingroom_pool);
        Collections.synchronizedMap(notipool);
        Collections.synchronizedMap(rooms);
    }

    public void start() {
        ServerSocket serverSocket = null;
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(7777);
            System.out.println("Server started!");
            while (true) {
                socket = serverSocket.accept();
                //           System.out.println("[" + socket.getInetAddress() + ":"
                //                   + socket.getPort() + "]" + ":from......");
                ServerReceiver thread = new ServerReceiver(socket);
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // start()


    void sendToAll(String msg) {
        Iterator it = clients.keySet().iterator();
        while (it.hasNext()) {
            try {
                DataOutputStream out = (DataOutputStream) clients
                        .get(it.next());
                out.writeUTF(msg);
            } catch (IOException e) {
            }
        } // while
    } // sendToAll

    void sendToWaitingRoom(String msg) {
        Iterator it = waitingroom_pool.keySet().iterator();
        while (it.hasNext()) {
            try {
                DataOutputStream out = (DataOutputStream) waitingroom_pool
                        .get(it.next());
                out.writeUTF(msg);
            } catch (IOException e) {
            }
        } // while
    } // sendToWaitingRoom


    void sendToNoti(String msg) {
        Iterator it = notipool.keySet().iterator();
        while (it.hasNext()) {
            try {
                DataOutputStream out = (DataOutputStream) notipool
                        .get(it.next());
                out.writeUTF(msg);
            } catch (IOException e) {
            }
        } // while
    } // sendToWaitingRoom



    public static void main(String args[]) {
        new TcpIpMultichatServer().start();
    }


    class ServerReceiver extends Thread {
        Socket socket;
        DataInputStream in;
        DataOutputStream out;
        ServerReceiver(Socket socket) {
            this.socket = socket;
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
            }
        }
        public void run() {
            String name = "";
            try {
                name = in.readUTF();
                //System.out.println("in: " + name);
                String[] rdata = name.split(":;:");

                if(rdata[0].equals("1") || rdata[0].equals("2")) {
                    System.out.println("name's structure: " + name);
                    try {
                        //rooms.get(rdata[1]).inCurrentPeople(rdata[3]); //connect to existing room
                        System.out.println("No " + rdata[1] + "'s current user(in): " + rooms.get(rdata[1]).getCurrent_people());
                        //sendToAll("4" + ":;:" + rdata[1] + ":;:" + rooms.get(rdata[1]).getCurrent_people() + ""); //connection
                    } catch (NullPointerException e) { //create room (creator)
                        RoomInfo roominfo;
                        try {
                            QuizThread quizThread = new QuizThread(rdata[1], rdata[5]);
                            quizThread.start();
                            List<RankingElement> rankingElements = new ArrayList<RankingElement>();
                            roominfo = new RoomInfo(rdata[1], rdata[2], rdata[3], rdata[4], rdata[5], Integer.parseInt(rdata[6]), Integer.parseInt(rdata[7]), quizThread, rankingElements);
                            rooms.put(rdata[1], roominfo);
                            sendToWaitingRoom("20");
                            System.out.println(rdata[1] + " is created!");
                            //Quiz Thread
                        } catch(NumberFormatException ne) {
                            System.out.println("closed room...");
                            sendToAll("8" + ":;:" + rdata[1] + ":;:" + rdata[3]);
                        }
                        //rooms.get(rdata[1]).inCurrentPeople(rdata[8]);
                        //sendToAll("5" + ":;:" + rdata[1] + ":;:" + rdata[2] + ":;:" + rdata[3] + ":;:" + rdata[4] + ":;:" + rdata[5] + ":;:" + rdata[6] + ":;:" + rdata[7]);
                        //System.out.println("5" + ":;:" + rdata[1] + ":;:" + rdata[2] + ":;:" + rdata[3] + ":;:" + rdata[4] + ":;:" + rdata[5] + ":;:" + rdata[6] + ":;:" + rdata[7]);
                    }
                    int witness = 0;
                    if(rdata[0].equals("1")) {
                        for(int i = 0; i < rooms.get(rdata[1]).getFired_member().size() ; i ++) {
                            if(rdata[3].equals(rooms.get(rdata[1]).getFired_member().get(i).getUserEmail())) {
                                sendToAll("" + name + ":;:in:;:1");
                                witness ++;
                                break;
                            }
                        }
                    }
                    if(witness == 0) {
                        sendToAll("" + name + ":;:in:;:0");
                    }
                    //#
                    //System.out.println("jeopjasu: " + rooms.get(rdata[1]).getCurrent_people());

                    clients.put(name, out);
                    System.out.println("(in)Current number of user is "
                            + clients.size() + "");
                    while (in != null) {
                        sendToAll(in.readUTF());
                    }
                } else if(rdata[0].equals("3")) {

                    //     System.out.println("badat seuni sil haeng jom hae bol kka???");
                    waitingroom_pool.put(rdata[1], out);
                    //   System.out.println("waitingroom_pool.put: " + rdata[1] + "///" + out);
                    Set<String> set = rooms.keySet();
                    Iterator<String> iter = set.iterator();
                    List<RoomInfo> imsi = new ArrayList<RoomInfo>();

                    while(iter.hasNext()) {
                        String key = iter.next();
                        RoomInfo value = rooms.get(key);
                        imsi.add(0, value);
                    }
                    //     System.out.println("imsi.size(): " + imsi.size());
                    String sending_data = "3";
                    for(int i = 0 ; i < imsi.size() ; i ++) {
                        String room_id = imsi.get(i).getRoom_id();
                        String room_name = imsi.get(i).getRoom_name();
                        String room_pw = imsi.get(i).getRoom_password();
                        String ptype = imsi.get(i).getPtype();
                        String qtype = imsi.get(i).getQtype();
                        String current_people = String.valueOf(imsi.get(i).getCurrent_people());
                        String max_people = String.valueOf(imsi.get(i).getMax_people());
                        sending_data += ":;:" + room_id + ":;:" + room_name + ":;:" + room_pw + ":;:" + ptype + ":;:" + qtype + ":;:" + current_people + ":;:" + max_people + ":;:" + String.valueOf(i) + ":::" + "3";
                    }
                    out.writeUTF(sending_data);
                }

                if (rdata[0].equals("5")) {
                    UserInfo inuser = new UserInfo();
                    inuser.setNowIn(rdata[1]);
                    inuser.setUserId(rdata[2]);
                    inuser.setUserEmail(rdata[3]);
                    inuser.setUserNickname(rdata[4]);
                    inuser.setUserPoints(rdata[5]);
                    inuser.setUserExp(rdata[6]);
                    inuser.setUserIntro(rdata[7]);
                    inuser.setUserProfileImgPath(rdata[8]);
                    inuser.setUserProfileImgPath_t(rdata[9]);
                    inuser.setUserNowUsingCharacter(rdata[10]);
                    //inuser.setUserProfileImg(rdata[8]);
                    inuser.setUserProfileImg("");
                    try {
                        if(userpool.get(rdata[3]).equals(rdata[3])) { //not roommaster
                            try {
                                userpool.put(rdata[3], rdata[3]);
                                rooms.get(rdata[1]).inCurrentPeople(inuser); //save in server
                                sendToWaitingRoom("20");
                                sendToAll("13" + ":;:" + rdata[1] + ":;:" + rdata[3]); //user already exists
                            } catch(NullPointerException ne) {
                                System.out.println("closed room...");
                                sendToAll("8" + ":;:" + rdata[1] + ":;:" + rdata[3]);
                            }
                            int fired = -1;
                            for(int i = 0; i < rooms.get(rdata[1]).getFired_member().size() ; i ++) {
                                if(rooms.get(rdata[1]).getFired_member().get(i).getUserEmail().equals(rdata[3])) {
                                    fired = 1;
                                    break;
                                } else {
                                    fired = 0;
                                }
                            }
                            sendToAll(name + ":;:" + String.valueOf(rooms.get(rdata[1]).start_people) + ":;:" + fired); //received data -> send to all
                            System.out.println("555name: " + name);
                            System.out.println("protocol 5 accepted");
                        }
                    } catch(NullPointerException e) { //roommaster(creator)'s
                        userpool.put(rdata[3], rdata[3]);
                        System.out.println("inner's email: " + rdata[3]);
                        try {
                            rooms.get(rdata[1]).inCurrentPeople(inuser); //save in server
                            sendToWaitingRoom("20");
                        } catch(NullPointerException ne) {
                            System.out.println("closed room...");
                            sendToAll("8" + ":;:" + rdata[1] + ":;:" + rdata[3]);
                        }
                        int fired = -1;
                        for(int i = 0; i < rooms.get(rdata[1]).getFired_member().size() ; i ++) {
                            if(rooms.get(rdata[1]).getFired_member().get(i).getUserEmail().equals(rdata[3])) {
                                fired = 1;
                                break;
                            } else {
                                fired = 0;
                            }
                        }
                        sendToAll(name + ":;:" + String.valueOf(rooms.get(rdata[1]).start_people) + ":;:" + fired); //received data -> send to all
                        System.out.println("555name: " + name);
                        System.out.println("protocol 5 accepted");
                        sendToWaitingRoom("20");
                    }
                }

                if(rdata[0].equals("6")) {
                    System.out.println("6 called!!");
                    String msg = "";
                    try {
                        for (int i = 0; i < rooms.get(rdata[1]).getCurrent_people(); i++) {
                            if(rooms.get(rdata[1]).getRoom_member().get(i).getUserEmail().equals(rdata[2])) {
                                continue;
                            }
                            String uid = rooms.get(rdata[1]).getRoom_member().get(i).getUserId();
                            String uemail = rooms.get(rdata[1]).getRoom_member().get(i).getUserEmail();
                            String unickname = rooms.get(rdata[1]).getRoom_member().get(i).getUserNickname();
                            String upoints = rooms.get(rdata[1]).getRoom_member().get(i).getUserPoints();
                            String uexp = rooms.get(rdata[1]).getRoom_member().get(i).getUserExp();
                            String uintro = rooms.get(rdata[1]).getRoom_member().get(i).getUserIntro();
                            String uprofileimg = rooms.get(rdata[1]).getRoom_member().get(i).getUserProfileImgPath();
                            String uprofileimg_t = rooms.get(rdata[1]).getRoom_member().get(i).getUserProfileImgPath_t();
                            //String uprofileimg = rooms.get(rdata[1]).getRoom_member().get(i).getUserProfileImg();
                            //String uprofileimg = "dd";
                            msg += "6" + ":;:" + rdata[1] + ":;:" + uid + ":;:" + uemail + ":;:" + unickname + ":;:" + upoints + ":;:" + uexp + ":;:" + uintro + ":;:" + uprofileimg + ":;:" + uprofileimg_t + ":::";
                        }
                        msg += rdata[2];
                        sendToAll(msg);
                        sendToWaitingRoom("20");
                    } catch(IndexOutOfBoundsException e) { System.out.println("IndexOutOfBound(otheruser)"); }
                }

                if(rdata[0].equals("14")) {
                    System.out.println("14 called / email: " + rdata[2]);
                    userpool.put(rdata[2], rdata[2]);
                    gofourteen = true;
                }

                if(rdata[0].equals("15")) {
                    System.out.println("15 called / email: " + rdata[2]);
                    rooms.get(rdata[1]).battle_triggered = true;
                    sendToAll("15" + ":;:" + rdata[1] + ":;:" + rdata[2] + ":;:" + rdata[3] + ":;:" + rdata[4]);
                }

                if(rdata[0].equals("16")) {
                    System.out.println("16 called / email: " + rdata[2]);
                    rooms.get(rdata[1]).go_battle = true;
                    rooms.get(rdata[1]).battle_finished = false;
                    sendToAll("16" + ":;:" + rdata[1] + ":;:" + rdata[2] + ":;:" + rdata[3]);
                }

                if(rdata[0].equals("18")) {
                    System.out.println("18 called");
                    if(rdata[2].equals("1")) {
                        rooms.get(rdata[1]).setRoom_name(rdata[3]);
                        sendToAll("18" + ":;:" + rdata[1] + ":;:" + "1" + ":;:" + rdata[3]);
                    } else if (rdata[2].equals("2")) {
                        if(rooms.get(rdata[1]).battle_triggered || rooms.get(rdata[1]).quiz_continuing) {
                            sendToAll("18" + ":;:" + rdata[1] + ":;:" + "2" + ":;:" + rdata[3] + ":;:" + rdata[4] + ":;:" + "1");
                        } else {
                            System.out.println("here occured");
                            rooms.get(rdata[1]).setQtype(rdata[3]);
                            rooms.get(rdata[1]).getQuizThread().interrupt();
                            QuizThread replace_quizthread = new QuizThread(rdata[1], rdata[3]);
                            replace_quizthread.start();
                            rooms.get(rdata[1]).setQuizThread(replace_quizthread);
                            sendToAll("18" + ":;:" + rdata[1] + ":;:" + "2" + ":;:" + rdata[3] + ":;:" + rdata[4] + ":;:" + "2");
                        }
                    } else if (rdata[2].equals("3")) {
                        rooms.get(rdata[1]).setPtype(rdata[3]);
                        if (rdata[3].equals("2")) {
                            rooms.get(rdata[1]).setRoom_password(rdata[4]);
                            sendToAll("18" + ":;:" + rdata[1] + ":;:" + "3" + ":;:" + "2" + ":;:" + rdata[4]);
                        } else {
                            sendToAll("18" + ":;:" + rdata[1] + ":;:" + "3" + ":;:" + "1");
                        }
                    } else if (rdata[2].equals("4")) {
                        if(rooms.get(rdata[1]).getCurrent_people() > Integer.parseInt(rdata[3])) {
                            sendToAll("18" + ":;:" + rdata[1] + ":;:" + "4" + ":;:" + rdata[3] + ":;:" + rdata[4] + ":;:" + "1");
                        } else {
                            rooms.get(rdata[1]).setMax_people(Integer.parseInt(rdata[3]));
                            sendToAll("18" + ":;:" + rdata[1] + ":;:" + "4" + ":;:" + rdata[3] + ":;:" + rdata[4] + ":;:" + "2");
                        }
                    } else {
                        System.out.println("unexpected error!");
                    }

                    sendToWaitingRoom("20");

                } else if(rdata[0].equals("19")) {
                    rooms.get(rdata[1]).battle_triggered = false;
                    sendToAll("19" + ":;:" + rdata[1] + ":;:" + rdata[2] + ":;:" + rdata[3]);
                }

                if(rdata[0].equals("21")) {
                    sendToWaitingRoom("20");
                }

                if(rdata[0].equals("22")) {
                    rooms.get(rdata[1]).battle_finished = true;
                    rooms.get(rdata[1]).battle_triggered = false;
                    rooms.get(rdata[1]).go_battle = false;
                    sendToAll("22" + ":;:" + rdata[1] + ":;:" + rdata[2] + ":;:" + rdata[3]);
                }

                if(rdata[0].equals("23")) {
                    rooms.get(rdata[1]).start_people = Integer.valueOf(rdata[2]);
                    System.out.println("start people: " + rdata[2]);
                }

                if(rdata[0].equals("25")) {
                    System.out.println("This is Jjokji for: " + rdata[1]);
                    notipool.put(rdata[1], out);
                    System.out.println("25~!~!: " + rdata[1] + "///" + out);
                    sendToWaitingRoom("25" + ":;:" + rdata[1]);
                    sendToNoti("25" + ":;:" + rdata[1]);
                    System.out.println("goood!");
                }

                if(rdata[0].equals("26")) {

                    System.out.println("26 received");
                    Set<String> set = rooms.keySet();
                    Iterator<String> iter = set.iterator();
                    List<RoomInfo> imsi = new ArrayList<RoomInfo>();

                    while(iter.hasNext()) {
                        String key = iter.next();
                        RoomInfo value = rooms.get(key);
                        imsi.add(0, value);
                    }
                    System.out.println("imsi.size(): " + imsi.size());
                    String sending_data = "26";
                    for(int i = 0 ; i < imsi.size() ; i ++) {
                        int witness;
                        switch(Integer.parseInt(rdata[1])) {
                            case 0:
                                System.out.println("0 case gogo");
                                if(!imsi.get(i).getRoom_name().contains(rdata[2]))
                                    continue;
                                break;
                            case 1:
                                System.out.println("1 case gogo");
                                if(!imsi.get(i).getQtype().equals(rdata[2]))
                                    continue;
                                break;
                            case 2:
                                System.out.println("2 case gogo");
                                witness = 0;
                                for(int j = 0; j < imsi.get(i).getRoom_member().size() ; j ++) {
                                    if(imsi.get(i).getRoom_member().get(j).getUserNickname().equals(rdata[2])) {
                                        witness ++;
                                        break;
                                    }
                                }
                                if(witness == 0)
                                    continue;
                                break;
                            case 3:
                                System.out.println("3 case gogo");
                                witness = 0;
                                for(int j = 0; j < imsi.get(i).getRoom_member().size() ; j ++) {
                                    if(imsi.get(i).getRoom_member().get(j).getUserEmail().equals(rdata[2])) {
                                        witness ++;
                                        break;
                                    }
                                }
                                if(witness == 0)
                                    continue;
                                break;
                            default:
                                break;
                        }

                        String room_id = imsi.get(i).getRoom_id();
                        String room_name = imsi.get(i).getRoom_name();
                        String room_pw = imsi.get(i).getRoom_password();
                        String ptype = imsi.get(i).getPtype();
                        String qtype = imsi.get(i).getQtype();
                        String current_people = String.valueOf(imsi.get(i).getCurrent_people());
                        String max_people = String.valueOf(imsi.get(i).getMax_people());
                        sending_data += ":;:" + room_id + ":;:" + room_name + ":;:" + room_pw + ":;:" + ptype + ":;:" + qtype + ":;:" + current_people + ":;:" + max_people + ":;:" + String.valueOf(i) + ":::" + "3";
                    }
                    out.writeUTF(sending_data);

                }

                if(rdata[0].equals("30")) {
                    System.out.println("30 received");
                    String qtype = rooms.get(rdata[1]).getQtype();
                    String ptype = rooms.get(rdata[1]).getPtype();
                    String room_name = rooms.get(rdata[1]).getRoom_name();
                    int current_people = rooms.get(rdata[1]).getCurrent_people();
                    int max_people = rooms.get(rdata[1]).getMax_people();
                    out.writeUTF("30" + ":;:" + rdata[2] + ":;:" +  room_name + ":;:" + current_people + ":;:" + max_people + ":;:" + ptype + ":;:" + qtype);
                }

                if(rdata[0].equals("31")) {
                    int witness = 0;
                    System.out.println("31 received");
                    for(int i = 0; i < rooms.get(rdata[1]).getRoom_member().size() ; i ++) {
                        if(rooms.get(rdata[1]).getRoom_member().get(i).getUserEmail().equals(rdata[2])) {
                            out.writeUTF("31" + ":;:" + "1");
                            witness ++;
                            break;
                        }
                    }
                    if(witness == 0) {
                        out.writeUTF("31" + ":;:" + "0");
                    }
                }

                if(rdata[0].equals("33")) {
                    System.out.println("33 received");
                    for(int i = 0; i < rooms.get(rdata[1]).getRoom_member().size() ; i ++) {
                        if(rooms.get(rdata[1]).getRoom_member().get(i).getUserEmail().equals(rdata[2])) {
                            rooms.get(rdata[1]).getRoom_member().get(i).setUserNowUsingCharacter(rdata[3]);
                            sendToAll("33" + ":;:" + rdata[1] + ":;:" + rdata[2] + ":;:" + rdata[3]);
                        }
                    }
                }

                if(rdata[0].equals("44")) {
                    System.out.println("44 received");
                    for(int i = 0 ; i < rooms.get(rdata[1]).getRoom_member().size() ; i ++) {
                        if(rooms.get(rdata[1]).getRoom_member().get(i).getUserEmail().equals(rdata[2])) {
                            rooms.get(rdata[1]).getFired_member().add(rooms.get(rdata[1]).getRoom_member().get(i));
                            FireThread fireThread = new FireThread(rdata[1], rdata[2]);
                            fireThread.start();
                            fireduserpool.put(rdata[2], fireThread);
                        }
                    }
                    sendToAll("44" + ":;:" + rdata[1] + ":;:" + rdata[2]);
                }

                if(rdata[0].equals("100")) {
                    System.out.println("100 received");
                    String sending_data = "100" + ":;:" + rdata[1];
                    for(int i = 0; i < rooms.get(rdata[1]).getRoom_member().size() ; i ++) {
                        String email = rooms.get(rdata[1]).getRoom_member().get(i).getUserEmail();
                        String nickname = rooms.get(rdata[1]).getRoom_member().get(i).getUserNickname();
                        String nowusingcharacter = rooms.get(rdata[1]).getRoom_member().get(i).getUserNowUsingCharacter();
                        String x = String.valueOf(rooms.get(rdata[1]).getRoom_member().get(i).destinationX);
                        String y = String.valueOf(rooms.get(rdata[1]).getRoom_member().get(i).destinationY);
                        sending_data += ":;:" + email + "::" + nickname + "::" + nowusingcharacter + "::" + x + "::" + y;
                    }
                    System.out.println("100 sent data: " + sending_data);
                    sendToAll(sending_data);
                }

                if(rdata[0].equals("101")) {

                    System.out.println("101 received");
                    for(int i = 0; i < rooms.get(rdata[1]).getRoom_member().size() ; i ++) {
                        if(rdata[2].equals(rooms.get(rdata[1]).getRoom_member().get(i).getUserEmail())) {
                            rooms.get(rdata[1]).getRoom_member().get(i).destinationX = Float.parseFloat(rdata[3]);
                            rooms.get(rdata[1]).getRoom_member().get(i).destinationY = Float.parseFloat(rdata[4]);
                        }
                    }
                    sendToAll("101" + ":;:" + rdata[1] + ":;:" + rdata[2] + ":;:" + rdata[3] + ":;:" + rdata[4]);
                }

                if(rdata[0].equals("1000")) { //upload~
                    final String[] rdata2 = rdata;

                    Thread uthread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                InetSocketAddress isaClient = (InetSocketAddress) socket.getRemoteSocketAddress();

                                System.out.println("A client("+isaClient.getAddress().getHostAddress()+
                                        ") is connected. (Port: " +isaClient.getPort() + ")");

                                String filename = rdata2[2];
                                long fileSize = Long.parseLong(rdata2[3]);
                                System.out.println("filename: " + filename);


                                FileOutputStream fos = new FileOutputStream(filename);
                                InputStream is = socket.getInputStream();

                                long totalReadBytes = 0;
                                double startTime = System.currentTimeMillis();
                                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                                int readBytes;int dan;

                               if(fileSize < 1024 * 1024) {
                                    dan = 1;
                                } else {
                                    dan = 2;
                                }

                                DecimalFormat df = new DecimalFormat("0.00");
                                String goal;
                                if(dan == 1) {
                                    goal = df.format((double) fileSize / (double) 1024);
                                } else {
                                    goal = df.format((double) fileSize / (double) (1024 * 1024));
                                }


                                long ccounter = 0;
                                try {
                                    while ((readBytes = is.read(buffer)) != -1) {
                                        String s = new String(buffer);
                                        if(s.contains("paused!")) {
                                            uploads.get(filename).setPaused(true);
                                        }
                                        if(uploads.get(filename).isPaused()) {
                                            System.out.println("Paused");
                                            while(true) {
                                                try {
                                                    Thread.sleep(500);
                                                    if(!uploads.get(filename).isPaused()) {
                                                        break;
                                                    }
                                                } catch(InterruptedException e) {}
                                            }
                                        }
                                        fos.write(buffer, 0, readBytes);
                                        totalReadBytes += readBytes;

										if(((double)totalReadBytes * 100 / (double)fileSize) > 100.00) {
											  System.out.println("Uploading...: " + fileSize + "/"
                                                + fileSize + " Byte(s) ("
                                                + ((double)fileSize * 100 / (double)fileSize) + " %)");
										} else {
											  System.out.println("Uploading...: " + totalReadBytes + "/"
                                                + fileSize + " Byte(s) ("
                                                + ((double)totalReadBytes * 100 / (double)fileSize) + " %)");
										}
                                      



                                        if(s.contains("fin!") || uploads.get(filename).isCanceled()) {
                                            break;
                                        }

                                        if(ccounter % 1000 == 0) {
                                            if(dan == 1) {
                                                out.writeUTF("1001" + ":;:" + "1" + ":;:" + df.format((double)totalReadBytes / 1024.00) + ":;:" + goal);
                                            } else {
                                                out.writeUTF("1001" + ":;:" + "2" + ":;:" + df.format((double)totalReadBytes / (1024.00 * 1024.00)) + ":;:" + goal);
                                            }
                                        }
                                        uploads.get(filename).setBytes(totalReadBytes);
                                        ccounter ++;
                                    }

                                    double endTime = System.currentTimeMillis();
                                    double diffTime = (endTime - startTime)/ 1000;

                                    if(!filename.contains("thumbnail")) {
                                        System.out.println("Transfer Complete");
                                    }
                                    //System.out.println("time: " + diffTime+ " second(s)");
                                    out.writeUTF("1000" + ":;:" + rdata2[1]);
                                } catch(SocketException e ) {}

                                //is.close();
                                //fos.close();
                                //socket.close();
                            } catch(IOException e) { e.printStackTrace(); }
                        }
                    });

                    UploadInfo uinfo = new UploadInfo();
                    uinfo.setThread(uthread);
                    uinfo.setPaused(false);
                    uploads.put(rdata2[2], uinfo);
                    uthread.start();
                }

                if(rdata[0].equals("1002")) {
                 //   System.out.println("1002 received. filename: " + rdata[1] + "... Paused???");
                    uploads.get(rdata[1]).setPaused(true);
                }

                if(rdata[0].equals("1003")) {
                 //   System.out.println("1003 received. filename: " + rdata[1] + "...Resume???");
                    uploads.get(rdata[1]).setPaused(false);
                }

                if(rdata[0].equals("1004")) {
                //    System.out.println("1004 received. filename: " + rdata[1] + "...Resume???");
                    uploads.get(rdata[1]).setCanceled(true);
                }


                if(rdata[0].equals("99")) {
                    throw new IOException();
                }

            } catch (IOException e) {
                // ignore
            } finally {
                try
                {
                    String[] rdata = name.split(":;:");
                    if(rdata[0].equals("2")) { //no.2 protocol (room creator's out)
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(System.currentTimeMillis());
                        SimpleDateFormat atime_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        SimpleDateFormat time_format = new SimpleDateFormat("a h:mm");
                        String atime = atime_format.format(cal.getTime());
                        String time = time_format.format(cal.getTime());

                        System.out.println("out(No2): " + name);
                        System.out.println("No " + rdata[1] + "'s current user(out(No2)): " + rooms.get(rdata[1]).getCurrent_people());

                        sendToAll("" + "1" + ":;:" +  rdata[1] + ":;:" + "2" + ":;:" +  rdata[8] + ":;:" + rdata[9] + ":;:" + atime + ":;:" + time + ":;:" + rdata[9] + ":;:" + "out");
                        clients.remove(name);
                        if(!gofourteen) {
                            userpool.remove(rdata[8]); } else {
                            gofourteen = false;
                        }
                        System.out.println("outer's email: " + rdata[8]);

                        try {
                            for (int i = rooms.get(rdata[1]).getRoom_member().size() - 1; i >= 0; i--) {
                                if (rooms.get(rdata[1]).getRoom_member().get(i).getUserEmail().equals(rdata[8])) {
                                    rooms.get(rdata[1]).getRoom_member().remove(i);
                                }
                                break;
                            }
                        } catch(IndexOutOfBoundsException e) { System.out.println("OutOfBounds(out, creator)"); }

                        sendToAll("7" + ":;:" + rdata[1] + ":;:" + rdata[8]);
                        System.out.println("[" + socket.getInetAddress() + ":"
                                + socket.getPort() + "]"
                                + ": from...");
                        System.out.println("(out(No2))Current number of user is "
                                + clients.size() + "");
                        rooms.get(rdata[1]).outCurrentPeople(rdata[8]);
                        sendToWaitingRoom("20");

                        //sendToAll("4:;:" + ":;:" + rdata[1] + ":;:" + rooms.get(rdata[1]).getCurrent_people() + "");
                        //System.out.println("jeopjasu: " + rooms.get(rdata[1]).getCurrent_people());

                    } else if(rdata[0].equals("1")) { //if not room creator

                        System.out.println("out: " + name);
                       // System.out.println("ssibal");
                        System.out.println("No " + rdata[1] + "'s current user(out): " + rooms.get(rdata[1]).getCurrent_people());
                      //  System.out.println("nono");
                        int witness = 0;
                        for(int i = 0 ; i < rooms.get(rdata[1]).getFired_member().size() ; i ++) {
                            if(rdata[3].equals(rooms.get(rdata[1]).getFired_member().get(i).getUserEmail())) {
                                sendToAll("" + name + ":;:out:;:1");
                                witness ++;
                                break;
                            }
                        }
                        if(witness == 0) {
                            sendToAll("" + name + ":;:out:;:0"); //#
                        }
                        //System.out.println("wow");
                        clients.remove(name);
                        if(!gofourteen) {
                            userpool.remove(rdata[3]); } else {
                            gofourteen = false;
                        }

                        try {
                            for (int i = rooms.get(rdata[1]).getRoom_member().size() - 1; i >= 0; i--) {
                                if (rooms.get(rdata[1]).getRoom_member().get(i).getUserEmail().equals(rdata[3])) {
                                    rooms.get(rdata[1]).getRoom_member().remove(i);
                                }
                                break;
                            }
                        } catch(IndexOutOfBoundsException e) { System.out.println("OutOfBounds(out)"); }

                        sendToAll("7" + ":;:" + rdata[1] + ":;:" + rdata[3]);

                        System.out.println("GREAT");
                        System.out.println("[" + socket.getInetAddress() + ":"
                                + socket.getPort() + "]"
                                + ": from...");
                        System.out.println("(out)Current number of user is "
                                + clients.size() + "");
                        rooms.get(rdata[1]).outCurrentPeople(rdata[3]);
                        sendToWaitingRoom("20");
                        //sendToAll("4:;:" + rdata[1] + ":;:" + rooms.get(rdata[1]).getCurrent_people() + "");
                    } else if(rdata[0].equals("11")) {
                        RankingElement re = new RankingElement(rdata[3], rdata[4]);
                        if(rdata[5].equals("1") && rooms.get(rdata[1]).go_battle && !rooms.get(rdata[1]).battle_finished) {
                            System.out.println("battler first arrived");
                            rooms.get(rdata[1]).go_battle = false;
                            rooms.get(rdata[1]).battle_finished = true;
                            sendToAll("17" + ":;:" + rdata[1] + ":;:" + rdata[2] + ":;:" + rdata[3]);
                        }
                        System.out.println(rdata[2]);
                        rooms.get(rdata[1]).getRanking().add(re);
                    } else if(rdata[0].equals("12")) {
                        rooms.get(rdata[1]).quiz_continuing = false;
                        String msg = "12:;:" + rdata[1] + ":;:";
                        for(int i = 0 ; i < rooms.get(rdata[1]).getRanking().size() ; i ++) {
                            msg += rooms.get(rdata[1]).getRanking().get(i).getNickname() + ";:;" + rooms.get(rdata[1]).getRanking().get(i).getRecord() + ":::";
                            System.out.println(rooms.get(rdata[1]).getRanking().get(i).getNickname());
                        }
                        sendToAll(msg);
                    }
                }
                catch (NullPointerException e) {
                }
            } // try
        } // run
    } // ReceiverThread

    public class UploadInfo {
        Thread thread;
        long bytes;
        boolean isPaused = false;
        boolean isCanceled = false;

        public Thread getThread() {
            return thread;
        }

        public void setThread(Thread thread) {
            this.thread = thread;
        }

        public long getBytes() {
            return bytes;
        }

        public void setBytes(long bytes) {
            this.bytes = bytes;
        }

        public boolean isPaused() {
            return isPaused;
        }

        public void setPaused(boolean paused) {
            isPaused = paused;
        }

        public boolean isCanceled() {
            return isCanceled;
        }

        public void setCanceled(boolean canceled) {
            isCanceled = canceled;
        }
    }

    public class UserInfo {
        String nowIn;
        String userId;
        String userEmail;
        String userPassword;
        String userNickname;
        String userPoints;
        String userExp;
        String userIntro;
        String userProfileImgPath;
        String userProfileImgPath_t;
        String userProfileImg;
        String userNowUsingCharacter;
        float positionX;
        float positionY;
        float destinationX;
        float destinationY;

        public UserInfo() {
            positionX = 0.5f;
            positionY = 0.5f;
            destinationX = 0.5f;
            destinationY = 0.5f;
        }

        public String getNowIn() {
            return nowIn;
        }

        public void setNowIn(String nowIn) {
            this.nowIn = nowIn;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public String getUserPassword() {
            return userPassword;
        }

        public void setUserPassword(String userPassword) {
            this.userPassword = userPassword;
        }

        public String getUserNickname() {
            return userNickname;
        }

        public void setUserNickname(String userNickname) {
            this.userNickname = userNickname;
        }

        public String getUserPoints() {
            return userPoints;
        }

        public void setUserPoints(String userPoints) {
            this.userPoints = userPoints;
        }

        public String getUserExp() {
            return userExp;
        }

        public void setUserExp(String userExp) {
            this.userExp = userExp;
        }

        public String getUserIntro() {
            return userIntro;
        }

        public void setUserIntro(String userIntro) {
            this.userIntro = userIntro;
        }

        public String getUserProfileImg() {
            return userProfileImg;
        }

        public void setUserProfileImg(String userProfileImg) {
            this.userProfileImg = userProfileImg;
        }

        public String getUserProfileImgPath() {
            return userProfileImgPath;
        }

        public void setUserProfileImgPath(String userProfileImgPath) {
            this.userProfileImgPath = userProfileImgPath;
        }

        public String getUserProfileImgPath_t() {
            return userProfileImgPath_t;
        }

        public void setUserProfileImgPath_t(String userProfileImgPath_t) {
            this.userProfileImgPath_t = userProfileImgPath_t;
        }

        public String getUserNowUsingCharacter() {
            return userNowUsingCharacter;
        }

        public void setUserNowUsingCharacter(String userNowUsingCharacter) {
            this.userNowUsingCharacter = userNowUsingCharacter;
        }
    }

    public class FireThread extends Thread {

        private String room_id;
        private String fired_member;

        public FireThread(String room_id, String fired_member) {
            this.room_id = room_id;
            this.fired_member = fired_member;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(30000);
                for(int i = 0; i < rooms.get(room_id).getFired_member().size() ; i ++) {
                    if(rooms.get(room_id).getFired_member().get(i).getUserEmail().equals(fired_member)) {
                        rooms.get(room_id).getFired_member().remove(i);
                    }
                }
            } catch(InterruptedException e) { e.printStackTrace(); }
        }
    }
    public class QuizThread extends Thread {

        private String room_id;
        private String qtype;

        public QuizThread(String room_id, String qtype) {
            this.room_id = room_id;
            this.qtype = qtype;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(20000);
            } catch(InterruptedException e) { return; }
            while(true) {
                try {

                    String param = "qtype=" + qtype;
                    String result = "";
                    try {
                        URL url = new URL("http://dak2183242.cafe24.com/fetch_question.php?qtype=" + qtype);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        conn.setRequestMethod("GET");
                        conn.setRequestMethod("POST");
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.connect();

                        //android->server
                        OutputStream outs = conn.getOutputStream();
                        outs.write(param.getBytes("UTF-8"));
                        outs.flush();
                        outs.close();

                        //server->android
                        InputStream is = null;
                        BufferedReader in = null;
                        String data = "";

                        conn.disconnect();
                        conn.getInputStream();

                        is = conn.getInputStream();
                        in = new BufferedReader(new InputStreamReader(is), 8 * 1024);
                        String line = null;
                        StringBuffer buff = new StringBuffer();
                        while((line = in.readLine() ) != null) {
                            buff.append(line + "\n");
                            break;
                        }

                        data = buff.toString().trim();

                        if(data.equals("ERROR")) {

                            result = "ERROR";
                        }
                        else if(data.equals("0"))
                        {
                            result = "0";
                        } else {
                            result = data;
                        }
                    } catch(MalformedURLException e)
                    {
                        e.printStackTrace();
                    } catch(IOException e)
                    {
                        e.printStackTrace();
                    }

                    System.out.println("Q and A: " + result);
                    String[] yogo = result.split(":;:");
                    rooms.get(room_id).getRanking().clear(); //ranking clear
                    rooms.get(room_id).quiz_continuing = true;
                    rooms.get(room_id).battle_finished = false;
                    rooms.get(room_id).battle_triggered = false;
                    sendToAll("10" + ":;:" + room_id + ":;:" + yogo[1] + ":;:" + yogo[2] + ":;:" + yogo[0]);
                    Thread.sleep(40000);
                } catch(InterruptedException e) { System.out.println("room exploded"); sendToWaitingRoom("20");  break; }
            }
        }
    }


    public class RankingElement {
        String nickname;
        String record;

        public RankingElement(String nickname, String record) {
            this.nickname = nickname;
            this.record = record;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getRecord() {
            return record;
        }

        public void setRecord(String record) {
            this.record = record;
        }
    }

    public class RoomInfo {
        String room_id;
        String room_name;
        String room_password;
        String ptype;
        String qtype;
        List<UserInfo> room_member;
        Thread quizThread;
        List<RankingElement> ranking;
        List<UserInfo> fired_member;
        boolean battle_triggered;
        boolean quiz_continuing;
        boolean go_battle;
        boolean battle_finished;

        int start_people;
        int current_people;
        int max_people;


        public RoomInfo(String room_id, String room_name, String room_password, String ptype, String qtype, int current_people, int max_people, QuizThread quizThread, List<RankingElement> ranking) {
            this.start_people = 0;
            this.room_id = room_id;
            this.room_name = room_name;
            this.room_password = room_password;
            this.ptype = ptype;
            this.qtype = qtype;
            this.current_people = current_people;
            this.max_people = max_people;
            this.room_member = new ArrayList<UserInfo>();
            this.fired_member = new ArrayList<UserInfo>();
            this.quizThread = quizThread;
            this.ranking = ranking;
            battle_triggered = false;
            quiz_continuing = false;
            go_battle = false;
            battle_finished = false;
        }

        public String getRoom_id() {
            return room_id;
        }

        public void setRoom_id(String room_id) {
            this.room_id = room_id;
        }

        public String getRoom_name() {
            return room_name;
        }

        public void setRoom_name(String room_name) {
            this.room_name = room_name;
        }

        public String getRoom_password() {
            return room_password;
        }

        public void setRoom_password(String room_password) {
            this.room_password = room_password;
        }

        public String getPtype() {
            return ptype;
        }

        public void setPtype(String ptype) {
            this.ptype = ptype;
        }

        public String getQtype() {
            return qtype;
        }

        public void setQtype(String qtype) {
            this.qtype = qtype;
        }

        public int getCurrent_people() {
            return current_people;
        }

        public void setCurrent_people(int current_people) {
            this.current_people = current_people;
        }

        public int getMax_people() {
            return max_people;
        }

        public void setMax_people(int max_people) {
            this.max_people = max_people;
        }

        public List<UserInfo> getRoom_member() {
            return room_member;
        }

        public void setRoom_member(List<UserInfo> room_member) {
            this.room_member = room_member;
        }

        public List<UserInfo> getFired_member() {
            return fired_member;
        }

        public void setFired_member(List<UserInfo> fired_member) {
            this.fired_member = fired_member;
        }

        public void inCurrentPeople(UserInfo userInfo) {
            this.room_member.add(userInfo);
            this.current_people ++;
        }

        public void outCurrentPeople(String email) {
            try {
                for (int i = 0; i < this.room_member.size() ; i ++) {
                    if(this.room_member.get(i).getUserEmail().equals(email)) {
                        room_member.remove(i);
                        break;
                    }
                }
            } catch(IndexOutOfBoundsException e ) { System.out.println("OutOfBoundsError(Out)"); e.printStackTrace(); }
            this.current_people --;
            if(current_people < 1) {
                try {
                    rooms.get(this.room_id).getQuizThread().interrupt();
                } catch (IllegalStateException e ) { System.out.println("(last out) not started thread?"); }
                rooms.remove(this.room_id);
            }
        }

        public Thread getQuizThread() {
            return quizThread;
        }

        public void setQuizThread(Thread quizThread) {
            this.quizThread = quizThread;
        }

        public List<RankingElement> getRanking() {
            return ranking;
        }

        public void setRanking(List<RankingElement> ranking) {
            this.ranking = ranking;
        }
    }


}
