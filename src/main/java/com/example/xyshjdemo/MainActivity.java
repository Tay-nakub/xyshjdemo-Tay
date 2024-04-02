package com.example.xyshjdemo;

import android.os.Handler;
import android.os.Message;
import android.serialport.SerialPort;
//import android.support.v7.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ListView listView;
    ListAdapter listAdapter;

    Thread mThread;
    SerialPort serialPort;
    String devPath;
    int baudrate;
    int no = 0;
    byte[] ackBytes = new byte[]{(byte) 0xFA,(byte)0xFB,0x42,0x00,0x43};
    private ByteArrayOutputStream mBuffer = new ByteArrayOutputStream();
    Queue<byte[]> queue =  new LinkedList<byte[]>();

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listView1);
        listAdapter = new ListAdapter();
        listAdapter.setContext(this);
        listView.setAdapter(listAdapter);

        findViewById(R.id.connect).setOnClickListener(this);
        findViewById(R.id.driverhd).setOnClickListener(this);
        findViewById(R.id.querydhstatus).setOnClickListener(this);
        findViewById(R.id.btnSubmit).setOnClickListener(this);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.connect:{
                if(null==serialPort)
                    bindSerialPort();
                else{
                    try{
                        serialPort.close();
                        serialPort = null;
                        ((Button)view).setText("连接(Connect)");//เชื่อมต่อ
                    }
                    catch (Exception e)
                    {

                    }
                }
            }
            break;
            case R.id.driverhd:{
                try {
                    int hdh = Integer.parseInt(((EditText) findViewById(R.id.hdh)).getText().toString());
                    short[] hdhbyte = HexDataHelper.Int2Short16_2(hdh);
                    //货道号补齐两字节
                    //หมายเลขช่องทางขนส่งสินค้าต้องกรอกด้วยขนาด 2 ไบต์
                    if(hdhbyte.length == 1)
                    {
                        short temp = hdhbyte[0];
                        hdhbyte = new short[2];
                        hdhbyte[0]= 0;
                        hdhbyte[1] = temp;
                    }
                    byte[] data  = new byte[]{(byte) 0xFA, (byte) 0xFB, 0x06, 0x05, (byte) getNextNo(), 0x01, 0x00, (byte) hdhbyte[0], (byte) hdhbyte[1], 0x00};
                    data[data.length - 1] = (byte) HexDataHelper.computerXor(data, 0, data.length - 1);
                    queue.add(data);
                    System.out.println("Print bytes(data):: = " + print(data));
                }
                catch (Exception e)
                {

                }
            }
            break;
            case R.id.querydhstatus:{
                try {
                    int hdh = Integer.parseInt(((EditText) findViewById(R.id.hdh)).getText().toString());
                    short[] hdhbyte = HexDataHelper.Int2Short16_2(hdh);

                    //货道号补齐两字节
                    //หมายเลขช่องทางขนส่งสินค้าต้องกรอกด้วยขนาด 2 ไบต์
                    if(hdhbyte.length == 1)
                    {
                        short temp = hdhbyte[0];
                        hdhbyte = new short[2];
                        hdhbyte[0]= 0;
                        hdhbyte[1] = temp;
                    }


                    byte[] data  = new byte[]{(byte) 0xFA, (byte) 0xFB, 0x01, 0x03, (byte) getNextNo(), (byte) hdhbyte[0], (byte) hdhbyte[1], 0x00};
                    data[data.length - 1] = (byte) HexDataHelper.computerXor(data, 0, data.length - 1);
                    queue.add(data);
                    System.out.println("Print bytes(check status):: = " + print(data));
                }
                catch (Exception e)
                {

                }
            }
            break;
            case R.id.btnSubmit:{
                try {
                    String CommandData = ((EditText)findViewById(R.id.txtCommand)).getText().toString();
                    String LengthData = ((EditText)findViewById(R.id.txtLength)).getText().toString();
                    String InputData = ((EditText)findViewById(R.id.txtInput)).getText().toString();
                    System.out.println("Print btnSubmit:: = " + InputData);

                    String[] words = InputData.split(" ");
                    int Inputlength = words.length;
                    System.out.println("Print Inputlength:: = " + Inputlength);

                    byte[] data ;
                    //data  = new byte[]{(byte) 0xFA, (byte) 0xFB, 0x06, 0x05, (byte) getNextNo(), 0x01, 0x00, (byte) hdhbyte[0], (byte) hdhbyte[1], 0x00};

                    //data  = new byte[]{(byte) 0xFA, (byte) 0xFB};
                    data = new byte[6+Inputlength];//new byte[3+Inputlength];
                    data[0] = (byte) 0xFA;
                    data[1] = (byte) 0xFB;

                    //-------CommandData--------
                    byte[] ComS = hexStringToBytes(CommandData);
                    data[2] = (byte) ComS[0];

                    //-------inputLengthData--------
                    int inputLengthData = 1;
                    if(LengthData != "") {
                        try{
                        inputLengthData = Integer.parseInt(LengthData);
                        }catch (Exception e)
                        {inputLengthData = Inputlength+1;}
                    }else{
                        inputLengthData = Inputlength+1;
                    }
                    short[] LenData = HexDataHelper.Int2Short16_2(inputLengthData);
                    data[3] = (byte) LenData[0];
                    //-------inputLengthData--------

                    //-------Running Queue--------
                    data[4] = (byte) getNextNo();
                    //-------Running Queue--------
                    System.out.println("Length data :" + data.length);
                    int item_ = 5;//2
                    for (String st: words) {
//                        //ฐาน 10 -> 16 -> byte
//                        int input1 = Integer.parseInt(st);
//                        short[] Input2 = HexDataHelper.Int2Short16_2(input1);
//                        //data[item_] = (byte) Input2[0];
                        System.out.println("Print (data) item["+item_+"] st:: = " + st);
                        if(st.equals("??"))
                        {
                            //System.out.println("st1: "+st);

                            int slothdh = Integer.parseInt(((EditText) findViewById(R.id.hdh)).getText().toString());
                            short[] slothdhbyte = HexDataHelper.Int2Short16_2(slothdh);
                            data[item_] = (byte) slothdhbyte[0];

                        }else {
                            //System.out.println("st2: " + st);

                            //16 -> byte
                            byte[] hexS = hexStringToBytes(st);
                            //System.out.println("Print bytes(hexS):: = " + print(hexS));
                            data[item_] = (byte) hexS[0];
                        }
                        System.out.println("Print (data)["+item_+"]:: = " + data[item_]);

                        item_++;
                    }
                    System.out.println("Print bytes(data1):: = " + print(data));

                    data[item_] = (byte) 0x00;

                    data[data.length - 1] = (byte) HexDataHelper.computerXor(data, 0, data.length - 1);
                    queue.add(data);
                    System.out.println("Print bytes(data3):: = " + print(data));
                }
                catch (Exception e)
                {

                }
            }
            break;
            default:{

            }
        }
    }


    public int getNextNo(){
        no++;
        if(no>=255){
            no=0;
        }
        return no;
    }

    public void onSerialPortConnectStateChanged(boolean connected){
        if(connected){
            ((Button)findViewById(R.id.connect)).setText("断开(Disconnect)");//ตัดการเชื่อมต่อ
        }
        else{
            ((Button)findViewById(R.id.connect)).setText("连接(Connect)");//เชื่อมต่อ
        }
    }
    private void  bindSerialPort()
    {
        devPath = ((EditText)findViewById(R.id.dev)).getText().toString();
        baudrate = Integer.parseInt(((EditText)findViewById(R.id.baudrate)).getText().toString());
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File serialFile = new File(devPath);
                    if (!serialFile.exists() || baudrate == -1) {
                        return;
                    }

                    try {
                        serialPort = new SerialPort(serialFile, baudrate, 0);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onSerialPortConnectStateChanged(true);
                            }
                        });
                        readSerialPortData();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                finally
                {
                    if(null!=serialPort){
                        try
                        {
                            serialPort.close();
                            serialPort = null;
                        }
                        catch (Exception e) {
                        }
                    }

                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onSerialPortConnectStateChanged(false);
                    }
                });
            }
        });
        mThread.start();
    }
    private void readSerialPortData()
    {
        while (true)
        {
            try{
                if(null == serialPort){
                    Thread.sleep(1000);
                    continue;
                }
                int available = serialPort.getInputStream().available();
                if(0 == available){
                    Thread.sleep(10);
                    continue;
                }

                byte[] data = readBytes(serialPort.getInputStream(),available);
                mBuffer.write(data);
                while(true) {
                    byte[] bytes = mBuffer.toByteArray();
                    int start = 0;
                    int cmdCount = 0;
                    boolean shuldBreak = false;
                    for(; start<= bytes.length-5; start++)
                    {
                        if((short) (bytes[start] & 0xff)==0xFA&&(short) (bytes[start+1] & 0xff)==0xFB) {
                            try {
                                int len = bytes[start + 3];
                                byte[] cmd = new byte[len + 5];
                                System.arraycopy(bytes, start, cmd, 0, cmd.length);
                                cmdCount++;
                                proccessCmd(cmd);


                                //计算还有多少剩余字节要解析，没有的跳出等待接收新的字节，有则继续处理
                                //คำนวณจำนวนไบต์ที่เหลือในการแยกวิเคราะห์
                                //หากไม่มีก็จะกระโดดออกมารอรับไบต์ใหม่ ถ้ามีก็จะประมวลผลต่อ
                                int remain = bytes.length - start - cmd.length;
                                if(0 == remain)
                                {
                                    shuldBreak = true;
                                    mBuffer.reset();
                                    break;
                                }
                                byte[] buffer2 = new byte[remain];
                                System.arraycopy(bytes, start + cmd.length, buffer2, 0, buffer2.length);
                                mBuffer.reset();
                                mBuffer.write(buffer2);
                            }
                            catch (Exception e)
                            {
                                shuldBreak = true;
                                //因数据包不全，导致越界异常，直接跳出即可
                                //เนื่องจากแพ็กเก็ตข้อมูลไม่สมบูรณ์
                                //ทำให้เกิดข้อยกเว้นนอกขอบเขต เพียงแค่กระโดดออกไป
                            }
                            break;
                        }
                    }
                    if(0==cmdCount||shuldBreak)
                    {
                        break;
                    }

                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    public byte[] readBytes(InputStream stream, int length) throws IOException {
        byte[] buffer = new byte[length];

        int total = 0;

        while (total < length) {
            int count =stream.read(buffer, total, length - total);
            if (count == -1) {
                break;
            }
            total += count;
        }

        if (total != length) {
            throw new IOException(String.format("Read wrong number of bytes. Got: %s, Expected: %s.", total, length));
        }

        return buffer;
    }
    public void writeCmd(byte[] cmd)
    {
        try{
            serialPort.getOutputStream().write(cmd);
            serialPort.getOutputStream().flush();
            addText(">> "+ HexDataHelper.hex2String(cmd));

//            Log.d(">>WriteCmd(ฐาน16)-2: ",print(cmd));
//            Log.d(">>WriteCmd(ฐาน16)-3: ",HexDataHelper.hex2String(cmd));
        }
        catch (Exception e)
        {

        }
    }
    public void proccessCmd(byte[] cmd)
    {
        addText("<<"+ HexDataHelper.hex2String(cmd));
       // System.out.println("<<proccessCmd bytes = " + print(cmd));

        if(0x41==(short) (cmd[2] & 0xff)){
            //收到POLL包
            //ได้รับแพ็คเกจ POLL แล้ว
            if(queue.size()==0){
                writeCmd(ackBytes);
            }
            else{
                writeCmd(queue.poll());
            }
        }
        else  if(0x42==(short) (cmd[2] & 0xff)){
            // 收到ACK
            //ได้รับแล้ว ACK 
        }
        else{
            if(0x02==(short) (cmd[2] & 0xff)&&0x04==(short) (cmd[3] & 0xff)) {
                //查询货道状态的返回值
                //ส่งคืนค่าของการสืบค้นสถานะช่องทางขนส่งสินค้า
                handler.post(new RunableEx(HexDataHelper.hex2String(cmd)) {
                    public void run() {
                        ((TextView) findViewById(R.id.dhstatus)).setText(text);
                    }
                });
            }
            writeCmd(ackBytes);

            System.out.println("<<proccessCmd bytes length = " + cmd.length);
            System.out.println("<<proccessCmd bytes = " + print(cmd));

            //4.2.1 VMC reports selection price, inventory, capacity and product ID (VMC sends out)
            if(cmd.length == 17 && 0x11==(short) (cmd[2]) && 0x0C==(short) (cmd[3])) {
                Func_VMC_0x11(cmd);
            }
            //4.1.2 VMC reports the current amount (VMC sends out)
            else if(cmd.length == 10 && 0x23 ==(short) (cmd[2]) && 0x05==(short) (cmd[3])) {
                Func_VMC_0x23(cmd);
            }

        }
    }
    public void Func_VMC_0x11(byte[] cmd) {

//                for (byte st : cmd) {
//                    System.out.println("<< bytes[Base10] = " + st);
        byte[] Sel_Number = new byte[2];
        Sel_Number[0] = cmd[5];
        Sel_Number[1] = cmd[6];
//        System.out.println("<<proccessCmd Sel_Number = " + print(Sel_Number));
        String Number_Str = bytesToHexString(Sel_Number);
        Integer Number_Int = hexadecimalToDecimal(Number_Str.toUpperCase());
//        System.out.println("<<Invertory stock: Base64:=("+Number_Str+"),Base10:= " + Number_Int);

        byte[] Sel_Price = new byte[4];
        Sel_Price[0] = cmd[7];
        Sel_Price[1] = cmd[8];
        Sel_Price[2] = cmd[9];
        Sel_Price[3] = cmd[10];
//        System.out.println("<<proccessCmd Sel_Price = " + print(Sel_Price));
        String Price_Str = bytesToHexString(Sel_Price);
        Integer Price_Int = hexadecimalToDecimal(Price_Str.toUpperCase());
//        System.out.println("<<Price: Base64:=("+Price_Str+"),Base10:= " + Price_Int);

        //byte Sel_Inventory = cmd[11];
        byte[] Sel_Inventory = new byte[1];
        Sel_Inventory[0] = cmd[12];
//        System.out.println("<<proccessCmd Sel_Capacity = "+ Sel_Inventory+",Cap2::= "+ print(Sel_Inventory));
        String Invertory_Str = bytesToHexString(Sel_Inventory);
        Integer Invertory_Int = hexadecimalToDecimal(Invertory_Str.toUpperCase());
//        System.out.println("<<Invertory stock: Base64:=("+Invertory_Str+"),Base10:= " + Invertory_Int);


        //byte Sel_Capacity = cmd[12];
        byte[] Sel_Capacity = new byte[1];
        Sel_Capacity[0] = cmd[12];
//        System.out.println("<<proccessCmd Sel_Capacity = "+ Sel_Capacity+",Cap2::= "+ print(Sel_Capacity));
        String Cap_Str = bytesToHexString(Sel_Capacity);
        Integer Cap_Int = hexadecimalToDecimal(Cap_Str.toUpperCase());
//        System.out.println("<<Capacity: Base64:=("+Cap_Str+"),Base10:= " + Cap_Int);

        byte[] Sel_CommodifyNumber = new byte[2];
        Sel_CommodifyNumber[0] = cmd[13];
        Sel_CommodifyNumber[1] = cmd[14];
//        System.out.println("<<proccessCmd Sel_CommodifyNumber = " + print(Sel_CommodifyNumber));
        String Commo_Str = bytesToHexString(Sel_CommodifyNumber);
        Integer Commo_Int = hexadecimalToDecimal(Commo_Str.toUpperCase());
//        System.out.println("<<Slot No. Base64:=("+Commo_Str+"),Base10:= " + Commo_Int);

        byte Sel_Status = cmd[15];
        //System.out.println("<<Sel_Status = " + (Sel_Status));
        String SS = "";
        if (0x00 == Sel_Status) {
            SS = "Normal";
        } else if (0x00 == Sel_Status) {
            SS = "Normal";
        }
        addText("<<"+"ช่องจ่ายสินค้า : "+ "Base64:=("+Commo_Str.toUpperCase()+"),Base10:= " + Commo_Int+ " ,Capacity:= "+Cap_Int+ " ,Inventory:= "+Invertory_Int+ " ,Price:= "+Price_Int + " ,Status:= "+ SS);
        System.out.println("<<"+"ช่องจ่ายสินค้า : "+ "Base64:=("+Commo_Str.toUpperCase()+"),Base10:= " + Commo_Int+ " ,Capacity:= "+Cap_Int+ " ,Inventory:= "+Invertory_Int+ " ,Price:= "+Price_Int + " ,Status:= "+ SS);
//                    break;
//                }
    }
    public void Func_VMC_0x23(byte[] cmd){
        byte[] Sel_price = new byte[4];
        Sel_price[0] = cmd[5];
        Sel_price[1] = cmd[6];
        Sel_price[1] = cmd[7];
        Sel_price[1] = cmd[8];
        String Number_Str = bytesToHexString(Sel_price);
        Integer Number_Int = hexadecimalToDecimal(Number_Str.toUpperCase());
        System.out.println("<<price stock: Base64:=("+Number_Str.toUpperCase()+"),Base10:= " + Number_Int);
        addText("<<"+"ราคา : "+Number_Int);
    }
    public void addText(String text){
        handler.post(new RunableEx(text) {
            public void run() {
                listAdapter.addText(text);
            }
        });

    }

    @NonNull
    public static String print(@NonNull byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (byte b : bytes) {
            sb.append(String.format("0x%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
    @Nullable
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder("");

        if (bytes == null || bytes.length <= 0) {
            return null;
        }

        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                sb.append(0);
            }
            sb.append(hv);
        }

        return sb.toString();
    }
    public static final String HEX_STRING_BLANK_SPLIT = " ";
//    public static String toHexString(String str) {
//        byte[] byteArray;
//        try {
//            byteArray = str.getBytes("utf-8");
//        } catch  {
//            byteArray = str.getBytes();
//        }
//        return bytesToHexString(byteArray);
//    }
    public static byte[] getBytes(String hexString) {
        String[] hexArray = hexString.split(HEX_STRING_BLANK_SPLIT);
        byte[] bytes = new byte[hexArray.length];
        for (int i = 0; i < hexArray.length; i++) {
            String hex = hexArray[i];
            bytes[i] = Integer.valueOf(hex, 16).byteValue();

        }
        return bytes;
    }
//    public static byte[] hexStringToBytes(String hex) {
//        int len = hex.length() / 2;
//        byte[] result = new byte[len];
//        char[] achar = hex.toCharArray();
//        for (int i = 0; i < len; i++) {
//            int pos = i * 2;
//            byte b1 = (byte) "0123456789ABCDEF".indexOf(achar[pos]);
//            byte b2 = (byte) "0123456789ABCDEF".indexOf(achar[(pos + 1)]);
//            result[i] = (byte) (b1 << 4 | b2);
//        }//from  ww  w  .  j a va 2s .  com
//        return result;
//    }
// To convert hexadecimal to decimal
static int hexadecimalToDecimal(String hexVal)
{
    // Storing the length of the
    int len = hexVal.length();

    // Initializing base value to 1, i.e 16^0
    int base = 1;

    // Initially declaring and initializing
    // decimal value to zero
    int dec_val = 0;

    // Extracting characters as
    // digits from last character

    for (int i = len - 1; i >= 0; i--) {

        // Condition check
        // Case 1
        // If character lies in '0'-'9', converting
        // it to integral 0-9 by subtracting 48 from
        // ASCII value
        if (hexVal.charAt(i) >= '0'
                && hexVal.charAt(i) <= '9') {
            dec_val += (hexVal.charAt(i) - 48) * base;

            // Incrementing base by power
            base = base * 16;
        }

        // Case 2
        // if case 1 is bypassed

        // Now, if character lies in 'A'-'F' ,
        // converting it to integral 10 - 15 by
        // subtracting 55 from ASCII value
        else if (hexVal.charAt(i) >= 'A'
                && hexVal.charAt(i) <= 'F') {
            dec_val += (hexVal.charAt(i) - 55) * base;

            // Incrementing base by power
            base = base * 16;
        }
    }

    // Returning the decimal value
    return dec_val;
}
}
