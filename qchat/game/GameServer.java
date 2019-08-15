

/**
 * Created by seodongmin on 2017-07-31.
 * Modified by seodongmin on 2019-06-27 10:36.
 */

//import android.support.annotation.NonNull;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
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


public class GameServer {

    private DecimalFormat df;
   // private HashMap<String, ServerReceiver> clients;
    private HashMap<String, RoomInfo> rooms;
    private HashMap<String, Socket> waitingRoomPool;

    private HashMap<String, UploadInfo> uploads;

    private HashMap<String, String> userpool;
    private HashMap<String, FireThread> fireduserpool;

    private GameServer() {

        df = new DecimalFormat("0.000");
        rooms = new LinkedHashMap<String, RoomInfo>();
        waitingRoomPool = new HashMap<String, Socket>();

        uploads = new HashMap<String, UploadInfo>();

        userpool = new HashMap<String, String>();
        fireduserpool = new HashMap<String, FireThread>();

        Collections.synchronizedMap(waitingRoomPool);
        Collections.synchronizedMap(rooms);
    }

    private void start() {
        ServerSocket serverSocket = null;
        Socket socket = null;

        try {
            serverSocket = new ServerSocket(7777);
            serverSocket.setSoTimeout(2100000000);
            System.out.println("Game Server started!");
            while (true) {
                socket = serverSocket.accept();
                System.out.println("MY PORT: " + socket.getPort());
                ServerReceiver thread = new ServerReceiver(socket);
                thread.start();
               // clients.put(String.valueOf(socket.getPort()), thread);
                System.out.println("새로운 소켓! port 번호: " + socket.getPort());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // start()

    private void sendToRoom(String roomId, String msg) {
        Iterator it = rooms.get(roomId).getRoomMember().keySet().iterator();
        while(it.hasNext()) {
            try {
                new DataOutputStream(rooms.get(roomId).getRoomMember().get(it.next()).userSocket.getOutputStream()).writeUTF(msg);
            } catch(SocketException e) {
                sendToWaitingRoom("3-003:;:3:;:" + roomId); //방 삭제 사실을 알림
                rooms.remove(roomId);
            } catch(IOException e) {
                e.printStackTrace();
            } catch(NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendToWaitingRoom(String msg) {
        Iterator it = waitingRoomPool.keySet().iterator();
        while (it.hasNext()) {
            try {
                new DataOutputStream(waitingRoomPool.get(it.next()).getOutputStream()).writeUTF(msg);
            } catch (IOException e) {
                e.printStackTrace();
            } catch(NullPointerException e) {
                e.printStackTrace();
            }
        } // while
    } // sendToWaitingRoom

    public static void main(String args[]) {
        new GameServer().start();
    }

    class ServerReceiver extends Thread {

        final int TYPE_WAITING_ROOM = 1;
        final int TYPE_CHATTING_ROOM = 2;

        int socketType = 0;
        String userNo;
        String userNickname;
        String nowIn;

        Socket socket;          //accept 한 소켓
        DataInputStream dIn;    //인풋스트림
        DataOutputStream dOut;  //아웃풋스트림

        ServerReceiver(Socket socket) {
            this.socket = socket;
            try {
                dIn = new DataInputStream(socket.getInputStream());
                dOut = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run() {
            String receivedData = "";
            //핑 체크 스레드
            new Thread() {
                @Override
                public void run() {

                    System.out.println("ping thread started");

                    try {
                        while(true) {
                            Thread.sleep(5000);
                            dOut.writeUTF("ping");
                            System.out.println("ping operated");
                        }
                    } catch(Exception e) {
                        System.out.println("out user(IO)");
                        switch(socketType) {
                            case TYPE_WAITING_ROOM : {
                                waitingRoomPool.remove(userNo);
                            }
                            case TYPE_CHATTING_ROOM : {
                                rooms.get(nowIn).leaveRoom(userNo, userNickname);
                            }
                        }
                    }
                }
            }.start();

            while(true) {

                try {
                    if(dIn.available() > 0) {
                        receivedData = dIn.readUTF();
                        if(receivedData.equals("ACK_SOCKET_CLOSED")) {
                            throw new SocketException();
                        }

                        String[] readData = receivedData.split(":;:");
                        System.out.println(readData[0] + " RECEIVED: " + receivedData);

                        //1

                        if(readData[0].equals("1-001")) {   //user creates room
                            //          0       1        2        3              4       5           6       7       8
                            //RECEIVED: 1-001:;:userNo:;;userId:;:userNickname:;:title:;:maxPeople:;:qType:;:pType:;:password
                            String cTime = String.valueOf(System.currentTimeMillis());
                            String roomId = cTime + readData[2];    //밀리세컨드 타임과 유저아이디의 결합: 방번호 중복을 없앰


                            //room creator also enter this room
                            HashMap<String, UserInfo> roomMember = new HashMap<String, UserInfo>();
                            UserInfo userInfo = new UserInfo(roomId, readData[1], readData[3], socket);
                            this.userNo = readData[1];
                            this.userNickname = readData[3];
                            this.socketType = TYPE_CHATTING_ROOM;
                            this.nowIn = roomId;
                            roomMember.put(userInfo.userNo, userInfo);
                            String password = (readData[7].equals("private")) ? readData[8] : "";
                            RoomInfo roomInfo = new RoomInfo(roomId, readData[1], cTime,
                                    readData[4], password, readData[7], readData[6], Integer.parseInt(readData[5]), roomMember);


                            //TO SEND: 1-001:;:cTime
                            dOut.writeUTF("1-001:;:" + cTime);

                            //TO SEND: 3-003:;:1(create room):;:roomInfo
                            JSONObject roomObj = new JSONObject();
                            roomObj.put("room_id", roomInfo.roomId);
                            roomObj.put("c_time", roomInfo.cTime);
                            roomObj.put("title", roomInfo.title);
                            roomObj.put("password", roomInfo.password);
                            roomObj.put("p_type", roomInfo.pType);
                            roomObj.put("q_type", roomInfo.qType);
                            roomObj.put("current_people", String.valueOf(roomInfo.currentPeople));
                            roomObj.put("max_people", String.valueOf(roomInfo.maxPeople));
                            sendToWaitingRoom("3-003:;:1:;:" +  roomObj.toString());

                        } else if(readData[0].equals("1-002")) {    //user enters already created room
                            //          0       1        2        3        4
                            //RECEIVED: 1-002:;:roomId:;:userNo:;:userId:;:userNickname
                            UserInfo userInfo = new UserInfo(readData[1], readData[2], readData[4], socket);
                            this.userNo = readData[2];
                            this.userNickname = readData[4];
                            this.socketType = TYPE_CHATTING_ROOM;
                            this.nowIn = readData[1];

                            //enter room
                            RoomInfo room = rooms.get(readData[1]);
                            room.enterRoom(userInfo);

                            //TO SEND: 1-002:;:roomInfo:;:character:;:character:;:...
                            RoomInfo roomInfo = rooms.get(readData[1]);
                            JSONObject roomObj = new JSONObject();

                            try {
                                rooms.get(readData[1]).getRoomId(); //없어진 방인지 검사
                            } catch(NullPointerException e) {
                                try {
                                    new DataOutputStream(waitingRoomPool.get(userInfo.userNo).getOutputStream()).writeUTF("3-004:;:" + readData[1]);
                                } catch(IOException e2) {
                                    return;
                                }
                                return;
                            }

                            roomObj.put("room_id", roomInfo.roomId);
                            roomObj.put("c_time", roomInfo.cTime);
                            roomObj.put("title", roomInfo.title);
                            roomObj.put("password", roomInfo.password);
                            roomObj.put("p_type", roomInfo.pType);
                            roomObj.put("q_type", roomInfo.qType);
                            roomObj.put("current_people", String.valueOf(roomInfo.currentPeople));
                            roomObj.put("max_people", String.valueOf(roomInfo.maxPeople));

                            HashMap<String, UserInfo> roomMember = roomInfo.roomMember;
                            StringBuilder charactersInfo = new StringBuilder();
                            Iterator it = roomMember.keySet().iterator();
                            while(it.hasNext()) {
                                UserInfo user = roomMember.get(it.next());
                                JSONObject characterObj = new JSONObject();
                                characterObj.put("user_no", String.valueOf(user.userNo));
                                characterObj.put("position_x", String.valueOf(user.positionX));
                                characterObj.put("position_y", String.valueOf(user.positionY));
                                charactersInfo.append(characterObj.toString());
                                if(it.hasNext()) charactersInfo.append(":;:");
                            }



                            System.out.println("USER NO" + readData[2] + "'s ENTERED ROOM INFO: " + roomObj.toString());
                            System.out.println("EXIST CHARACTER INFO" + charactersInfo.toString());
                            dOut.writeUTF("1-002:;:" + roomObj.toString() + ":;:" + charactersInfo.toString()); //send room info to entered user

                            //TO SEND: 1-003:;:userNo
                            sendToRoom(readData[1], "1-003:;:" + readData[2]);
                        } else if(readData[0].equals("1-005")) {    //user sends chat message
                            //          0       1        2        3              4
                            //RECEIVED: 1-005:;:roomId:;:userNo:;:userNickname:;:message
                            //TO SEND: 1-005:;:userNo:;:message
                            sendToRoom(readData[1], "1-005:;:" + readData[2] + ":;:" + readData[3] + ":;:" + readData[4]);
                        } else if(readData[0].equals("1-006")) {
                            //          0       1        2        3              4
                            //RECEIVED: 1-006:;:roomId:;:userNo:;:userNickname:;:answeredTime
                            RoomInfo room = rooms.get(readData[1]);

                            long answeredTimeLong = Long.parseLong(readData[4]);
                            String answeredTimeString = df.format(answeredTimeLong / 1000000000f);

                            //atomic 하게 추가(addRanking : synchronized method)
                            System.out.println("답 맞힘: " + readData[3] + " / " + answeredTimeString);
                            room.getQuizThread().addToRanking(readData[2], readData[3], answeredTimeLong, answeredTimeString);

                        } else if(readData[0].equals("1-009")) {
                            //           0       1        2            3                 4(pType 의 경우에만)
                            //RECEIVED : 1-009:;:roomId:;:modifyType:;:modifiedContent:;:roomPassword
                            //접속해있는 방에 전달
                            if(readData[2].equals("3") && readData[3].equals("private")) {
                                sendToRoom(readData[1],"1-009:;:" + readData[2] + ":;:" + readData[3] + ":;:" + readData[4]);
                            } else {
                                sendToRoom(readData[1],"1-009:;:" + readData[2] + ":;:" + readData[3]);
                            }

                            //서버의 데이터 처리를 위한 방 참조
                            RoomInfo roomInfo = rooms.get(readData[1]);
                            //대기방에 전달
                            JSONObject waitingRoomJSONData = new JSONObject();
                            waitingRoomJSONData.put("room_id", readData[1]);
                            waitingRoomJSONData.put("modify_type", readData[2]);
                            switch(Integer.parseInt(readData[2])) {
                                case 1:
                                    roomInfo.setTitle(readData[3]);
                                    waitingRoomJSONData.put("title", readData[3]);
                                    break;
                                case 2:
                                    roomInfo.setqType(readData[3]);
                                    waitingRoomJSONData.put("q_type", readData[3]);
                                    break;
                                case 3:
                                    roomInfo.setpType(readData[3]);
                                    roomInfo.setPassword(readData[4]);
                                    waitingRoomJSONData.put("p_type", readData[3]);
                                    if(readData[3].equals("private")) {
                                        waitingRoomJSONData.put("password", readData[4]);
                                    }
                                    break;
                                case 4:
                                    roomInfo.setMaxPeople(Integer.parseInt(readData[3]));
                                    waitingRoomJSONData.put("max_people", readData[3]);
                                    break;
                                default:
                                    break;
                            }
                            sendToWaitingRoom("3-003:;:4:;:" + waitingRoomJSONData.toString());
                            //sendToWaitingRoom("3-002")
                        }

                        //2

                        else if(readData[0].equals("2-002")) {  //user moves
                            //          0       1        2        3              4
                            //RECEIVED: 2-002:;:roomId:;:userNo:;:destinationX:;:destinationY
                            UserInfo user = rooms.get(readData[1]).getRoomMember().get(readData[2]);
                            user.setPositionX(Double.parseDouble(readData[3]));
                            user.setPositionY(Double.parseDouble(readData[4]));

                            //send user's destination to who are in the same room.
                            //TO SEND: 2-002:;:userNo:;:destinationX:;:destinationY
                            sendToRoom(user.getNowIn(), "2-002:;:" + user.getUserNo() + ":;:" + readData[3] + ":;:" + readData[4]);
                        }

                        //3

                        else if(readData[0].equals("3-001")) {  //user app implemented(wants to get room list)
                            //RECEIVED: 3-001:;:userNo
                            waitingRoomPool.put(readData[1], socket);
                            this.userNo = readData[1];
                            this.socketType = TYPE_WAITING_ROOM;
                            Iterator it = rooms.keySet().iterator();
                            StringBuilder toSend = new StringBuilder();
                            while(it.hasNext()) {
                                /*
                                Gson roomObj = new Gson();
                                roomObj.toJson(rooms.get(it.next()));
                                toSend.append(roomObj);
                                toSend.append(":;:"); */

                                JSONObject roomObj = new JSONObject();
                                RoomInfo currentRoom = rooms.get(it.next());

                                roomObj.put("room_id", currentRoom.roomId);
                                roomObj.put("c_time", currentRoom.cTime);
                                roomObj.put("title", currentRoom.title);
                                roomObj.put("password", currentRoom.password);
                                roomObj.put("p_type", currentRoom.pType);
                                roomObj.put("q_type", currentRoom.qType);
                                roomObj.put("current_people", String.valueOf(currentRoom.currentPeople));
                                roomObj.put("max_people", String.valueOf(currentRoom.maxPeople));

                                toSend.append(roomObj.toString());
                                toSend.append(":;:");
                            }
                            //TO SEND: 3-001:;:roomInfo:;:roomInfo:;:...:;:
                            dOut.writeUTF("3-001:;:" + toSend.toString());
                        }
                    }


                } catch(EOFException e1) {
                    e1.printStackTrace();
                } catch(SocketException e2) {
                    System.out.print("나감: ");
                    switch(this.socketType) {
                        case TYPE_WAITING_ROOM : {
                            System.out.println("대기방에서..");
                            waitingRoomPool.remove(this.userNo);
                        } break;
                        case TYPE_CHATTING_ROOM : {
                            System.out.println("채팅방에서..");
                            rooms.get(this.nowIn).leaveRoom(this.userNo, this.userNickname);
                        } break;
                    }

                    return;
                } catch(Exception e3) {
                    e3.printStackTrace();
                } finally {

                }
                //System.out.println("Transaction Complete!");
            }
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
        private String nowIn;
        private String userNo;
        private String userNickname;
        private Socket userSocket;
        private double positionX;
        private double positionY;
        private double destinationX;
        private double destinationY;

        public UserInfo() {
            positionX = 0.5;
            positionY = 0.5;
            destinationX = 0.5;
            destinationY = 0.5;
        }

        public UserInfo(String nowIn, String userNo, String userNickname, Socket userSocket) {
            this.nowIn = nowIn;
            this.userNo = userNo;
            this.userNickname = userNickname;
            this.userSocket = userSocket;
            positionX = 0.5;
            positionY = 0.5;
            destinationX = 0.5;
            destinationY = 0.5;
        }

        public String getNowIn() {
            return nowIn;
        }

        public void setNowIn(String nowIn) {
            this.nowIn = nowIn;
        }

        public String getUserNo() {
            return userNo;
        }

        public void setUserNo(String userNo) {
            this.userNo = userNo;
        }

        public String getUserNickname() {
            return userNickname;
        }

        public void setUserNickname(String userNickname) {
            this.userNickname = userNickname;
        }

        public Socket getUserSocket() {
            return userSocket;
        }

        public void setUserSocket(Socket userSocket) {
            this.userSocket = userSocket;
        }

        public double getPositionX() {
            return positionX;
        }

        public void setPositionX(double positionX) {
            this.positionX = positionX;
        }

        public double getPositionY() {
            return positionY;
        }

        public void setPositionY(double positionY) {
            this.positionY = positionY;
        }

        public double getDestinationX() {
            return destinationX;
        }

        public void setDestinationX(double destinationX) {
            this.destinationX = destinationX;
        }

        public double getDestinationY() {
            return destinationY;
        }

        public void setDestinationY(double destinationY) {
            this.destinationY = destinationY;
        }

        public void setDestinationY(float destinationY) {
            this.destinationY = destinationY;
        }

    }

    public class FireThread extends Thread {
        private String roomId;
        private String firedMemberUserNo;

        public FireThread(String roomId, String firedMemberUserNo) {
            this.roomId = roomId;
            this.firedMemberUserNo = firedMemberUserNo;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(30000);
                for(int i = 0; i < rooms.get(roomId).getFiredMember().size() ; i ++) {
                    if(rooms.get(roomId).getFiredMember().get(i).getUserNo().equals(firedMemberUserNo)) {
                        rooms.get(roomId).getFiredMember().remove(i);
                    }
                }
            } catch(InterruptedException e) { e.printStackTrace(); }
        }
    }

    public class QuizThread extends Thread {

        private final String roomId;
        private RoomInfo roomInfo;
        private List<RankingElement> ranking;
        private String qType;
        private String qnAData;
        private long startTime;
        private int startPeople;

        public QuizThread(String roomId, String qType) {
            this.roomId = roomId;
            this.roomInfo = rooms.get(roomId);
            this.ranking = this.roomInfo.getRanking();
            this.qType = qType;
        }

        @Override
        public void run() {
            //fetch question for every 40 seconds
            while(true) {
                if(!this.roomInfo.isQuizContinuing()) {  //퀴즈가 출제되어 있지 않은 상황. 입장 직후, 퀴즈가 끝난 후에 실행
                    //방 기준시각 설정
                    roomInfo.standardTime = System.nanoTime();
                    //랭킹 클리어
                    this.ranking.clear();

                    //퀴즈 가져옴
                    String param = "q_type=" + this.qType;
                    String tableNumber = "";
                    if (this.qType.equals("상식")) {
                        tableNumber = "1";
                    } else if (this.qType.equals("영단어")) {
                        tableNumber = "2";
                    } else if (this.qType.equals("초성")) {
                        tableNumber = "3";
                    } else if (this.qType.equals("눈치게임")) {
                        tableNumber = "4";
                    }

                    qnAData = "1-006" + ":;:" + roomInfo.standardTime + ":;:" +
                    doHTMLWorks("http://dak2183242.cafe24.com/qchat/game/fetch_question.php?sec_code=thisrequestissentbygameserver!pleasesendmeaquestionasap" + "&" + "q_type=" + tableNumber, this.roomId + "의 퀴즈 스레드 종료");
                    sendToRoom(roomId, qnAData);



                    /*
                    String result = "";
                    try {
                        URL url = new URL("http://dak2183242.cafe24.com/qchat/game/fetch_question.php?sec_code=thisrequestissentbygameserver!pleasesendmeaquestionasap" + "&" + "q_type=" + tableNumber);
                        System.out.println("URL: " + url.toString());
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        conn.setRequestMethod("GET");
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
                        while ((line = in.readLine()) != null) {
                            buff.append(line + "\n");
                            break;
                        }
                        data = "1-006:;:" + roomInfo.standardTime + ":;:" +  buff.toString().trim();
                        qnAData = data;
                        sendToRoom(roomId, data);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        System.out.println(this.roomId + "의 퀴즈 스레드 종료");
                        return;
                    }
                    */

                    //퀴즈를 가져온 시간을 제외한 시간동안 sleep 하여 20초를 대기한다.
                    //여기서 System.nanoTime() - roomInfo.standardTime 의 값은 퀴즈를 가져온 시간이 된다.
                    try {
                        Thread.sleep(19900 - (System.nanoTime() - roomInfo.standardTime) / 1000000);
                        while(System.nanoTime() - roomInfo.standardTime < 20000000000L);    //20초까지 채운다
                        roomInfo.sendTriggerToFirstEntered();                               //최초 입장한 유저에게 퀴즈 트리거를 준다
                        roomInfo.setQuizContinuing(true);                                   //퀴즈 출제상태로 바꾼다.
                        startPeople = roomInfo.getCurrentPeople();                          //퀴즈 출제시점에 방 안에 존재하는 사람 수를 기록한다
                        roomInfo.standardTime = System.nanoTime();                          //퀴즈 출제시점을 방 기준시각으로 재설정한다
                    } catch(InterruptedException e) {
                        System.out.println(this.roomId + "의 quizClock 중단");
                    }
                } else {    //퀴즈 출제가 되어 있는 상태
                    roomInfo.standardTime = System.nanoTime();
                    //20초동안 대기한다(퀴즈 출제후 방 기준시각으로부터 20초 대기하도록 함)
                    try {
                        Thread.sleep(19900);
                        while(System.nanoTime() - roomInfo.standardTime < 20000000000L);
                        roomInfo.setQuizContinuing(false);
                        roomInfo.standardTime = System.nanoTime();
                    } catch(InterruptedException e) {
                        System.out.println(this.roomId + "의 quizClock 중단");
                    }

                    //TODO(퀴즈결과 처리)
                    //퀴즈 결과를 생성한다
                    StringBuilder quizResultString = new StringBuilder(50);
                    quizResultString.append("1-007");
                    for(int i = 0; i < ranking.size(); i ++) {
                        quizResultString.append(":;:");
                        quizResultString.append(ranking.get(i).getUserNo());
                        quizResultString.append("::");
                        quizResultString.append(ranking.get(i).getNickname());
                        quizResultString.append("::");
                        quizResultString.append(determinePoints(i + 1));
                        quizResultString.append("::");
                        quizResultString.append(ranking.get(i).getRecord());

                        //랭킹 리스트에 결정된 포인트를 저장한다.
                        ranking.get(i).setPoints(String.valueOf(determinePoints(i + 1)));
                    }

                    //퀴즈 결과 전송
                    sendToRoom(roomId, quizResultString.toString());

                    //포인트 지급 실행
                    //퀴즈 정답자 중 현재 시점에도 방에 존재하는 사람들에게만 포인트를 지급한다.
                    StringBuilder getPointUserList = new StringBuilder(10);
                    for(int i = 0; i < ranking.size(); i ++) {
                        try {
                            //존재하는 유저인지 확인. 존재하지 않으면 NullPointerException 발생
                            roomInfo.roomMember.get(ranking.get(i).getUserNo());
                            //전송형식: userNo::points:;:...

                            getPointUserList.append(ranking.get(i).getUserNo());
                            getPointUserList.append("::");
                            getPointUserList.append(ranking.get(i).getPoints());
                            getPointUserList.append(":;:");
                        } catch(NullPointerException e) {
                            System.out.println("나간 유저에게 포인트 지급 X");
                        }
                    }

                    doHTMLWorks("http://dak2183242.cafe24.com/qchat/game/give_points.php?sec_code=thisrequestissentbygameserver!pleasegivepointstomyroommember" + "&" + "user_list=" + getPointUserList.toString(), "포인트 지급 실패");
                }
            }
        }

        public long getStartTime() {
            return this.startTime;
        }

        public int determinePoints(int ranking) {
            return Math.max((this.startPeople + 5) - (2 * ranking) ,2);
        }

        synchronized public void addToRanking(String userNo, String nickname, long answeredTimeLong, String answeredTimeString) {
            boolean positionDetermined = false; //랭킹 삽입이 완료되었음을 알리는 변수
            for(int i = 0; i < roomInfo.ranking.size(); i ++) {
                if(answeredTimeLong < roomInfo.ranking.get(i).getComp()) {
                    roomInfo.ranking.add(i, new RankingElement(userNo, nickname, answeredTimeLong, answeredTimeString));
                    positionDetermined = true;
                    break;
                }
            }
            if(!positionDetermined) roomInfo.ranking.add(new RankingElement(userNo,nickname, answeredTimeLong, answeredTimeString));
        }

        public String getQnAData() {
            return qnAData;
        }

        public String doHTMLWorks(String urlString, String errorMessage) {

            String param = "";
            String result = "";
            try {
                URL url = new URL(urlString);
                System.out.println("URL: " + url.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("GET");
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
                while ((line = in.readLine()) != null) {
                    buff.append(line + "\n");
                    break;
                }
                result = buff.toString().trim();
            } catch (MalformedURLException e) {
                System.out.println("MalformedURLException: " + errorMessage);
            } catch (IOException e) {
                System.out.println("IOException: " + errorMessage);
            } catch (NullPointerException e) {
                System.out.println("NullPointerException: " + errorMessage);
            }
            return result;
        }

    }

    public class RankingElement {
        private String userNo;
        private String nickname;
        private long comp;
        private String record;
        private String points;

        public RankingElement(String userNo, String nickname, long comp, String record) {
            this.userNo = userNo;
            this.nickname = nickname;
            this.comp = comp;
            this.record = record;
        }

        public String getUserNo() {
            return userNo;
        }

        public void setUserNo(String userNo) {
            this.userNo = userNo;
        }

        public long getComp() {
            return comp;
        }

        public void setComp(long comp) {
            this.comp = comp;
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

        public String getPoints() {
            return points;
        }

        public void setPoints(String points) {
            this.points = points;
        }
    }

    public class RoomInfo {
        String roomId;
        String creatorUserNo;
        String cTime;
        private Long standardTime;
        String title;
        String password;
        String pType;
        String qType;
        HashMap<String, UserInfo> roomMember;
        HashMap<String, UserInfo> firedMember;
        ArrayList<Socket> firstEnteredList;
        QuizThread quizThread;
        List<RankingElement> ranking;

        final String curPeopleKey = "CUR_PEOPLE_KEY";

        boolean isBattleTriggered;
        volatile boolean isQuizContinuing;
        int startPeople;
        volatile int currentPeople;
        int maxPeople;

        public RoomInfo(String roomId, String creatorUserNo, String cTime, String title, String password, String pType, String qType, int maxPeople, HashMap<String, UserInfo> roomMember) {

            //room information save to server
            rooms.put(roomId, this);

            this.startPeople = 0;
            this.currentPeople= 1;
            this.roomId = roomId;
            this.creatorUserNo = creatorUserNo;
            this.cTime = cTime;
            this.standardTime = System.nanoTime();
            this.title = title;
            this.password = password;
            this.pType = pType;
            this.qType = qType;
            this.maxPeople = maxPeople;
            this.roomMember = roomMember;
            this.firstEnteredList = new ArrayList<Socket>();
            this.ranking = Collections.synchronizedList(new ArrayList<RankingElement>());
            this.quizThread =  new QuizThread(roomId, this.qType);
            this.firedMember = new HashMap<String, UserInfo>();

            isBattleTriggered = false;
            isQuizContinuing = false;

            quizThread.start();
        }

        public String getRoomId() {
            return roomId;
        }

        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getpType() {
            return pType;
        }

        public void setpType(String pType) {
            this.pType = pType;
        }

        public String getqType() {
            return qType;
        }

        public void setqType(String qType) {
            this.qType = qType;
        }

        public HashMap<String, UserInfo> getRoomMember() {
            return roomMember;
        }

        public void setRoomMember(HashMap<String, UserInfo> roomMember) {
            this.roomMember = roomMember;
        }

        public HashMap<String, UserInfo> getFiredMember() {
            return firedMember;
        }

        public void setFiredMember(HashMap<String, UserInfo> firedMember) {
            this.firedMember = firedMember;
        }

        public int getStartPeople() {
            return startPeople;
        }

        public void setStartPeople(int startPeople) {
            this.startPeople = startPeople;
        }

        public int getCurrentPeople() {
            return currentPeople;
        }

        public void setCurrentPeople(int currentPeople) {
            this.currentPeople = currentPeople;
        }

        public int getMaxPeople() {
            return maxPeople;
        }

        public void setMaxPeople(int maxPeople) {
            this.maxPeople = maxPeople;
        }

        public boolean isBattleTriggered() {
            return isBattleTriggered;
        }

        public void setBattleTriggered(boolean battleTriggered) {
            isBattleTriggered = battleTriggered;
        }

        public boolean isQuizContinuing() {
            return isQuizContinuing;
        }

        public void setQuizContinuing(boolean quizContinuing) {
            isQuizContinuing = quizContinuing;
        }


        public void enterRoom(UserInfo userInfo) {
            this.roomMember.put(userInfo.userNo, userInfo);
            synchronized(curPeopleKey) {
                try {
                    rooms.get(this.roomId).getRoomId(); //없어진 방인지 검사
                } catch(NullPointerException e) {
                    try {
                        new DataOutputStream(waitingRoomPool.get(userInfo.userNo).getOutputStream()).writeUTF("3-004:;:" + this.roomId);

                    } catch(IOException e2) {
                        return;
                    }
                    return;
                }
                this.currentPeople ++;

                try {
                    JSONObject roomObj = new JSONObject();
                    roomObj.put("room_id", this.roomId);
                    roomObj.put("current_people", String.valueOf(this.currentPeople));
                    sendToWaitingRoom("3-003:;:2:;:" + roomObj.toString());
                    if(!isQuizContinuing) {
                        new DataOutputStream(userInfo.getUserSocket().getOutputStream()).writeUTF(quizThread.getQnAData());
                        firstEnteredList.add(userInfo.getUserSocket());
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public synchronized void sendTriggerToFirstEntered() {
            for(int i = 0; i < this.firstEnteredList.size(); i ++) {
                try {
                    new DataOutputStream(firstEnteredList.get(i).getOutputStream()).writeUTF("1-008");
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
            firstEnteredList.clear();
        }

        public void leaveRoom(String userNo, String userNickname) {
            synchronized(curPeopleKey) {
                roomMember.remove(userNo);
                if(currentPeople > 1) {
                    this.currentPeople --;
                    try {
                        JSONObject roomObj = new JSONObject();
                        roomObj.put("room_id", this.roomId);
                        roomObj.put("current_people", String.valueOf(this.currentPeople));
                        sendToWaitingRoom("3-003:;:2:;:" + roomObj.toString());
                        sendToRoom(this.roomId, "1-004:;:" + userNo + ":;:" + userNickname);
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        System.out.println("마지막 사람이 나갔으므로 퀴즈 스레드 종료");
                        rooms.get(this.roomId).getQuizThread().interrupt();
                    } catch (IllegalStateException e ) { System.out.println("마지막 사람이 나갔으므로 퀴즈 스레드 종료"); }
                    try {
                        JSONObject roomObj = new JSONObject();
                        roomObj.put("room_id", this.roomId);
                        sendToWaitingRoom("3-003:;:3:;:" + roomObj.toString());
                    } catch(JSONException e) {
                        e.printStackTrace();
                    } catch(Exception e) {
                        System.out.println("닫힌 소켓");
                    }
                    rooms.remove(this.roomId);
                }
            }
        }

        public QuizThread getQuizThread() {
            return quizThread;
        }

        public void setQuizThread(QuizThread quizThread) {
            this.quizThread = quizThread;
        }

        public List<RankingElement> getRanking() {
            return ranking;
        }

        public void setRanking(List<RankingElement> ranking) {
            this.ranking = ranking;
        }

        synchronized public void addToRankingList() {

        }
    }
}
