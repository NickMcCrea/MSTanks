package main

import (
	"bufio"
	"fmt"
	"math/rand"
	"net"
	"strconv"
	"time"
)

const (
	create  byte = 1
	fire    byte = 3
	turn    byte = 11
	forward byte = 12
)

func main() {
	conn, err := net.Dial("tcp", "localhost:8052")
	if err != nil {
		fmt.Println(err)
	}
	createTank(conn)
	go readLoop(conn)
	writeLoop(conn)
}

func createTank(conn net.Conn) {
	namePayload := "{\"Name\":\"GoBot\"}"
	sendMessage(conn, create, namePayload)
}

func sendMessage(conn net.Conn, msgType byte, msg string) {
	msgBytes := make([]byte, 2)
	payload := []byte(msg)
	msgBytes[0] = msgType
	msgBytes[1] = byte(len(payload))
	msgBytes = append(msgBytes, payload...)
	conn.Write(msgBytes)
}

func readLoop(conn net.Conn) {
	readbuf := bufio.NewReader(conn)
	for {
		readbuf.ReadByte() // message type
		payloadLength, _ := readbuf.ReadByte()
		payload := make([]byte, payloadLength)
		readbuf.Read(payload)
		fmt.Println(string(payload))
	}
}

func writeLoop(conn net.Conn) {
	for {
		headingPayload := "{\"Amount\":" + strconv.Itoa(rand.Intn(359)) + "}"
		sendMessage(conn, turn, headingPayload)

		time.Sleep(50 * time.Millisecond)

		moveDistance := rand.Intn(30) + 20
		movePayload := "{\"Amount\":" + strconv.Itoa(moveDistance) + "}"
		sendMessage(conn, forward, movePayload)

		time.Sleep(2000 * time.Millisecond)
	}
}
