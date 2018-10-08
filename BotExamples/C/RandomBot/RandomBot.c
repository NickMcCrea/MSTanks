#include <stdio.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <stdarg.h>
#include <time.h>
#include <netdb.h> 
#include <arpa/inet.h>
#include <sys/types.h>     
#include <netinet/in.h>
#include <string.h>

#define MESSAGE_TEST 0
#define MESSAGE_CREATETANK 1
#define MESSAGE_DESPAWNTANK 2
#define MESSAGE_FIRE 3
#define MESSAGE_TOGGLEFORWARD 4
#define MESSAGE_TOGGLEREVERSE 5
#define MESSAGE_TOGGLELEFT 6
#define MESSAGE_TOGGLERIGHT 7
#define MESSAGE_TOGGLETURRETLEFT 8
#define MESSAGE_TOGGLETURRETRIGHT 9
#define MESSAGE_TURNTURRETTOHEADING 10
#define MESSAGE_TURNTOHEADING 11
#define MESSAGE_MOVEFORWARDDISTANCE 12
#define MESSAGE_MOVEBACKWARSDISTANCE 13
#define MESSAGE_STOPALL 14
#define MESSAGE_STOPTURN 15
#define MESSAGE_STOPMOVE 16
#define MESSAGE_STOPTURRET 17
#define MESSAGE_OBJECTUPDATE 18
#define MESSAGE_HEALTHPICKUP 19
#define MESSAGE_AMMOPICKUP 20
#define MESSAGE_SNITCHPICKUP 21
#define MESSAGE_DESTROYED 22
#define MESSAGE_ENTEREDGOAL 23
#define MESSAGE_KILL 24
#define MESSAGE_SNITCHCOLLECTED 25,
#define MESSAGE_SNITCHAPPEARED 26
#define MESSAGE_GAMETIMEUPDATE 27

struct netMessage {
    unsigned short int type;
    unsigned short int length;
    char payload[256];
};

void printMessage(const char *str, ...)
{
    /*
     * Print log entries to stderr prefixed by date and time
     */
    va_list ap;
    time_t now;
    struct tm *nowlocal;
    char buffer[256];
    
    va_start(ap, str);

    now = time(NULL);
    nowlocal = localtime(&now);

    strftime(buffer, sizeof(buffer), "[%Y/%m/%d:%H:%M:%S] ", nowlocal);
    fputs(buffer, stderr);
    vfprintf(stderr, str, ap);
    fprintf(stderr, "\n");
}

void copyAndStrip(char *src, char *dst)
{
    /*
     * Copy a string and strip certain characters from it on the fly. This
     * is horrible but the bare minimum required to work four our fake
     * JSON parsing.
     */
    for(; *src != '\0'; src++) {
        if(*src == '"' || *src == '{' || *src == '}' || *src == ':') {
            // We skip these elements
        } else {
            // Never do this. Unsafe pointer arithmetic as an intended side effect!
            *(dst++) = *src;
        }
    }
    // Terminate the string at last
    *dst = '\0';
}

char *fakeEncodeJson(char *name, char *value)
{
    /*
     * Our JSON output is very simple - one value at a time - so we can
     * very easily make one with raw sprintf()
     */
    static char jsonBuffer[256];
    
    sprintf(jsonBuffer, "{\"%s\":\"%s\"}", name, value);
    return (char*)&jsonBuffer;
}

char *fakeExtractValueFromJson(char *json, char *name)
{
    /*
     * Parse through a simple, flat JSON object looking for a given value. Will
     * never work in the real world. Does no type conversion, everything is a
     * string.
     */
    static char buffer[256];
    static char varname[256];
    static char value[256];
    char inputCopy[256];
    char *token;
    char *colon;
    const char sep[4] = ",";
    char *saveptr = inputCopy;
    
    strcpy(inputCopy, json);
    token = strtok_r(inputCopy, sep, &saveptr);

    while(token != NULL) {
        colon = strchr(token, ':');
        memset(buffer, 0, 255);
        strncpy(buffer, token, (strlen(token) - strlen(colon)));
        copyAndStrip(buffer, varname);
        copyAndStrip(colon, value);
        
        if(strcmp(varname, name) == 0) {
            return (char*)&value;
        }
        
        token = strtok_r(NULL, ",", &saveptr);
    }
    
    return NULL;
}

int connectToServer(char *hostname, unsigned int port)
{
    /*
     * Setup the network socket for comms
     */
    struct sockaddr_in address;
    int sock = 0;
    
    if((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        printMessage("Couldn't create socket");
        return -1;
    }
    
    memset(&address, 0, sizeof(address));
    address.sin_family = AF_INET;
    address.sin_port = htons(port);
    
    if(inet_pton(AF_INET, hostname, &address.sin_addr) <= 0) {
        printMessage("Address invalid or not supported");
        return -2;
    }
    
    if(connect(sock, (struct sockaddr*)&address, sizeof(address)) < 0) {
        printMessage("Couldn't connect to host/port");
        return -3;
    }
    
    return sock;
}

int readNetMessage(int sock, struct netMessage *message)
{
    /*
     * Read a message from the network with blocking I/O
     */
    unsigned char buffer[256];
    int len;
        
    memset(buffer, 0, 255);
    len = recv(sock, buffer, 1, 0);
    if(len == -1) {
        printMessage("Failed to read message type from network");
        return -1;
    }
    message->type = buffer[0];
        
    memset(buffer, 0, 255);
    len = recv(sock, buffer, 1, 0);
    if(len == -1) {
        printMessage("Failed to read message length from network");
        return -2;
    }
    message->length = buffer[0];
    
    if(message->length == 0) {
        printMessage("Got zero-length message");
        memset(message->payload, 0, 255);
        return 0;
    }
        
    memset(buffer, 0, 255);
    memset(message->payload, 0, 255);
    len = recv(sock, message->payload, message->length, 0);
    if(len == -1) {
        printMessage("Failed to read message payload from network");
        return -3;
    }
    
    return 0;
}

int buildNetMessage(int type, char *payload, struct netMessage *message)
{
    /*
     * Prepare a message for sending on the network
     */
    message->type = type;
    message->length = strlen(payload);
    strcpy(message->payload, payload);
    
    return 0;
}

int sendNetMessage(int sock, struct netMessage *message)
{
    /*
     * Send a prepared message out on the network
     */
    if(send(sock, &message->type, 1, 0) == -1) {
        printMessage("Failed to send message type to network");
        return -1;
    }
    
    if(send(sock, &message->length, 1, 0) == -1) {
        printMessage("Failed to send message length to network");
        return -2;
    }
    
    if(message->length > 0) {        
        if(send(sock, message->payload, message->length, 0) == -1) {
            printMessage("Failed to send message payload to network");
            return -3;
        }
    }
    
    return 0;
}


int main(int argc, char const *argv[])
{
    int sock;
    int i;
    struct netMessage inMessage;
    struct netMessage outMessage;
    char buffer[256];
    
    // Connect to our server
    sock = connectToServer("127.0.0.1", 8052);
    if(sock < 0) {
        printMessage("Can't connect, exiting");
        return -1;
    }
    
    // Spawn our tank
    buildNetMessage(MESSAGE_CREATETANK, fakeEncodeJson("Name", "RandomTank-C"), &outMessage);
    if(sendNetMessage(sock, &outMessage) != 0) {
        printMessage("Couldn't spawn tank, exiting");
        return -2;
    }
    
    // Main loop - read messages and randomy fire, turn and move
    i = 0;
    while(readNetMessage(sock, &inMessage) == 0) {
        printMessage("Got message type %d payload '%s'", inMessage.type, inMessage.payload);
        
        if(inMessage.type == MESSAGE_OBJECTUPDATE) {
            printMessage("Update was for object called %s", fakeExtractValueFromJson(inMessage.payload, "Name"));
        }
        
        if(i == 5) {
            if(rand() % 10 > 5) {
                printMessage("Randomly firing");
                buildNetMessage(MESSAGE_FIRE, "", &outMessage);
                sendNetMessage(sock, &outMessage);
            }
        } else if(i == 10) {
            printMessage("Turning randomly");
            sprintf(buffer, "%d", rand() % 359);
            buildNetMessage(MESSAGE_TURNTOHEADING, fakeEncodeJson("Amount", buffer), &outMessage);
            sendNetMessage(sock, &outMessage);
        } else if(i == 15) {
            printMessage("Moving randomly");
            sprintf(buffer, "%d", rand() % 10);
            buildNetMessage(MESSAGE_MOVEFORWARDDISTANCE, fakeEncodeJson("Amount", buffer), &outMessage);
            sendNetMessage(sock, &outMessage);
        }        
        i++;
        if(i > 20)
            i = 0;
    }


        
}
