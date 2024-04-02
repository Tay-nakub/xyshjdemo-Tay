package com.example.xyshjdemo;

public class HexDataHelper {
    public  static short[] Int2Short16_2(int v)
    {
        if(v==0)return new short[]{0x00};
        if(v<0)v=((short)v)&0xff;
        short[] buffer = new short[50];
        int tmpV = v;

        int i=0;
        while(tmpV > 0)
        {
            int v1 = tmpV % 256;
            buffer[i++]= (short) v1;//getByte16(v1);
            tmpV = tmpV/256;
        }
        short[] re = new short[i];
        for(int j=0; j < i; j++)
        {
            re[j] = buffer[i-j-1];
        }
        return re;
    }

    public static String hex2String(byte[] data)
    {
        return hex2String(data,data.length);
    }
    public static String hex2String(byte[] data, int len)
    {

        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = data;
        int bit;

        for (int i = 0; i < len; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            sb.append(' ');
        }
        return sb.toString().trim();
    }
    public static short computerXor(byte[] data, int offset, int len)
    {
        short crc = 0;
        for(int i=offset; i < offset+ len; i++)
        {
            crc ^=data[i];
        }
        return (short) (crc & 0xff);
    }
}
