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

        private string ipAddress = "localhost";
        private int port = 8052;

        
        //Our TCP client.
        private TcpClient client;

        //Thread used to listen to the TCP connection, so main thread doesn't block
        private Thread listeningThread;

        //store incoming messages on the listening thread,
        //before transfering them safely onto main thread.
        private Queue<byte[]> incomingMessages;

        public bool BotQuit { get; internal set; }

        public SimpleBot(string ip, int port, string name, string colour)
        {


            incomingMessages = new Queue<byte[]>();

            ConnectToTcpServer();

            //wait for a bit to allow connection to establish before proceeding.
            Thread.Sleep(5000);


            //SendMessage(MessageFactory.CreateMessage(NetworkMessageType.test, ""));


            //send the create tank request.
            SendMessage(MessageFactory.CreateTankMessage(name, colour));


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

                //this will  hold our message data.
                Byte[] bytes = new Byte[512];

                while (true)
                {
                    // Get a stream object for reading 				
                    using (NetworkStream stream = client.GetStream())
                    {

                        int length;
                      

                        // Read incoming stream into byte arrary. 					
                        while ((length = stream.Read(bytes, 0, bytes.Length)) != 0)
                        {

                           //for(int i = 0; i < bytes.Length-1; i++)
                           // {
                           //     if (bytes[i] == 125)
                           //         if (bytes[i + 1] == 125)
                           //         {
                           //             Debugger.Break();
                           //             Console.WriteLine("Gubbed JSON");
                           //         }
                           // }

                            lock (incomingMessages)
                            {
                                Byte[] byteArrayCopy = new Byte[512];
                                bytes.CopyTo(byteArrayCopy, 0);
                                incomingMessages.Enqueue(byteArrayCopy);
                                bytes = new Byte[512];
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

        private void DecodeMessage(NetworkMessageType messageType, byte[] bytes, int length)
        {
            try
            {
                var incomingData = new byte[length];
                Array.Copy(bytes, 1, incomingData, 0, length - 1);
                string clientMessage = Encoding.ASCII.GetString(incomingData);
                Console.WriteLine(messageType.ToString() + " -- " + clientMessage);

                if(messageType == NetworkMessageType.objectUpdate)
                {
                    GameObjectState objectState = JsonConvert.DeserializeObject<GameObjectState>(clientMessage);
                    Console.WriteLine(objectState.Name + " - " + objectState.X + "," + objectState.Y + " : " + objectState.Heading + " : " + objectState.TurretHeading);

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
                DecodeMessage((NetworkMessageType)nextMessage[0], nextMessage, nextMessage.Length);
            }

        }

        private void BasicTest()
        {

            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateMessage(NetworkMessageType.forward, ""));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateMessage(NetworkMessageType.reverse, ""));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateMessage(NetworkMessageType.stop, ""));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateMessage(NetworkMessageType.left, ""));
            Thread.Sleep(1000);


            SendMessage(MessageFactory.CreateMessage(NetworkMessageType.right, ""));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateMessage(NetworkMessageType.stop, ""));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateMessage(NetworkMessageType.turretLeft, ""));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateMessage(NetworkMessageType.turretRight, ""));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateMessage(NetworkMessageType.stopTurret, ""));
            Thread.Sleep(1000);

            SendMessage(MessageFactory.CreateMessage(NetworkMessageType.fire, ""));


            //SendMessage(MessageFactory.CreateMessage(NetworkMessageType.despawnTank, ""));


        }

    }

    public static class MessageFactory
    {

        public static byte[] CreateTankMessage(string name, string color)
        {

            string json = JsonConvert.SerializeObject(new { Name = name});
            byte[] clientMessageAsByteArray = Encoding.ASCII.GetBytes(json);
            return AddByteStartOfToArray(clientMessageAsByteArray, (byte)NetworkMessageType.createTank);
        }

        public static byte[] AddByteStartOfToArray(byte[] bArray, byte newByte)
        {
            byte[] newArray = new byte[bArray.Length + 1];
            bArray.CopyTo(newArray, 1);
            newArray[0] = newByte;
            return newArray;
        }

        public static byte[] CreateMessage(NetworkMessageType type, string message)
        {
            byte[] clientMessageAsByteArray = Encoding.ASCII.GetBytes(message);
            return AddByteStartOfToArray(clientMessageAsByteArray, (byte)type);
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
        objectUpdate = 12
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
