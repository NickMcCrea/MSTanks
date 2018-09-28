using System.Threading;

namespace Simple
{
    class Program
    {
        static void Main(string[] args)
        {
            SimpleBot bot = new SimpleBot("http://localhost", 8000, "NickBot", "#EA9414");


            while (!bot.BotQuit)
            {

                bot.Update();

                //run at 60Hz
                Thread.Sleep(16);

            }
        }
    }
}
