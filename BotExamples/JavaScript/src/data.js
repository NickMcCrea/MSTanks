const OUTDATE_MS = 2000;

class TankData {
    constructor(tankValues) {
        this.updateData(tankValues);
    }

    updateData(tankValues) {
        this.x = tankValues["X"];
        this.y = tankValues["Y"];
        this.id = tankValues["Id"];
        this.name = tankValues["Name"];
        this.type = tankValues["Type"];
        this.heading = tankValues["Heading"];
        this.turretHeading = tankValues["TurretHeading"];
        this.health = tankValues["Health"];
        this.ammo = tankValues["Ammo"];
        this.updateTime = Date.now();
    }

    isOutDate(){
        return (Date.now() - this.updateTime) >= OUTDATE_MS;
    }
}

module.exports = TankData;