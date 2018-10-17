var net = require('net');
const utf8 = require('utf8');

const commands =
{
    TEST: 0,
    CREATE_TANK: 1,
    DESPAWN_TANK: 2,
    FIRE: 3,
    TOGGLE_FORWARD: 4,
    TOGGLE_REVERSE:  5,
    TOGGLE_LEFT: 6,
    TOGGLE_RIGHT: 7,
    TOGGLE_TURRET_LEFT: 8,
    TOGGLE_TURRET_RIGHT: 9,
    TURN_TURRET_TO_HEADING: 10,
    TURN_TO_HEADING: 11,
    MOVE_FORWARD_DISTANCE: 12,
    MOVE_BACKWARD_DISTANCE: 13,
    STOP_ALL: 14,
    STOP_TURN: 15,
    STOP_MOVE: 16,
    STOP_TURRET: 17,
};

class TankRemote
{
    constructor(hostname, port)
    {
        this.hostname = hostname;
        this.port = port;
        this.socket = new net.Socket();
        this.tankBrain = null;
        this.onConnect = [];

        console.log("Attempt to connect to http://" + hostname + ":" + port);
        this.socket.connect(port, hostname, (()=>{
            console.log('Successfully connected to http://' + this.hostname + ':' + this.port);
            for(let i = 0 ; i < this.onConnect.length; i++)
                this.onConnect[i]();
        }).bind(this));

        this.socket.on('data', (function(data) {
            const bytes = Uint8Array.from(data);
            const rawString = data+'';
            let i = 0;

            while ( i < bytes.length )
            {
                const type = bytes[i++];
                const len = bytes[i++];
                const jsonString = rawString.slice(i,i+len);
                i += len;

                if(this.tankBrain === null)
                    continue;

                if(jsonString.length <= 3)
                    continue;

                this.tankBrain.memorise(type,JSON.parse(jsonString));
                // console.log(jsonString);
            }
        }).bind(this));

        this.socket.on('close', function() {
            console.log('Disconnected from http://' + hostname + ':' + port);
        });
    }

    setTarget(brain){
        this.tankBrain = brain;
    }

    createTank(name){
        var cmd = '{"Name":"' + name + '"}';
        var header = new Uint8Array([commands.CREATE_TANK, cmd.length+1]);
        this.socket.write(header);
        this.socket.write(utf8.encode(cmd));
    }

    despawnTank() {
        var header = new Uint8Array([commands.DESPAWN_TANK, 0]);
        this.socket.write(header);
    }

    fire() {
        var header = new Uint8Array([commands.FIRE, 0]);
        this.socket.write(header);
    }

    toggleForward() {
        var header = new Uint8Array([commands.TOGGLE_FORWARD, 0]);
        this.socket.write(header);
    }

    toggleBackward() {
        var header = new Uint8Array([commands.TOGGLE_REVERSE, 0]);
        this.socket.write(header);
    }

    toggleTurnLeft() {
        var header = new Uint8Array([commands.TOGGLE_LEFT, 0]);
        this.socket.write(header);
    }

    toggleTurnRight() {
        var header = new Uint8Array([commands.TOGGLE_RIGHT, 0]);
        this.socket.write(header);
    }

    toggleTurretLeft() {
        var header = new Uint8Array([commands.TOGGLE_TURRET_LEFT, 0]);
        this.socket.write(header);
    }

    toggleTurretRight() {
        var header = new Uint8Array([commands.TOGGLE_TURRET_RIGHT, 0]);
        this.socket.write(header);
    }

    turnTurretToHeading(amount) {
        var cmd = '{ "Amount" : ' + amount + ' }';
        var header = new Uint8Array([commands.TURN_TURRET_TO_HEADING, cmd.length+1]);
        this.socket.write(header);
        this.socket.write(utf8.encode(cmd));
    }

    turnBodyToHeading(amount) {
        var cmd = '{ "Amount" : ' + amount + ' }';
        var header = new Uint8Array([commands.TURN_TO_HEADING, cmd.length+1]);
        this.socket.write(header);
        this.socket.write(utf8.encode(cmd));
    }

    moveForward(amount)
    {
        var cmd = '{ "Amount" : ' + amount + ' }';
        var header = new Uint8Array([commands.MOVE_FORWARD_DISTANCE, cmd.length+1]);
        this.socket.write(header);
        this.socket.write(utf8.encode(cmd));
    }

    moveBackward(amount) {
        var cmd = '{ "Amount" : ' + amount + ' }';
        var header = new Uint8Array([commands.MOVE_BACKWARD_DISTANCE, cmd.length+1]);
        this.socket.write(header);
        this.socket.write(utf8.encode(cmd));
    }

    stopAll() {
        var header = new Uint8Array([commands.STOP_ALL, 0]);
        this.socket.write(header);
    }

    stopTurn() {
        var header = new Uint8Array([commands.STOP_TURN, 0]);
        this.socket.write(header);
    }

    stopMove() {
        var header = new Uint8Array([commands.STOP_MOVE, 0]);
        this.socket.write(header);
    }

    stopTurret() {
        var header = new Uint8Array([commands.STOP_TURRET, 0]);
        this.socket.write(header);
    }
}

module.exports = TankRemote;