/* 
Attempts to solve a modified graph traversal

Rules:
Each node may only be traveled to once if it is lettered, 
    if it is numbered it must be traveled to that many times
Each node connects only to its neighboring nodes
Each node to node transfer has a distance 1 (all distances between neigboring nodes is equal)
Each node must be traveled to its required number of times

Each lettered nodes must have two root nodes, denoted in capital letters, the traversal 
must start at one root node and end at the other. If a path encounters an end node, it must 
have first encountered all other nodes of its type.

No paths may cross except at nodes; which is only relavent for diagonal paths.

A graph is considered traveresed when all nodes have reached their required number of visits

Key features:
Not all graphs are solvable. 
There may not be a unique solution


Ex 
A a A
 solves to 
A -> a - > A
 or 
A <- a <- A

Ex 
A 2 A
0 B B

Has a solution like
A>2>A
  |\
0 B B


*/

import groovy.transform.Canonical
import groovy.transform.TupleConstructor

class SharedClosures {
    /*
    * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    * Generic Callables
    * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    */

    /**
    * identity
    */
    static def I = {
        world, x, y->
        

        world[y][x]
    }


}

@Canonical(excludes= 'neighbors')
class Node {
    def neighbors
    def moves
    def x
    def y
}

@TupleConstructor(includeSuperProperties=true) 
@Canonical(excludes= 'neighbors')
class CommonNode extends Node{
    short count
    short hits
}

@TupleConstructor(includeSuperProperties=true) 
@Canonical(excludes= 'neighbors')
class SingleNode extends Node {
    char type
    boolean start
}

class Move {
    def count
    def invalidatedBy
    def invalidates
    Node source, destination;
}

class DiagonalMove extends Move {
}

def buildWorld(def argFile){
    def worldText = []
    

    new File(argFile).eachLine {
        def worldLine = []
        it.getChars().each {
            //
            worldLine << it
        }
        worldText << worldLine
    }

    def world = []

    //first pass, build
    (0..worldText.size-1).each {
        y -> 
        def worldCol = [];
        (0..worldText[y].size-1).each {
            x -> 

            def val = worldText[y][x]
            def node = null

            if(Character.isDigit( val )){
                val = Integer.parseInt( "$val" )
                if(val>0){
                    node = new CommonNode(count: val, hits: 0)
                }
            } else {
                //
                node = new SingleNode(type: val.toLowerCase(), start: Character.isUpperCase( val ) )
            }

            if(node!=null){
                node.x = x
                node.y = y
            }
            worldCol << node
        }

        world << worldCol;

    }


    def makeNeighbors = {
        w, mx, my, m_x, m_y ->
        if(w[my][mx].neighbors == null){
            w[my][mx].neighbors = []
            println ''
            println 'working on ($mx, $my)'
        }

        w[my][mx].neighbors << w[m_y][m_x] 

        print "${w[my][mx].neighbors.size} "
    }
    
    //next pass build neighbors in each node
    forEachNeighbor(world, makeNeighbors)
    println ''

    world
}

def validate(world){
    def cnt = [:]
    (0..world.size-1).each {
        y -> 

        (0..world[y].size-1).each {
            x -> 
            //
            if(world[y][x] instanceof SingleNode){

                if(world[y][x].start){

                    if(cnt[world[y][x].type]==null){
                        cnt[world[y][x].type]=1
                    }else if(cnt[world[y][x].type]==1){
                        cnt[world[y][x].type]=2
                    }else{
                        throw IllegalStateException("too many end points for ${world[y][x].type}")
                    }
                }
            }
        }
    }

    cnt.each {
        if(it.value == 1){
            throw new IllegalStateException("start without end for ${it.key}")
        }
    }
}

def createMoveBetween(Node me, Node it, world){
    if(!me.moves){
        me.moves = []
    }

    if(me.x!=it.x && me.y!=it.y){
        //diagonal move
        def move = new DiagonalMove(source: me, destination: it)

        me.moves << move
    }else{
        me.moves << new Move(source: me, destination: it)
    }
    
}

def fixDiagonals(Node node, world){
    node?.moves?.each {
        move ->
        move.invalidates = []

        def me = move.source;
        def it = move.destination;

        //remove opposite
        it.moves.each {
            reverse ->

            if(reverse.destination == me){
                move.invalidates << reverse
            }
        }

        if(move instanceof DiagonalMove){
            println 'DIAG'
            print " ME (${me.x},${me.y}) to (${it.x},${it.y}) "
            

            def point1 = world[me.y][it.x]
            def point2 = world[it.y][me.x]

            if(point1==null){
                println "counterpoints at ${it.x},${me.y} is null"
            }else if(point2==null){
                println "counterpoints at ${me.x},${it.y} is null"
            }else{
                point1.moves?.each {
                    dm ->
                    println "Diag : (${dm.source.x},${dm.source.y}) to (${dm.destination.x},${dm.destination.y})"
                    if(dm.destination == point2){
                        move.invalidates << dm
                    }
                }

                point2.moves?.each {
                    dm ->
                    println "Diag : (${dm.source.x},${dm.source.y}) to (${dm.destination.x},${dm.destination.y})"
                    if(dm.destination == point1){
                        move.invalidates << dm
                    }
                }
            }
        }
    }
}

def buildLinks(nodeList, world){
    def tot = 0;
    nodeList.each {
        node -> 
        //
        def myClosure
        if(node instanceof CommonNode){
            myClosure = {
                it, me ->
                if(it!=null){
                    print '- adding link c'
                    createMoveBetween(me, it, world)
                }
            }
        }else if(node instanceof SingleNode){
            myClosure = {
                it, me ->

                if(it instanceof CommonNode){
                    print '- adding link sc'
                    createMoveBetween(me, it, world)
                }else if(it instanceof SingleNode){
                    if((it as SingleNode).type == me.type){
                        print '- adding link ss'
                        createMoveBetween(me, it, world)
                    }
                }
            }
        }

        println "Neighbors of $node : "
        forEachNeighborofNode(node) {
            print it
            myClosure(it, node)
            println ''
            
        }
        println "moves : ${node?.moves?.size}"
        tot += node?.moves?.size
    }

    nodeList.each {
        node ->
        fixDiagonals(node, world)
    }
    println "Total Moves in world $tot"
}


def makeMove(Move move, movesMade, movesAvailable){

    movesAvailable.remove(move)
    movesMade << move

    if(!move.invalidatedBy){
        move.invalidatedBy = []
    }
    move.invalidatedBy << move;

    move.invalidates?.each{
        if(it.invalidatedBy==null){
            it.invalidatedBy = []
        }
        it.invalidatedBy << move;
        movesAvailable.remove(it)
    }

    //add all 
    moves.destination.movesAvailable?.each{
        if(!it.invalidatedBy && !movesAvailable.contains(it)){
            movesAvailable.add(it);
        }
    }
}

def unmakeMove(Move move, movesMade, movesAvailable){
    if(movesMade[movesMade.size-1]!=move){
        throw new IllegalStateException("Cannot Reverse any but last move");
    }

    movesMade[movesMade.size-1] = null

    move.invalidatedBy = null

    movesAvailable << move

    move.invalidates?.each{
        if(it.invalidatedBy){
            it.invalidatedBy?.remove(move);
        }

        if(!it.invalidatedBy){
            movesAvailable << it
        }

    }
}
/**
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
* 
* Helpers that run through the 2d array in different ways   *
* 
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 
*/
def forEachNeighborofNode(Node node, callThis){
    node?.neighbors?.each {
        callThis(it)
    }
}

def forEachNeighborOfPosition(world, x, y, callThis){
    if(world[y][x]!=null){
        (y-1..y+1).each {
            _y->
            if(!(_y < 0 || _y > world.size-1)){
                (x-1..x+1).each {
                    _x ->
                    if(_x < 0 || _x > world[_y].size-1 || (_x == x && _y == y)){
                        //continue       
                    } else if(world[_y][_x]!=null) {
                        //add as neighbor
                        //world[y][x].neighbors << world[_y][_x] 
                        callThis(world, x, y, _x, _y)
                    }
                }
            }
        }
    }
}
def forEachNeighbor(world, callThis){
    (0..world.size-1).each {
        y -> 

        (0..world[y].size-1).each {
            x -> 

            forEachNeighborOfPosition(world, x, y, callThis)
            
        }
    }
}

def forEachAsMap(world , callable){
    def result = []
    (0..world.size-1).each {
        y -> 
        def resLine = []
        (0..world[y].size-1).each {
            x -> 
            resLine << callable(world, x , y);
        }

        result << resLine
    }

    result
}

def forEachAsArray(world , callable){
    def result = []
    (0..world.size-1).each {
        y -> 
        (0..world[y].size-1).each {
            x -> 
            def res = callable(world, x , y)
            if(res)
                result << res
        }
    }

    result
}


def calcValidMoves(nodeList){

}






/**
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
* Args
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*/
def argFile = 'C:\\Users\\Courtney Cecil\\Documents\\groovy\\world.txt'//this.args[0]

/**
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
* MAIN
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*/
def world = buildWorld(argFile)
validate(world)

def nodeList = forEachAsArray(world, SharedClosures.I);
buildLinks(nodeList, world)


println '- - - -'








