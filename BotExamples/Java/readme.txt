Example run:

Maven package

then:

Minimal logging:

java -Dhost="192.168.1.146" -Dport="8052" -Dname="TeamA:Botimus"  -jar E:\Development\MSTanks\BotExamples\Java\target\MSTanksJavaBot-1.0-jar-with-dependencies.jar

Log all the things!!

java -Dhost="192.168.1.146" -Dport="8052" -Dname="TeamA:Botimus" -Dlogback.configurationFile="E:\Development\MSTanks\BotExamples\Java\src\main\resources\logback-debug.xml" -jar E:\Development\MSTanks\BotExamples\Java\target\MSTanksJavaBot-1.0-jar-with-dependencies.jar

