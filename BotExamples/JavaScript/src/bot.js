const TankBrain = require('./random_brain.js');
const TankRemote = require('./remote.js');

class TankBot {
    constructor(hostname, port, name) {
        this.remote = new TankRemote(hostname, port);
        this.brain = new TankBrain(name, this.remote);
        this.name = name;

        this.remote.onConnect.push((
            function(){
                this.remote.createTank(this.name);
            }
        ).bind(this));
    }
}

module.exports = TankBot;
