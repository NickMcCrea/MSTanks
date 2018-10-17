const Calculator = require('./calculator.js');
const TankData = require('./data.js');

const events = {
    OBJECT_UPDATE  : 18,
    HEALTH_PICKUP  : 19,
    AMMO_PICKUP    : 20,
    SNITCH_PICKUP  : 21,
    DESTROYED     : 22,
    ENTERED_GOAL   : 23,
    KILL          : 24,
    SNITCH_APPEARED : 25,
    GAME_TIME_UPDATE : 26,
    HIT_DETECTED   : 27,
    SUCCESSFULL_HIT : 28
};

const eventString = {
    18      :"Object Update",
    19      :"Health Pickup",
    20      :"Ammo Pickup",
    21      :"Snitch Pickup",
    22      :"Destroyed",
    23      :"Entered Goal",
    24      :"Kill",
    25      :"Snitch Appeared",
    26      :"Game Time Update",
    27      :"Hit Detected",
    28      :"Successful Hit"
};

const PERFORM_RATE_INTERVAL = 100;

class TankBrain {
    constructor(name, remote) {
        this.name = name;
        this.lastPerformTime = Date.now();
        this.data = null; // will fetch later

        this.calculator = new Calculator(this);
        this.control = remote;
        this.control.setTarget(this);

        setInterval(this.thinkAndPerform.bind(this), PERFORM_RATE_INTERVAL);
    }

    updateSelfTankData(tankValues) {
        this.data = new TankData(tankValues);
    }

    notReadyToPerform() {
        return this.data === null || this.control === null;
    }

    messageIsFlooding() {
        return (Date.now() - this.lastPerformTime) < PERFORM_RATE_INTERVAL;
    }

    updateLastPerformTime() {
        this.lastPerformTime = Date.now();
    }

    memorise(eventType, values) {
        console.log("event " + eventString[eventType] + " received");
        console.log(values);
        console.log();

        switch(eventType) {
            case events.OBJECT_UPDATE: {
                if(values["Type"] == "Tank" && values["Name"] == this.name)
                    this.updateSelfTankData(values);
                break;
            }

            case events.HEALTH_PICKUP: {
                break;
            }

            case events.AMMO_PICKUP: {
                break;
            }

            case events.SNITCH_PICKUP: {
                break;
            }
            case events.DESTROYED: {
                break;
            }

            case events.ENTERED_GOAL: {
                break;
            }

            case events.KILL: {
                break;
            }

            case events.SNITCH_APPEARED: {
                break;
            }

            case events.GAME_TIME_UPDATE: {
                break;
            }

            case events.HIT_DETECTED: {
                break;
            }

            case events.SUCCESSFULL_HIT: {
                break;
            }
        }
    }

    thinkAndPerform() {
        if(this.notReadyToPerform())
            return;
        if(this.messageIsFlooding())
            return;
        this.updateLastPerformTime();

        /* your code now */
        // note : only 1 action should be thinkAndPerform to maintain 10 message/sec discipline
        var randomA = this.calculator.randomInt(1,4); // random [1,15]

        if(randomA == 1)
        {
            var randomB = this.calculator.randomInt(0,12);
            switch (randomB) {
                case 0: this.control.toggleForward(); break;
                case 1: this.control.toggleBackward(); break;
                case 2: this.control.toggleTurnLeft(); break;
                case 3: this.control.toggleTurnRight(); break;
                case 4: this.control.toggleTurretLeft(); break;
                case 5: this.control.toggleTurretRight(); break;
                case 6: this.control.fire(); break;
                case 7: this.control.turnBodyToHeading(0); break;
                case 8: this.control.turnTurretToHeading(90); break;
                case 9: this.control.stopAll(); break;
                case 10: this.control.stopMove(); break;
                case 11: this.control.stopTurn(); break;
                case 12: this.control.stopTurret(); break;
            }
        }
    }
}

module.exports = TankBrain;