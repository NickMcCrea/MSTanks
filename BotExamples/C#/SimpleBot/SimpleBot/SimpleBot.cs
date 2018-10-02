using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Net.Sockets;
using System.Text;
using System.Threading;

namespace Simple
{
    public class SimpleBot
    {

        private string ipAddress = "127.0.0.1";
        private int port = 8052;
        private string tankName;


        //Our TCP client.
        private TcpClient client;

        //Thread used to listen to the TCP connection, so main thread doesn't block
        private Thread listeningThread;

        //store incoming messages on the listening thread,
        //before transfering them safely onto main thread.
        private Queue<byte[]> incomingMessages;

        public bool BotQuit { get; internal set; }

        public SimpleBot(string name="SimpleBot1")
        {
            tankName = name;

            incomingMessages = new Queue<byte[]>();

            ConnectToTcpServer();

            //wait for a bit to allow connection to establish before proceeding.
            Thread.Sleep(5000);


            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.test));


            //send the create tank request.
            SendMessage(MessageFactory.CreateTankMessage(tankName));

            //conduct basic movement requests.
            BasicTest();

        }

        private void ConnectToTcpServer()
        {
            try
            {
                //set up a TCP client on a background thread
                listeningThread = new Thread(new ThreadStart(ConnectAndListen));
                listeningThread.IsBackground = true;
                listeningThread.Start();
            }
            catch (Exception e)
            {
                Console.WriteLine("On client connect exception " + e);
            }
        }

        private void ConnectAndListen()
        {
            try
            {
                client = new TcpClient(ipAddress, port);

            
                // Get a stream object for reading 				
                using (NetworkStream stream = client.GetStream())
                {

                    while (client.Connected)
                    {
                        int type = stream.ReadByte();
                        int length = stream.ReadByte();

                        Byte[] bytes = new Byte[length];

                        //there's a JSON package
                        if (length > 0)
                        {
                            // Read incoming stream into byte arrary. 					
                            stream.Read(bytes, 0, length);

                            Byte[] byteArrayCopy = new Byte[length + 2];
                            bytes.CopyTo(byteArrayCopy, 2);
                            byteArrayCopy[0] = (byte)type;
                            byteArrayCopy[1] = (byte)length;

                            lock (incomingMessages)
                            {
                                incomingMessages.Enqueue(byteArrayCopy);
                            }
                        }
                        else
                        {

                            //no JSON
                            lock (incomingMessages)
                            {
                                byte[] zeroPayloadMessage = new byte[2];
                                zeroPayloadMessage[0] = (byte)type;
                                zeroPayloadMessage[1] = 0;
                                incomingMessages.Enqueue(zeroPayloadMessage);
                            }

                        }
     
                    }

                }

            }
            catch (SocketException socketException)
            {
                Console.WriteLine("Socket exception: " + socketException);
            }
        }

        private void DecodeMessage(NetworkMessageType messageType, int payloadLength, byte[] bytes)
        {
            try
            {
                string jsonPayload = "";
                if (payloadLength > 0)
                {
                    var payload = new byte[payloadLength];
                    Array.Copy(bytes, 2, payload, 0, payloadLength);
                    jsonPayload = Encoding.ASCII.GetString(payload);
                    //Console.WriteLine("Payload length: " + payloadLength);
                }

                if(messageType == NetworkMessageType.test)
                {
                    Console.WriteLine("TEST ACK RECEIVED");
                }

                if (messageType == NetworkMessageType.objectUpdate)
                {
                    //Console.WriteLine(jsonPayload);
                    GameObjectState objectState = JsonConvert.DeserializeObject<GameObjectState>(jsonPayload);


                    if (objectState.Name == tankName)
                    {
                        //it's our tank
                        //Console.WriteLine(objectState.Name + " - " + objectState.X + "," + objectState.Y + " : " + objectState.Heading + " : " + objectState.TurretHeading);

                    }
                    else
                    {
                        //it's something else.
                        //Console.WriteLine(objectState.Name + " - " + objectState.X + "," + objectState.Y + " : " + objectState.Heading + " : " + objectState.TurretHeading);
                    }
                }
                if(messageType == NetworkMessageType.healthPickup)
                {
                    Console.WriteLine("HEALTH PICKUP EVENT");
                } 
                if (messageType == NetworkMessageType.ammoPickup)
                {
                    Console.WriteLine("AMMO PICKUP EVENT");
                }
                if (messageType == NetworkMessageType.snitchPickup)
                {
                    Console.WriteLine("SNITCH PICKUP EVENT");
                }

            }
            catch (Exception e)
            {
                Console.WriteLine("Message decode exception " + e);
            }

        }

        private void SendMessage(byte[] message)
        {
            if (client == null)
            {
                return;
            }
            try
            {
                // Get a stream object for writing. 			
                NetworkStream stream = client.GetStream();
                if (stream.CanWrite)
                {
                    stream.Write(message, 0, message.Length);

                }
            }
            catch (SocketException socketException)
            {
                Console.WriteLine("Socket exception: " + socketException);
            }
        }

        public void Update()
        {

            if (incomingMessages.Count > 0)
            {
                var nextMessage = incomingMessages.Dequeue();
                DecodeMessage((NetworkMessageType)nextMessage[0], nextMessage[1], nextMessage);
            }

        }

        private void BasicTest()
        {

            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.forward));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.reverse));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.stop));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.left));
            Thread.Sleep(1000);


            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.right));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.stop));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.turretLeft));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.turretRight));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.stopTurret));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateZeroPayloadMessage(NetworkMessageType.fire));


            //SendMessage(MessageFactory.CreateMessage(NetworkMessageType.despawnTank));


        }

    }

    public static class MessageFactory
    {

        public static byte[] CreateTankMessage(string name)
        {

            string json = JsonConvert.SerializeObject(new { Name = name });
            byte[] clientMessageAsByteArray = Encoding.ASCII.GetBytes(json);
            return AddTypeAndLengthToArray(clientMessageAsByteArray, (byte)NetworkMessageType.createTank);
        }

        public static byte[] AddTypeAndLengthToArray(byte[] bArray, byte type)
        {
            byte[] newArray = new byte[bArray.Length + 2];
            bArray.CopyTo(newArray, 2);
            newArray[0] = type;
            newArray[1] = (byte)bArray.Length;
            return newArray;
        }

        public static byte[] CreateZeroPayloadMessage(NetworkMessageType type)
        {

            byte[] message = new byte[2];
            message[0] = (byte)type;
            message[1] = 0;
            return message;
        }


    }

    public enum NetworkMessageType
    {
        test = 0,
        createTank = 1,
        despawnTank = 2,
        fire = 3,
        forward = 4,
        reverse = 5,
        left = 6,
        right = 7,
        stop = 8,
        turretLeft = 9,
        turretRight = 10,
        stopTurret = 11,
        objectUpdate = 12,
        healthPickup = 13,
        ammoPickup = 14,
        snitchPickup = 15
    }

    public class GameObjectState
    {
        public string Name;
        public string Type;
        public float X;
        public float Y;
        public float ForwardX;
        public float ForwardY;
        public float Heading;
        public float TurretHeading;
        public float TurretForwardX;
        public float TurretForwardY;

        public int Health;
        public int Ammo;


    }

}
