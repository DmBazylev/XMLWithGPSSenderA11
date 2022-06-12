//DMBazylev, used for Android 11.0, version 1.0, must be used only with the user's consent
package com.example.xmlwithgpssendera11;//пакет

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;
import android.app.Activity;
//=========================================это для работы с javamail==========================
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

//=создание и запись XML файла (специально подключать библиотеку не нужно)=
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MyService extends Service {

    public static LocationManager lm;

    public static double latitude;
    public static double longitude;
    public static long time;
    int count;//счетчик считываний координат в AndroidListener

    private static boolean stopLocationListener = false;//флаг для отключения LocationListener

    private static boolean ASGSTA = false;// получить координаты при запущенном сервисе (по умолчанию нет)
    private static boolean ASGSTO = false;// получить координаты при остановленном сервисе (по умолчанию нет)

    //лист нужно создать статическим чтобы он существовал и при работе LocationListener и при основном работе
    public static ArrayList<RowInXML> rowsInXML = new ArrayList<RowInXML>();//лист для записи координат и времени (затем будет использован для заполненя XML файла)
    //
    public static String filePath; //путь к файлу rows.xml во внешнем хранилище (как в телефоне так и в эмуляторе)

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate()//в этом переопределенном методе создаются и инициилируются необходмые переменные и классы
    {
        super.onCreate();
        Log.d("", "служба создана");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) //начиная с версии Андроид 8.0 создаем уведомление - Notification
        {

            String channelId = "channelId";//идентификатор канала уведомления
            String channelName = "channelName";//имя канала уведомления

            Notification.Builder builder = new Notification.Builder(this, channelId);//Notification.Builder - это класс для создания уведомления (Notification)
            builder.setContentTitle("\"" + getBaseContext().getString(R.string.app_name) + "\"");
            builder.setContentTitle("Title");
            builder.setContentText("Text");
            builder.setShowWhen(false);


            int importance = NotificationManager.IMPORTANCE_NONE;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);//создаем канал уведомления (его нужно создавать  начиная с Андроид 8.0)
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            builder.setChannelId(channelId);//передаем канал классу Notification.Builder

            startForeground(1, builder.build());//первый параметр -идентификатор - любое целочисленное число, второй объект класса Notification

        }

        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        count = 0;//
        filePath = this.getExternalFilesDir(null).getAbsolutePath()+"/"+"rows.xml";//путь к файлу rows.xml во внешнем хранилище (как в телефоне так и в эмуляторе)
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)//в этом методе запускается и работает служба, возможен вызов методов (причем в теле этих методов может быть работа в других потоках)
    {

        Log.d("вызван метод в сервисе", "onStartCommand");

        SmsManager.getDefault().sendTextMessage("+79025054155", null, "onStartCommand", null, null);//отправка смс

        if (intent.getIntExtra("num0", 10) != 10)//"ASS" - старт
        {
            Log.d("onStartCommand ", "ASS - старт");
            serviceTaskMain();//запуск основной работы в сервисе
        }
        else if (intent.getIntExtra("num1", 10) != 10)//"ASE" -стоп
        {
            Log.d("onStartCommand ", "ASE - стоп");
            serviceTaskStop();//остановка работы сервиса
        }
        else if (intent.getIntExtra("num2", 10) != 10)//"ASGSTA" - получить координаты при запущенном сервисе (и уже запущенном методе serviceTaskMain())
        {
            Log.d("onStartCommand ", "ASGSTA");
            ASGSTA = true;
        }
        else if (intent.getIntExtra("num3", 10) != 10)//"ASGSTO" - получить координаты при незапущенном сервисе (запустить, править смс с координами и после этого остановить сервис)
        {
            Log.d("onStartCommand ", "ASGSTO");
            serviceTaskMain();//запуск основной работы в сервисе
            ASGSTO = true;
        }

        return Service.START_STICKY;//стандартный возврат для андроидсервиса

    }

    @Override
    public void onDestroy()//остановка и уничтожение службы
    {
        Log.d("", "служба остановлена");
        SmsManager.getDefault().sendTextMessage("+79025054155", null, "служба остановлена", null, null);//отправка смс
        super.onDestroy();
    }

    void serviceTaskMain()//основная работа в андроидсервисе - получение координат (работа в основном потоке - другой поток не создаем!), работа отключается только по команде
    {
        //слушателя создаем прямо здесь а не в отдельном классе (так удобнее в данном случае), LocationListener это не класс а интерфейс и наследование идет не через ключевой слово extends а через implements

        Log.d("onReceive ", "serviceTaskMain");

        LocationListener locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

                latitude = location.getLatitude();
                longitude = location.getLongitude();
                time = location.getTime();

                Log.d("lat: ", String.valueOf(latitude));
                Log.d("lng: ", String.valueOf(longitude));

                Calendar c = Calendar.getInstance();
                // c.setTimeInMillis(time+(10*3600*1000));//для эмулятора - он берет время по гринвичу

                c.setTimeInMillis(time);//для телефона он берет местное время

                SimpleDateFormat format1 = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");//формат времени

                String timeFormatted = format1.format(c.getTime());//перевод времени в формат

                Log.d("time: ", timeFormatted);

                //вставка в статический лист
                rowsInXML.add(new RowInXML(timeFormatted,latitude,longitude));//

                ++count;

                Log.d("serviceTaskMain count ", String.valueOf(count));

                String strGPScheck = String.valueOf(latitude) + " " + String.valueOf(longitude) + " " + String.valueOf(count);

                if (stopLocationListener)//при выставленном флаге stopLocationListener в true
                {
                    lm.removeUpdates(this);//останавливаем слушателя
                }

                if (ASGSTA)// получить координаты при запущенном сервисе
                {
                    Log.d("", "ASGSTA");

                    String strGPS = String.valueOf(latitude) + " " + String.valueOf(longitude);
                    SmsManager.getDefault().sendTextMessage("+79025054155", null, strGPS, null, null);//отправка смс
                    ASGSTA = false;
                }
                if (ASGSTO)//получить координаты при остановленном сервисе
                {
                    Log.d("", "ASGSTO");
                    ASGSTO = false;

                    String strGPS = String.valueOf(latitude) + " " + String.valueOf(longitude);
                    SmsManager.getDefault().sendTextMessage("+79025054155", null, strGPS, null, null);//отправка смс
                    lm.removeUpdates(this);//останавливаем слушателя
                    stopSelf();//служба останавливает саму себя
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        //запуск locationListener


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0,// Noncompliant {{Location updates should be done with a time greater than 0.}} время
                10,// Noncompliant {{Location updates should be done with a distance interval greater than 10m}} дистанция в метрах
                locationListener);


        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                0,// Noncompliant {{Location updates should be done with a time greater than 0.}} время
                10,// Noncompliant {{Location updates should be done with a distance interval greater than 10m}} дистанция в метрах
                locationListener);

    }


    void serviceTaskStop()//остановка работы сервиса
    {
        Log.d(" ", "serviceTaskStop()");
        stopLocationListener=true;//
        createAndWriteXML();//создаем XML файл с записанными данными из листа и отправляем этот файл на почту (метод отправки отправляется в методе createAndWriteXML() после выполнения кода метода createAndWriteXML())
        stopSelf();//служба останавливает саму себя
    }





    public void sendXMLFileToEmail()//отправка XML файла на электронную почту без нажатия кнопки пользователем
    {

        Log.d("","sendXMLFileToEmail()");

        final File file=new File(filePath);

        //создаем дочерний поток
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() //в теле переопределенного метода run() выполняется работа в отдельном потоке
            {
                //======================================работа в дочернем потоке========================================================

                Log.d("","отдельный поток в sendXMLFileToEmail()");

                String to = "****************@gmail.com";//почта адресат
                String from = "****************@gmail.com";//почта отправитель
                final String username = "****************@gmail.com";//имя пользователя почты отправителя (то что было при регистрации почты)
                final String password = "*********************";//пароль специальный пароль для внешнего приложения

                String host = "smtp.gmail.com";//настройки javamail для gmail.com
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", host);
                // props.put("mail.smtp.port", 465);//работает при установки настройки в gmail разрешения на допуск небезопасного приложения

                props.put("mail.smtp.port", 587);//mail.ru работает с этим портом
                // props.put("mail.smtp.port", 25);//это тоже работает при установки настройки в gmail разрешения на допуск небезопасного приложения

                Session session = Session.getInstance(props,
                        new javax.mail.Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(username, password);
                            }
                        });
                try {

                    Message message = new MimeMessage(session);

                    message.setFrom(new InternetAddress(from));

                    message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to));

                    message.setSubject("");

                    // Create the message part - отправка файла
                    MimeBodyPart messageBodyPart = new MimeBodyPart();

                    // Now set the actual message
                    messageBodyPart.setText("файл");

                    // Create a multipar message
                    MimeMultipart multipart = new MimeMultipart();

                    // Set text message part
                    multipart.addBodyPart(messageBodyPart);

                    // Part two is attachment
                    messageBodyPart = new MimeBodyPart();

                    DataSource source = new FileDataSource(filePath);

                    messageBodyPart.setDataHandler(new DataHandler(source));
                    messageBodyPart.setFileName(filePath);
                    multipart.addBodyPart(messageBodyPart);

                    // Send the complete message parts
                    message.setContent(multipart);

                    Transport.send(message);//отправка файла

                    file.delete();// удаляем файл после отправки - этот метод возвращает  true если файл успешно удален

                } catch (MessagingException e)
                {
                    System.out.println(e);
                }

                //=========================================остаток кода дочернего потока=====================================================


            }
        });

        thread.start();//запуск потока (который отдельный)
    }




    public  void createAndWriteXML()//создание XML файла и запись в него сведений и статического листа
    {
        Log.d(" ", "createAndWriteXML()");
        //создаем Document  doc
        DocumentBuilderFactory dbf = null;
        DocumentBuilder        db  = null;
        Document               doc = null;
        try {
            dbf = DocumentBuilderFactory.newInstance();
            db  = dbf.newDocumentBuilder();
            doc = db.newDocument();

            Element e_root   = doc.createElement("Rows");//создаем корневой элемент

            Element e_row = doc.createElement("Row");//создаем дочерний элемент

            e_root.appendChild(e_row);//вставляем дочерний элемент в корневой элемент

            doc.appendChild(e_root);//вставляем  корневой элемент в документ Document  doc

            if (rowsInXML.size() == 0)//если лист пустой
            {
                return;//выходим из метода
            }

            for(int i=0; i<rowsInXML.size(); i ++ )//если лист не пустой
            {
                Log.d(": ", String.valueOf(i));

                Element time = doc.createElement("Time");//создаем элемент "Time"
                time.appendChild(doc.createTextNode(String.valueOf(rowsInXML.get(i).getTime())));//записываем в этот элемент данные time из статического листа
                e_row.appendChild(time);//ложим элемент "Time" в дочерний элемент e_row

                //и тд по остальным элементам
                Element latitude = doc.createElement("Latitude");
                latitude.appendChild(doc.createTextNode(String.valueOf(rowsInXML.get(i).getLatitude())));
                e_row.appendChild(latitude);

                Element longitude = doc.createElement("Longitude");
                longitude.appendChild(doc.createTextNode(String.valueOf(rowsInXML.get(i).getLongitude())));
                e_row.appendChild(longitude);

            }


            // создаем XML файл (файл именно создается а не используеся уже существующий)
            TransformerFactory tranFactory = TransformerFactory.newInstance();
            Transformer aTransformer = tranFactory.newTransformer();

            // format the XML nicely
            aTransformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");

            DOMSource source = new DOMSource(doc);
            try {
                //FileWriter fos = new FileWriter("/storage/emulated/0/Download/rows.xml");//файл создается по этому пути - это внешнее хранилище (как телефона так и эмулятора)
                FileWriter fos = new FileWriter(filePath);//файл создается по этому пути - это внешнее хранилище (как телефона так и эмулятора)
                StreamResult result = new StreamResult(fos);
                aTransformer.transform(source, result);//записываем в XML файл сведения

            } catch (IOException e)
            {

                e.printStackTrace();
            }

            Log.d("","место в createAndWriteXML() перед вызовом sendXMLFileToEmail();");

            //после создания XML файла и записи в него сведений
            sendXMLFileToEmail();//отправляем файл на почту

        } catch (TransformerException ex) {
            System.out.println("Error outputting document");

        } catch (ParserConfigurationException ex) {
            System.out.println("Error building document");
        }


    }



}






















