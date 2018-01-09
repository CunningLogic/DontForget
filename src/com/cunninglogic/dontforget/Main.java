package com.cunninglogic.dontforget;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {

    /*ToDo
        Reducing the size of dont_forget_about_jcase would work fine, now that this is automated, and would greatly
        speed up the exploit.

        We need to craft seperate payloads for RC and GL, until then this won't work
     */

    private static SerialPort activePort;

    private static ClassLoader classLoader;

    private static FTPClient ftpClient;
    
    private static String VERSION = "1.0.0";

    private static int mode = 0;

    public static void main(String[] args) {
        System.out.println("Don't Forget v" + VERSION + " by Jon 'jcase' Sawyer - jcase@cunninglogic.com");
        System.out.println();
        System.out.println("PayPal Donations - > jcase@cunninglogic.com");
        System.out.println("Bitcoin Donations - > 1LrunXwPpknbgVYcBJyDk6eanxTBYnyRKN");
        System.out.println("Bitcoin Cash Donations - > 1LrunXwPpknbgVYcBJyDk6eanxTBYnyRKN");
        System.out.println("Amazon giftcards, plain thank yous or anything else -> jcase@cunninglogic.com");
        System.out.println("Any donations in excess of needed HW costs go to my local Special Olympics team.");
        System.out.println("Over the last 2 years, myself and reesarch partners have raised over 5 figures for the Orcas through bugs and bug bounties.");
        System.out.println();



        classLoader = Main.class.getClassLoader();
/*
        if (args.length != 1 || (args.length != 1 && (!args[0].equals("AC") || !args[0].equals("RC") || !args[0].equals("GL")))){
            System.out.println("java -jar DontForget.jar <mode>");
            System.out.println("Modes:");
            System.out.println("\tAC - Aircraft");
            System.out.println("\tRC - Remote");
            System.out.println("\tGL - Goggles");
            return;
        }
        */

        if (args[0].equals("RC")) {
            mode = 1;
            System.out.println("RC Mode");
        } else if (args[0].equals("GL")) {
            mode = 2;
            System.out.println("GL Mode");
        } else {
            System.out.println("AC Mode");
        }


        int count = 1;

        System.out.println("Choose target port: (* suggested port)");
        for (SerialPort s : SerialPort.getCommPorts()) {
            if (s.getDescriptivePortName().contains("DJI")) {
                System.out.print("*");
            }

            System.out.println("\t[" + count + "] " + s.getSystemPortName() + " : " + s.getDescriptivePortName());
            count++;
        }

        System.out.println("\t[E] Exit");

        String str = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Choose port: ");
        while (true) {
            try {
                str = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {

                if (str.toLowerCase().toLowerCase().equals("e")) {
                    System.out.println("Exiting");
                    System.exit(0);
                }


                int port = Integer.parseInt(str.trim());

                if ((port > count) || (port < 1)) {
                    System.out.println("[!] Invalid port selection");
                    System.out.print("Choose port: ");
                } else {
                    activePort = SerialPort.getCommPorts()[port - 1];
                    System.out.println("Using Port: " + activePort.getSystemPortName());
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid port selection");
                System.out.print("Choose port: ");
            }
        }

        if (activePort == null) {
            System.out.println("Couldn't find port, exiting");
            return;
        }

        if (activePort.isOpen()) {
            System.out.println(activePort.getSystemPortName() + " is already open");
            activePort.closePort(); //meh why not
            return;
        }

        if (!activePort.openPort()) {
            System.out.println("Couldn't open port, please close all other DUML/DJI Apps and try again");
            activePort.closePort(); //meh why not
            return;
        }

        activePort.setBaudRate(115200);

        InputStream payload = null;

        if (mode == 1) {
            //ToDo paylaod for RC
        } else if (mode == 2) {
            //ToDo payload for GL
        } else {
            //AC
            payload = classLoader.getResourceAsStream("resources/mavic_payload.tar.gz");
        }

        byte[] file = null;
        try {
           file = isToArray(classLoader.getResourceAsStream("resources/payload.tar.gz"));
            payload.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Sending upgrade command");
        write(getUpgradePacket());

        System.out.println("Sending filesize command");
        write(getFileSizePacket(file.length));

        System.out.println("Uploading payload");

        ftpClient = new FTPClient();
        try {
            ftpClient.connect("192.168.42.2", 21);
            ftpClient.login("dontforget","aboutjcase");
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(ftpClient.BINARY_FILE_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        payload = classLoader.getResourceAsStream("resources/payload.tar.gz");
        boolean done = false;
        try {
             done = ftpClient.storeFile("/upgrade/dji_system.bin", payload);
            payload.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Sleep so file system can catch up
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Sending hash/start command");
        write(getHashPacket(file));

        if (done) {
            InputStream dontforget = classLoader.getResourceAsStream("resources/dontforget");

            int countdown = 180; // 1 minute by half seconds

            boolean winner = false;

            while (!winner) {

                try {
                    FTPFile[] files = ftpClient.listFiles("/upgrade/upgrade/signimgs");

                    for (FTPFile f : files) {
                        if (f.getName().equals("jcase")) {
                            System.out.println("Overwriting wm220.cfg.sig");
                            ftpClient.setFileType(ftpClient.BINARY_FILE_TYPE);
                            winner = ftpClient.storeFile("/upgrade/upgrade/signimgs/jcase", dontforget);
                            break;
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                countdown--;

                if (countdown < 1) {
                    break;
                }
            }

            if (winner) {
                System.out.println("Try downgrading!.");
            } else {
                System.out.println("Something went wrong during the overwrite, reboot and try again.");
            }

            try {
                dontforget.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Failed to upload payload");
        }

        try {
            ftpClient.disconnect();
            activePort.closePort();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static byte[] getFileSizePacket(int fileSize) {
        byte[] packet = new byte[] {0x55, 0x1A, 0x04, (byte)0xB1, 0x2A, 0x28, 0x6B, 0x57, 0x40, 0x00, 0x08, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x04};

        if (mode == 1) {
            packet = new byte[] {0x55, 0x1A, 0x04, (byte)0xB1, 0x2A, 0x2D, (byte)0xEC, 0x27, 0x40, 0x00, 0x08, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x04};
        } else if (mode == 2) {
            packet = new byte[] {0x55, 0x1A, 0x04, (byte)0xB1, 0x2A, 0x3C, (byte)0xFD, 0x35, 0x40, 0x00, 0x08, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x04};
        }

        byte[] size = ByteBuffer.allocate(4).putInt(fileSize).array();

        packet[12] = size[3];
        packet[13] = size[2];
        packet[14] = size[1];
        packet[15] = size[0];

        return  CRC.pktCRC(packet);
    }

    private static void write(byte[] packet) {
        activePort.writeBytes(packet,packet.length);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static byte[] isToArray(InputStream is) throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    private static byte[] getUpgradePacket() {

        byte[] packet = new byte[] {0x55, 0x16, 0x04, (byte)0xFC, 0x2A, 0x28, 0x65, 0x57, 0x40, 0x00, 0x07, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x27, (byte)0xD3};

        if (mode == 1) {
            packet = new byte[] {0x55, 0x16, 0x04, (byte)0xFC, 0x2A, 0x2D, (byte)0xE7, 0x27, 0x40, 0x00, 0x07, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x9F, 0x44};
        } else if (mode == 2) {
            packet = new byte[] {0x55, 0x16, 0x04, (byte)0xFC, 0x2A, 0x3C, (byte)0xF7, 0x35, 0x40, 0x00, 0x07, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x29};
        }

        return packet;
    }

    private static byte[] getHashPacket(byte[] payload) {
        byte[] packet  = new byte[] {0x55, 0x1E, 0x04, (byte)0x8A, 0x2A, 0x28, (byte)0xF6, 0x57, 0x40, 0x00, 0x0A, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        if (mode == 1) {
            packet = new byte[] {0x55, 0x1E, 0x04, (byte)0x8A, 0x2A, 0x2D, 0x02, 0x28, 0x40, 0x00, 0x0A, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        } else if (mode == 2) {
            packet = new byte[] {0x55, 0x1E, 0x04, (byte)0x8A, 0x2A, 0x3C, 0x5B, 0x36, 0x40, 0x00, 0x0A, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }

        byte[] md5 = payload;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md5 = md.digest(md5);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        System.arraycopy(md5,0, packet, 12, 16);

        return  CRC.pktCRC(packet);
    }
}
