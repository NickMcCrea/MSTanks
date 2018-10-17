class Calculator {
    constructor(myTank){
        this.myTank = myTank.data;
    }

    maxHealth() {
        return this.myTank.health == 5;
    }

    lowHealth() {
        return this.myTank.health <= 2;
    }

    outOfAmmo(ammo){
        return this.myTank.ammo == 0;
    }

    distance(x1,x2,y1,y2){
        return Math.sqrt((x1 - x2)**2 + (y1 - y2)**2)
    }

    degreeBetween(fx,fy,tx,ty){
        var dx = tx - fx;
        var dy = ty - fy;

        // angle in degrees
        var angleDeg = Math.atan2(dx, dy) * 180 / Math.PI + 180 + 90;
        angleDeg = angleDeg > 360 ? angleDeg - 360 : angleDeg

        return angleDeg
    }

    squarePath(x, y, width=40) {
        return [
            [x-width/2,y-width/2],
            [x-width/2,y+width/2],
            [x+width/2,y+width/2],
            [x+width/2,y-width/2]
        ];
    }

    distanceTo(x,y){
        return this.distance(x, this.myTank.x, y, this.myTank.y)
    }

    randomInt(minInclusive, maxInclusive) {
        var interval = maxInclusive - minInclusive + 1;
        return Math.floor(Math.random() * interval) + minInclusive;
    }
}

module.exports = Calculator;