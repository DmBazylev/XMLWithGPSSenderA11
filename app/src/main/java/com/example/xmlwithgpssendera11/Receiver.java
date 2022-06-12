
//DMBazylev, used for Android 11.0, version 1.0, must be used only with the user's consent
package com.example.xmlwithgpssendera11;//пакет

import android.content.BroadcastReceiver;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;//для работы с массивом смс
import java.lang.Object;//для работы с массивом смс
import android.telephony.SmsMessage;//для работы с массивом смс
import android.os.Build.VERSION_CODES;//для работы с разными версиями ОС Андроид
import android.os.Build.VERSION;
import android.os.Build;
import android.widget.Toast;

public class Receiver extends BroadcastReceiver
{

    public Receiver()
    {
    }


    @Override
    public void onReceive(Context context, Intent intent)
    {

        String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
        boolean toStart=false;//переменная для проверки смс - если смс та которая нужна то запускаем сервис

        int commandID=-1;
        Intent i = new Intent(context.getApplicationContext(), MyService.class);

        if (intent.getAction().equals(SMS_RECEIVED))//без этой проверки код для запуска по смс работать не будет даже тот код что выше
        {

            Bundle extras = intent.getExtras();
            if (extras == null)
            {
                return;
            }

            Object[] pdus = (Object[]) extras.get("pdus"); //создаем массив смс
            for (int w = 0; w < pdus.length; w++)
            {
                SmsMessage SMessage = SmsMessage.createFromPdu((byte[]) pdus[w]);//получаем смс массива на каждой итерации
                String sender = SMessage.getOriginatingAddress();//получение телефона с которого отправлено смс - в обычном формате напр "+79146966428"
                String body = SMessage.getMessageBody();//"тело" то есть текст смс

                if (body.equals("ASS"))//если текст смс равен "ASS" - старт
                {
                    Log.d("onReceive ","ASS");
                    commandID=0;
                }

                if (body.equals("ASE"))//если текст смс равен "ASE" -стоп
                {
                    Log.d("onReceive ","ASE");
                    commandID=1;
                }

                if (body.equals("ASGSTA"))//если текст смс равен "ASGSTA" - получить координаты при запущенном сервисе
                {
                    Log.d("onReceive ","ASGSTA");
                    commandID=2;
                }
                if (body.equals("ASGSTO"))//если текст смс равен "ASGSTO" - получить координаты при незапущенном сервисе (запустить, править смс с координами и после этого остановить сервис)
                {
                    Log.d("onReceive ","ASGSTO");
                    commandID=3;
                }

            }


            if (commandID==0||commandID==1||commandID==2||commandID==3)
            {
                switch (commandID)
                {
                    case 0:
                        i.putExtra ("num0",0);//"ASS" - старт
                        //   context.getApplicationContext().startService(i);//запуск сервиса, метод getApplicationContext() используется тк нужен "стабильный" контекст а не текущий контекст - обычный который передан в параметр метода onReceive приемник
                        break;
                    case 1:
                        i.putExtra ("num1",1);//"ASE" -стоп
                        //  context.getApplicationContext().startService(i);//запуск сервиса, метод getApplicationContext() используется тк нужен "стабильный" контекст а не текущий контекст - обычный который передан в параметр метода onReceive приемник
                        break;
                    case 2:
                        i.putExtra ("num2",2);//"ASGSTA" - получить координаты при запущенном сервисе
                        //   context.getApplicationContext().startService(i);//запуск сервиса, метод getApplicationContext() используется тк нужен "стабильный" контекст а не текущий контекст - обычный который передан в параметр метода onReceive приемник
                        break;
                    case 3:
                        i.putExtra ("num3",3);//"ASGSTO" - получить координаты при незапущенном сервисе (запустить, править смс с координами и после этого остановить сервис)
                        //  context.getApplicationContext().startService(i);//запуск сервиса, метод getApplicationContext() используется тк нужен "стабильный" контекст а не текущий контекст - обычный который передан в параметр метода onReceive приемник
                        break;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)//если версия больше чем Андроид 7.0
                {
                    Log.d("RECEIVER:","Build.VERSION.SDK_INT "+String.valueOf(Build.VERSION.SDK_INT));
                    Log.d("","context.getApplicationContext().startForegroundService(i)");

                    Toast.makeText(context, "startForegroundService(i)", Toast.LENGTH_LONG).show();//вывод всплывающего окна

                    context.getApplicationContext().startForegroundService(i);
                }
                else {
                    Toast.makeText(context, "startService(i)", Toast.LENGTH_LONG).show();//вывод всплывающего окна

                    context.getApplicationContext().startService(i);//запуск сервиса, метод getApplicationContext() используется тк нужен "стабильный" контекст а не текущий контекст - обычный который передан в параметр метода onReceive приемник

                }




            }




        }



    }








}


