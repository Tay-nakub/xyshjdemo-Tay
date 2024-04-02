package com.example.xyshjdemo;

abstract class RunableEx implements Runnable{
    String text;
    public RunableEx(String data)
    {
        this.text = data;
    }
}
